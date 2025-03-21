package me.nald.blog.data.dto;

import lombok.*;
import me.nald.blog.data.entity.Storage;
import me.nald.blog.data.vo.YN;

import java.text.SimpleDateFormat;
import java.util.List;

public class StorageResponseDto {


    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class getStorageList {

        int statusCode;
        List<StorageResponseDto.StorageInfo> list;
        Long total;

        @Builder
        public getStorageList(int statusCode, List<StorageResponseDto.StorageInfo> list, Long total) {
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
        Boolean downloadable;
        Boolean fileAuth;

        public StorageInfo(Storage storage) {
            SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd");
            String fileSrcPath=storage.getFileSrc();
            storageId = storage.getStorageId();
            fileName = storage.getFileName();
            fileSrc = fileSrcPath+ "/hls/"+fileSrcPath+".m3u8";
            fileSize = storage.getFileSize();
            fileDesc = storage.getDescription();
            fileAuth= YN.convert(storage.getFileAuth());
            fileType= storage.getFileType();
            fileCover= storage.getFileCover();
            vttSrc = storage.getVttSrc();
            createdDt = sdf.format(storage.getCreatedDt());
            downloadable = YN.convert(storage.getFileDownload());
        }


    }


}
