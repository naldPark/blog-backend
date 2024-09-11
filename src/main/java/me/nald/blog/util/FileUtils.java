package me.nald.blog.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    public static void createDirectoriesIfNotExists(String fullPath) throws IOException {
        createDirectoriesIfNotExists(Paths.get(fullPath));
    }

    public static void createDirectoriesIfNotExists(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
    }

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



    public static String extractFilenamePrefix(String filePath) {
        // 파일 경로에서 마지막 '/' 또는 '\\' 기준으로 문자열을 자름
        int lastSeparatorIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        int lastIndex = filePath.lastIndexOf('_');
        if (lastSeparatorIndex >= 0 && lastIndex > lastSeparatorIndex) {
            return filePath.substring(lastSeparatorIndex + 1, lastIndex);
        }
        return null; // 적절한 접두사를 찾을 수 없는 경우
    }

    // 임시 파일 삭제 메서드
    public static void deleteTemporaryFile(MultipartFile multipartFile) {
        if (multipartFile instanceof DiskFileItem diskFileItem) {
            String tempFilePath = diskFileItem.getStoreLocation().getPath();

            System.out.println("지울꺼 ????????"+tempFilePath);
            if (diskFileItem.getStoreLocation().exists() && !diskFileItem.getStoreLocation().delete()) {
                log.warn("임시 파일 삭제 실패" + tempFilePath);
            }
        } else {
            log.warn("임시 파일 삭제 실패: DiskFileItem 인스턴스가 아님.");
        }
    }

    public static void  deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file); // 폴더 안의 폴더를 재귀적으로 삭제
                } else {
                    if (!file.delete()) {
                        System.out.println("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
        if (!folder.delete()) {
            System.out.println("Failed to delete folder: " + folder.getAbsolutePath());
        }
    }

}
