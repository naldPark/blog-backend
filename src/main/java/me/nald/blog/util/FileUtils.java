package me.nald.blog.util;

import lombok.extern.slf4j.Slf4j;
import me.nald.blog.config.BlogProperties;
import org.springframework.beans.factory.annotation.Autowired;

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

    private static BlogProperties blogProperties;

    @Autowired
    public void setBlogProperties(BlogProperties blogProperties) {
        this.blogProperties = blogProperties;
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

    // tomcat 임시 temp파일 삭제 로직
    public static void deleteFilesStartingWith(String filenamePrefix) throws IOException {
        String directoryPath =  blogProperties.getCommonPath() +blogProperties.getTomcatTempFilePath();
        File dir = new File(directoryPath);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                // 파일 이름이 지정된 접두사로 시작하는지 확인
                if (file.getName().startsWith(filenamePrefix)) {
                    if (file.delete()) {
                        System.out.println("삭제된 파일: " + file.getName());
                    } else {
                        System.err.println("파일 삭제 실패: " + file.getName());
                    }
                }
            }
        } else {
            System.err.println("지정된 경로에 파일이 존재하지 않습니다: " + directoryPath);
        }
    }


}
