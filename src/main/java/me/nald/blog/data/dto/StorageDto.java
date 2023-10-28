package me.nald.blog.data.dto;

import lombok.*;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.data.persistence.entity.Storage;
import me.nald.blog.data.vo.YN;
import me.nald.blog.util.FileUtils;
import me.nald.blog.util.Util;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
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
        String fileDesc;
        String fileType;
        String fileCover;
        String vttSrc;
        String createdDt;
        String downloadable;

        public StorageInfo(Storage storage) {
            SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd");
            storageId = storage.getStorageId();
            fileName = storage.getFileName();
            fileSrc = storage.getFileSrc();
            fileSize = storage.getFileSize();
            fileDesc = storage.getDescription();
            fileType= storage.getFileType();
//            fileCover= storage.getFileCover();
//            fileCover =  Util.storageImgToString(storage.getFileCover());
            vttSrc = storage.getVttSrc();
            createdDt = sdf.format(storage.getCreatedDt());
            downloadable = storage.getFileDownload().name();
        }


    }


}
