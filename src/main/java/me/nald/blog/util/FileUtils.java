package me.nald.blog.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
public class FileUtils {
    private FileUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void copyFile(String source, String target) throws IOException {
        Path sourcePath = Paths.get(source);
        Path targetPath = Paths.get(target);
        copyFile(sourcePath, targetPath);
    }
    
    public static void copyFile(Path sourcePath, Path targetPath) throws IOException {
        if (!Files.exists(targetPath.getParent())) {
            Files.createDirectories(targetPath.getParent());
        }
        Files.copy(sourcePath, targetPath);
    }

    public static void copyFile(String source, String target, CopyOption... options) throws IOException {
        Path sourcePath = Paths.get(source);
        Path targetPath = Paths.get(target);
        if (!Files.exists(targetPath.getParent())) {
            Files.createDirectories(targetPath.getParent());
        }
        Files.copy(sourcePath, targetPath, options);
    }

    public static void writeFile(String fullPath, String data) throws IOException {
        Path path = Paths.get(fullPath);
        log.info("writeFile path : {}, data : {}", path, data);
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        try (FileChannel channel = FileChannel.open(path,
                                        StandardOpenOption.WRITE,
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(data);
            channel.write(byteBuffer);
            channel.force(true);
        }
    }

    public static void writeFile(String fullPath, InputStream data) throws IOException {
        Path path = Paths.get(fullPath);
        log.info("writeFile path : {}, data : {}", path, data);
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, IOUtils.toByteArray(data), StandardOpenOption.CREATE);
    }

    public static String readFile(String fullPath) throws IOException {

        Path path = Paths.get(fullPath);
        return readFile(path);
    }

    public static String readFile(Path path) throws IOException {
        String data = null;
        try (FileChannel channel = FileChannel.open(path,
                                        StandardOpenOption.READ)) {
            int buuferSize = (int) channel.size();
            ByteBuffer byteBuffer = ByteBuffer.allocate(buuferSize);
            channel.read(byteBuffer);
            byteBuffer.flip();
            data = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        }
        return data;
    }

    public static boolean existsPath(String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) {
            return false;
        }
        return existsPath(Paths.get(fullPath));
    }

    public static boolean existsPath(Path fullPath) {
        if (fullPath == null) {
            return false;
        }
        return Files.exists(fullPath);
    }

    public static void createDirectoriesIfNotExists(String fullPath) throws IOException {
        createDirectoriesIfNotExists(Paths.get(fullPath));
    }

    public static void createDirectoriesIfNotExists(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
    }

    public static void deletePath(String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) {
            log.warn("fullPath is null or empty");
            return;
        }
        Path deletePath = Paths.get(fullPath);
        try (Stream<Path> walk = Files.walk(deletePath)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.error("delete error", e);
                    }
                });
        } catch (IOException e) {
            log.error("deletePath error", e);
        }
    }

    public static void deletePath(Path deletePath) {
        if (deletePath == null) {
            log.warn("deletePath is null");
            return;
        }
        try (Stream<Path> walk = Files.walk(deletePath)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.error("delete error", e);
                    }
                });
        } catch (IOException e) {
            log.error("deletePath error", e);
        }
    }


    public static void compressToZip(String source, String zipFile) throws IOException {
        if (source == null || source.isEmpty()) {
            System.out.println("source is null or empty");
            return;
        }
        Path sourcePath = Paths.get(source);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            try (Stream<Path> walk = Files.walk(sourcePath)) {
                walk.forEach(path -> {
                    if (Files.isDirectory(path)) {
                        if(sourcePath.equals(path)) {
                            return;
                        }
                        try {
                            Path targetFile = sourcePath.relativize(path);
                            if (path.getFileName().endsWith(File.separator)) {
                                zipOutputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                                zipOutputStream.closeEntry();
                            } else {
                                zipOutputStream.putNextEntry(new ZipEntry(targetFile.toString() + File.separator));
                                zipOutputStream.closeEntry();
                            }
                            System.out.println("compress dir: {}");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try (FileInputStream fileInputStream = new FileInputStream(path.toFile())) {
                            Path targetFile = sourcePath.relativize(path);
                            zipOutputStream.putNextEntry(new ZipEntry(targetFile.toString()));

                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = fileInputStream.read(buffer)) > 0) {
                                zipOutputStream.write(buffer, 0, length);
                            }
                            zipOutputStream.closeEntry();
                            System.out.println("compress file: {}");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException)e.getCause();
            }
            System.out.println("compressToZip error");
        }
    }


    public static void decompressFromZip(String zipFile, String target) throws IOException {
        if (zipFile == null || zipFile.isEmpty()) {
            log.warn("zipFile is null or empty");
            return;
        }
        if (target == null || target.isEmpty()) {
            log.warn("target is null or empty");
            return;
        }

        Path zipFilePath = Paths.get(zipFile);
        Path targetPath = Paths.get(target);
        decompressFromZip(zipFilePath, targetPath);
    }

    public static void decompressFromZip(Path zipFilePath, Path targetPath) throws IOException {
        if (zipFilePath == null) {
            log.warn("zipFilePath is null");
            return;
        }
        if (targetPath == null) {
            log.warn("targetPath is null");
            return;
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                Path zipEntryTargetPath = zipSlipProtect(zipEntry, targetPath);
                if (zipEntry.getName().endsWith(File.separator)) {
                    Files.createDirectories(zipEntryTargetPath);
                    log.info("decompress dir:{}", zipEntryTargetPath);
                } else {
                    if (Files.notExists(zipEntryTargetPath.getParent())) {
                        Files.createDirectories(zipEntryTargetPath.getParent());
                    }
                    Files.copy(zipInputStream, zipEntryTargetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("decompress file:{}", zipEntryTargetPath);
                }
                zipEntry = zipInputStream.getNextEntry();

            }
            zipInputStream.closeEntry();
        }
    }

    public static Path zipSlipProtect(ZipEntry zipEntry, Path targetPath)
        throws IOException {
        Path targetDirResolved = targetPath.resolve(zipEntry.getName());
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetPath)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName());
        }
        return normalizePath;
    }

//    sa-889
    public static void copyDirectoryFiles(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            copyDirectory(source, destination);
        } else {
            copyFile(source.getPath(), destination.getPath());
        }
    }

    private static void copyDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
        }
        for (String f : sourceDirectory.list()) {
            copyDirectoryFiles(new File(sourceDirectory, f), new File(destinationDirectory, f));
        }
    }


}
