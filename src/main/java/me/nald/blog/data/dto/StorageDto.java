package me.nald.blog.data.dto;

import lombok.*;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.data.persistence.entity.Storage;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
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

        public StorageInfo(Storage storage) {
            storageId = storage.getStorageId();
            fileName = storage.getFileName();
            fileSrc = storage.getFileSrc();
            fileSize = storage.getFileSize();
            fileType= storage.getFileType();
            fileCover = storage.getFileCover();
        }
    }
}
