package me.nald.blog.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.data.dto.StorageDto;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.data.persistence.entity.QAccount;
import me.nald.blog.data.persistence.entity.QStorage;
import me.nald.blog.data.persistence.entity.Storage;
import me.nald.blog.model.SearchItem;
import me.nald.blog.repository.StorageRepository;
import me.nald.blog.response.CommonResponse;
import me.nald.blog.response.Response;
import me.nald.blog.util.Util;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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


//    public List<StorageDto.StorageInfo> getVideoList(SearchItem searchItem){
//        System.out.println(searchItem);
//        return storageRepository.findAll().stream().map(StorageDto.StorageInfo::new).collect(Collectors.toList());
//    }

    public StorageDto.getStorageList getVideoList(SearchItem searchItem){

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

        if(searchItem.getLimit() != 0){
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

    public Map<String, Object> playVideo(Long videoId) {
        HashMap<String, Object> map = new HashMap<>();
        Storage storage = storageRepository.getById(videoId);
        StorageDto.StorageInfo res = new StorageDto.StorageInfo(storage);
        map.put("statusCode", 200);
        map.put("data", res);
        return map;
    }

    public Boolean videoHls(String movieName) {
        Boolean result = false;
        try {
            String movieDir = blogProperties.getCommonPath() + "/movie";
            String fileName = FilenameUtils.getBaseName(movieName);
            String inputPath = movieDir + "/upload/";
            String hlsPath = movieDir + "/hls/" + fileName + "/";

            File folder = new File(hlsPath);
            if (!folder.exists()) {
                folder.mkdir();
            }
            FFmpeg ffmpeg = new FFmpeg(blogProperties.getFfmpegPath() + "/ffmpeg");
            FFprobe ffprobe = new FFprobe(blogProperties.getFfmpegPath() + "/ffprobe");

            FFmpegBuilder builder = new FFmpegBuilder()
                    .overrideOutputFiles(true)
                    .setInput(inputPath + movieName)
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
            job.run();
            if (job.getState() == FFmpegJob.State.FINISHED) {
                result = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
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
        Path filePath = Paths.get(blogProperties.getCommonPath()+"/movie"+storage.getDownloadSrc());
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


    //    public void uploadVideo(List<MultipartFile> files, String userId, Long noticeId, int groupId) {
//        String noticeFilePath = Constants.FILE_MANAGER_PATH_PREFIX + Constants.NOTICE_FILE_PATH;
//        String folderPath = noticeFilePath + "/" + noticeId;
//        try {
//            for (int i = 0; i < files.size(); ++i) {
//                MultipartFile currentFile = files.get(i);
//                FileUtils.createDirectoriesIfNotExists(folderPath);
//                String fileName = currentFile.getOriginalFilename();
//                String ext = fileName.substring(fileName.lastIndexOf("."));
//                String filePath = folderPath + "/" + System.currentTimeMillis() + (int) (Math.random() * 1000000) + i + ext;
//                CreateAdminNoticeFile createAdminNoticeFile = new CreateAdminNoticeFile(fileName, filePath, currentFile.getSize(), userId, noticeId, groupId);
//                InputStream readStream = currentFile.getInputStream();
//                byte[] readBytes = new byte[4096];
//                OutputStream writeStream = Files.newOutputStream(Paths.get(createAdminNoticeFile.getFileSrc()));
//                while(readStream.read(readBytes) > 0) {
//                    writeStream.write(readBytes);
//                }
//                writeStream.close();
//                adminNoticeDAO.createNoticeFile(createAdminNoticeFile);
//            }
//        } catch (IOException e) {
//            log.error("write file failed {}", e);
//        }
//    }


}

