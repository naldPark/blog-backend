package me.nald.blog.data.dto;

import lombok.*;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.data.persistence.entity.Storage;
import me.nald.blog.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class StorageDto {


    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class getStorageList {

        int statusCode;
        List<StorageDto.StorageInfo> list;
        Long total;

        @Builder
        public getStorageList(int statusCode, List<StorageDto.StorageInfo> list, Long total) {
            this.statusCode = statusCode;
            this.list = list;
            this.total = total;
        }
    }

    @Getter
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class StorageInfo {

        Long storageId;
        String fileName;
        String fileSrc;
        Long fileSize;
        String fileType;
        String fileCover;
        String vttSrc;

        public StorageInfo(Storage storage) {
            storageId = storage.getStorageId();
            fileName = storage.getFileName();
            fileSrc = storage.getFileSrc();
            fileSize = storage.getFileSize();
            fileType= storage.getFileType();
//            fileCover = storage.getFileCover();
            System.out.println("=================");
            System.out.println("C:/nfs" + "/movie" +storage.getFileCover());

            try {
                byte[] bytes = Files.readAllBytes(Paths.get("C:/nfs/movie" +storage.getFileCover()));
                String base64EncodedImageBytes = Base64.getEncoder().encodeToString(bytes);
                fileCover = base64EncodedImageBytes;
            } catch (IOException e) {
                fileCover = null;
            }
            vttSrc = storage.getVttSrc();
        }
    }


}
