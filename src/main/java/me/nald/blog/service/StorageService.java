package me.nald.blog.service;

import lombok.RequiredArgsConstructor;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.dto.StorageDto;
import me.nald.blog.data.persistence.entity.Storage;
import me.nald.blog.repository.StorageRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StorageService {

    private static BlogProperties blogProperties;
    private final StorageRepository storageRepository;

    @Autowired
    public void setBlogProperties(BlogProperties blogProperties) {
        this.blogProperties = blogProperties;
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
            String HlsPath = movieDir + "/hls/" + fileName + "/";

            File folder = new File(HlsPath);
            if (!folder.exists()) {
                folder.mkdir();
            }
            FFmpeg ffmpeg = new FFmpeg(blogProperties.getFfmpegPath() + "/ffmpeg");
            FFprobe ffprobe = new FFprobe(blogProperties.getFfmpegPath() + "/ffprobe");

            FFmpegBuilder builder = new FFmpegBuilder()
                    .overrideOutputFiles(true)
                    .setInput(inputPath + movieName)
                    .addOutput(HlsPath + fileName + ".m3u8")
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
        String HlsPath = movieDir + "/hls/" + fileName + "/";
        String fileFullPath = HlsPath + fileName + ".m3u8";
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

    public ResponseEntity<Resource> videoHlsTs(String movieName, String tsName) {
        String movieDir = blogProperties.getCommonPath() + "/movie";
        String fileName = FilenameUtils.getBaseName(movieName);
        String HlsPath = movieDir + "/hls/" + fileName + "/";
        String fileFullPath = HlsPath + tsName + ".ts";

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

}

