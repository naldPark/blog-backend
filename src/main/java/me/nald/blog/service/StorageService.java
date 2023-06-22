package me.nald.blog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import static me.nald.blog.exception.ErrorSpec.*;

import java.awt.image.BufferedImage;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.dto.StorageDto;
import me.nald.blog.data.model.StorageRequest;
import me.nald.blog.data.persistence.entity.QStorage;
import me.nald.blog.data.persistence.entity.Storage;
import me.nald.blog.data.vo.YN;
import me.nald.blog.exception.Errors;
import me.nald.blog.model.SearchItem;
import me.nald.blog.repository.StorageRepository;
import me.nald.blog.util.FileUtils;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static me.nald.blog.exception.ErrorSpec.DuplicatedId;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StorageService {

    @Autowired
    EntityManager em;

    private static BlogProperties blogProperties;
    private final StorageRepository storageRepository;

    @Autowired
    public void setBlogProperties(BlogProperties blogProperties) {
        this.blogProperties = blogProperties;
    }

    public StorageDto.getStorageList getVideoList(SearchItem searchItem) {

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QStorage storage = QStorage.storage;
        BooleanBuilder builder = new BooleanBuilder();

        if (Objects.nonNull(searchItem.getSearchText()) && !searchItem.getSearchText().isEmpty()) {
            builder.and(storage.fileName.contains(searchItem.getSearchText()));
        }
        if (Objects.nonNull(searchItem.getType()) && !searchItem.getType().isEmpty()) {
            builder.and(storage.fileType.eq(searchItem.getType()));
        }

        JPAQuery<Storage> query = queryFactory
                .selectFrom(storage).distinct()
                .from(storage)
                .where(builder)
                .orderBy(storage.fileName.asc().nullsLast());

        if (searchItem.getLimit() != 0) {
            query.offset(searchItem.getOffset());
            query.limit(searchItem.getLimit());
        }

        List<StorageDto.StorageInfo> list = query.fetch().stream().map(StorageDto.StorageInfo::new).collect(Collectors.toList());

        Long totalCount = queryFactory
                .select(storage.count())
                .from(storage)
                .fetchOne();

        return StorageDto.getStorageList.builder()
                .statusCode(200)
                .list(list)
                .total(totalCount)
                .build();
    }

    public Map<String, Object> getVideoDetail(Long videoId) {
        HashMap<String, Object> map = new HashMap<>();
        Storage storage = storageRepository.getById(videoId);
        StorageDto.StorageInfo res = new StorageDto.StorageInfo(storage);
        map.put("statusCode", 200);
        map.put("data", res);
        return map;
    }

    @Async
    public void convertVideoHls(Long videoId) {

        Storage storage = storageRepository.getById(videoId);
        if (storage.getFileSrc() != null) {
            throw Errors.of(AlreadyExists, "AlreadyExists");
        } else {
            try {
                String movieDir = blogProperties.getCommonPath() + "/movie";
                String fileName = FilenameUtils.getBaseName(storage.getDownloadSrc());
                String inputPath = movieDir + storage.getDownloadSrc();
                String hlsPath = movieDir + "/hls/" + fileName + "/";

                File folder = new File(hlsPath);
                if (!folder.exists()) {
                    folder.mkdir();
                }
                FFmpeg ffmpeg = new FFmpeg(blogProperties.getFfmpegPath() + "/ffmpeg");
                FFprobe ffprobe = new FFprobe(blogProperties.getFfmpegPath() + "/ffprobe");

                FFmpegBuilder builder = new FFmpegBuilder()
                        .overrideOutputFiles(true)
                        .setInput(inputPath)
                        .addOutput(hlsPath + fileName + ".m3u8")
                        .addExtraArgs("-profile:v", "baseline")
                        .addExtraArgs("-level", "3.0")
                        .addExtraArgs("-start_number", "0")
                        .addExtraArgs("-hls_time", "10")
                        .addExtraArgs("-hls_list_size", "0")
                        .addExtraArgs("-f", "hls")
                        .addExtraArgs("-safe", "0")
                        .addExtraArgs("-preset", "ultrafast")
                        .setVideoResolution(1920, 1080)
                        .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                        .done();

                builder.setVerbosity(FFmpegBuilder.Verbosity.INFO);
                FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
                FFmpegJob job = executor.createJob(builder);
                System.out.println("여기가 끝");
                job.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ResponseEntity<Resource> videoHlsM3U8(String movieName) {

        String movieDir = blogProperties.getCommonPath() + "/movie";
        String fileName = FilenameUtils.getBaseName(movieName);
        String hlsPath = movieDir + "/hls/" + fileName + "/";
        String fileFullPath = hlsPath + fileName + ".m3u8";
        Path filePath = Paths.get(fileFullPath);
        Resource resource = new FileSystemResource(filePath) {
            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(filePath.toFile()) {
                    @Override
                    public void close() throws IOException {
                        super.close();
//                                Files.delete(zipFilePath);
                    }
                };
            }
        };
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(resource.getFilename()).build());
        headers.setContentType(mediaType);
        return ResponseEntity.ok().headers(headers).body(resource);

    }

    public ResponseEntity<Resource> videoVtt(String movieName, String language) {

        String movieDir = blogProperties.getCommonPath() + "/movie";
        String fileName = FilenameUtils.getBaseName(movieName);
        String fileFullPath = movieDir + "/vtt/" + fileName + "_" + language + ".vtt";
        Path filePath = Paths.get(fileFullPath);
        Resource resource = new FileSystemResource(filePath) {
            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(filePath.toFile()) {
                    @Override
                    public void close() throws IOException {
                        super.close();
                    }
                };
            }
        };
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(resource.getFilename()).build());
        headers.setContentType(mediaType);
        return ResponseEntity.ok().headers(headers).body(resource);

    }

    public ResponseEntity<Resource> videoHlsTs(String movieName, String tsName) {
        String movieDir = blogProperties.getCommonPath() + "/movie";
        String fileName = FilenameUtils.getBaseName(movieName);
        String hlsPath = movieDir + "/hls/" + fileName + "/";
        String fileFullPath = hlsPath + tsName + ".ts";

        Path filePath = Paths.get(fileFullPath);
        Resource resource = new FileSystemResource(filePath) {
            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(filePath.toFile()) {
                    @Override
                    public void close() throws IOException {
                        super.close();
                    }
                };
            }
        };
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(resource.getFilename()).build());
        headers.setContentType(mediaType);
        return ResponseEntity.ok().headers(headers).body(resource);

    }


    public ResponseEntity<Resource> downloads(Long videoId) {
        Storage storage = storageRepository.getById(videoId);
        Path filePath = Paths.get(blogProperties.getCommonPath() + "/movie" + storage.getDownloadSrc());
        try {
            Resource resource = new FileSystemResource(filePath) {
                @Override
                public InputStream getInputStream() throws IOException {
                    return new FileInputStream(filePath.toFile()) {
                        @Override
                        public void close() throws IOException {
                            super.close();
                        }
                    };
                }
            };
            MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(resource.getFilename()).build());
            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    private String getMultiFileExt(MultipartFile file) {
        if (file != null) {
            return file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        } else {
            return null;
        }
    }


    @Transactional
    @Async
    public Map<String, Object> uploadVideo1(StorageRequest info) {
        HashMap<String, Object> map = new HashMap<>();
        String movieDir = blogProperties.getCommonPath() + "/movie";
        String saveFileName = "/" + System.currentTimeMillis() + (int) (Math.random() * 1000000);
        String uploadPath = "/upload" + saveFileName;
        System.out.println("1번");

        try {
            FileUtils.createDirectoriesIfNotExists(movieDir + uploadPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fileExt = null;
        String vttExt = null;
        String imageExt = null;
        System.out.println("2번");
        Map<String, MultipartFile> files = new HashMap<>();
        if (info.getFile() != null) {
            fileExt = uploadPath + saveFileName + getMultiFileExt(info.getFile());
            files.put(fileExt, info.getFile());
        }
        System.out.println("3번" + info.getFileVtt() == null);
        if (info.getFileVtt() != null) {
            vttExt = uploadPath + saveFileName + getMultiFileExt(info.getFileVtt());
            files.put(vttExt, info.getFileVtt());
        }

        if (info.getFileCover() != null) {
            System.out.println("4번");
            String[] imageInfo = info.getFileCover().split(",");
            String extension = imageInfo[0].replace("data:image/", "").replace(";base64", "");
            byte[] data = DatatypeConverter.parseBase64Binary(imageInfo[1]);
            imageExt = uploadPath + saveFileName + "." + extension;
            File file = new File(movieDir + imageExt);
            try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                outputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("5번");
        }

        Set<String> keySet = files.keySet();
        for (String key : keySet) {
            InputStream readStream = null;
            try {
                readStream = files.get(key).getInputStream();

                byte[] readBytes = new byte[4096];
                OutputStream writeStream = null;

                writeStream = Files.newOutputStream(Paths.get(movieDir + key));

                while (readStream.read(readBytes) > 0) {
                    writeStream.write(readBytes);
                }
                System.out.println("6번");
                writeStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Storage storageInfo = Storage.createStorage(
                info.getFileName(),
                info.getFileSize(),
                fileExt,
                info.getCategory(),
                imageExt,
                vttExt,
                YN.convert(info.getFileAuth()),
                YN.convert(info.getFileDownload())
        );
        System.out.println("7번");
        storageRepository.save(storageInfo);
//            System.out.println("8번");
//            convertVideoHls(storageInfo.getStorageId());
//            System.out.println("9번");
        map.put("statusCode", 200);
        map.put("storageId", storageInfo.getStorageId());
        System.out.println("10번");
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        return map;
    }

    @Transactional
//    @Async
    public Map<String, Object> uploadVideo(StorageRequest info) {

        String movieDir = blogProperties.getCommonPath() + "/movie";
        String saveFileName = "/" + System.currentTimeMillis() + (int) (Math.random() * 1000000);
        String uploadPath = "/upload" + saveFileName;
        System.out.println("1번");

        HashMap<String, Object> map = new HashMap<>();
        try {
            FileUtils.createDirectoriesIfNotExists(movieDir + uploadPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
            CompletableFuture<Storage> future = CompletableFuture.supplyAsync(() -> {

                String fileExt = null;
                String vttExt = null;
                String imageExt = null;
                System.out.println("2번");
                Map<String, MultipartFile> files = new HashMap<>();
                if (info.getFile() != null) {
                    fileExt = uploadPath + saveFileName + getMultiFileExt(info.getFile());
                    files.put(fileExt, info.getFile());
                }
                System.out.println("3번" + info.getFileVtt() == null);
                if (info.getFileVtt() != null) {
                    vttExt = uploadPath + saveFileName + getMultiFileExt(info.getFileVtt());
                    files.put(vttExt, info.getFileVtt());
                }

                if (info.getFileCover() != null) {
                    System.out.println("4번");
                    String[] imageInfo = info.getFileCover().split(",");
                    String extension = imageInfo[0].replace("data:image/", "").replace(";base64", "");
                    byte[] data = DatatypeConverter.parseBase64Binary(imageInfo[1]);
                    imageExt = uploadPath + saveFileName + "." + extension;
                    File file = new File(movieDir + imageExt);
                    try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                        outputStream.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("5번");
                }

                Set<String> keySet = files.keySet();
                for (String key : keySet) {
                    InputStream readStream = null;
                    try {
                        readStream = files.get(key).getInputStream();

                        byte[] readBytes = new byte[4096];
                        OutputStream writeStream = null;

                        writeStream = Files.newOutputStream(Paths.get(movieDir + key));

                        while (readStream.read(readBytes) > 0) {
                            writeStream.write(readBytes);
                        }
                        System.out.println("6번");
                        writeStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Storage storageInfo = Storage.createStorage(
                        info.getFileName(),
                        info.getFileSize(),
                        fileExt,
                        info.getCategory(),
                        imageExt,
                        vttExt,
                        YN.convert(info.getFileAuth()),
                        YN.convert(info.getFileDownload())
                );
                System.out.println("7번");
                storageRepository.save(storageInfo);
                return storageInfo;
//            }).thenAccept(result -> {
//                System.out.println("checkBackupState success" + result);
//                convertVideoHls(result.getStorageId());
//            }).thenAccept(result -> {
//                return "ㅋㅋㅋ";
//                System.out.println("checkBackupState success" + result);
//                convertVideoHls(result.getStorageId());
            });





//            }).exceptionally(e -> {
//                log.error("checkBackupState error", e);
//                try {
//                    FileUtils.deletePath(workspaceBackup.getBackupPath());
//                } catch (Exception exception) {
//                    exception.printStackTrace();
//                } finally {
//                    if (e.getCause() instanceof BackupRequestFailedException) {// e로 instanceof하면 안걸러짐
//                        BackupRequestFailedException exception = (BackupRequestFailedException)(e.getCause());
//                        workspaceBackup.setErrorMessage(exception.getAdditionalMessage());
//                        workspaceBackup.setErrorSubCode(exception.getSubCode());
//                    } else {
//                        workspaceBackup.setErrorMessage(e.getMessage());
//                        workspaceBackup.setErrorSubCode(Constants.RESTORE_ERROR_SUB_CODE.UNKNOWN.VALUE);
//                    }
//                    writeStatusWorkspace(Constants.BACKUP_STATUS.ERROR, workspaceBackup);
//                }
//                return null;
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
////            handleBackupWorkspaceException(workspaceBackup, e);
//        }
        System.out.println("맵");
        try {
            convertVideoHls(future.get().getStorageId());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return map;

    }



}

