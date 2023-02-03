package me.nald.blog.service;

import lombok.RequiredArgsConstructor;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.data.dto.StorageDto;
import me.nald.blog.data.persistence.entity.Storage;
import me.nald.blog.repository.AccountRepository;
import me.nald.blog.repository.StorageRepository;
import me.nald.blog.response.CommonResponse;
import me.nald.blog.response.Response;
import me.nald.blog.util.FileUtils;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
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


    public Path postWorkspaceDownloads() {
        Path zipFilePath = Paths.get(blogProperties.getCommonPath()+"/movie/hls/sample").resolve("sample0." + "ts");

        return zipFilePath;
    }

    public Boolean videoHls(String movieName) {
        Boolean result = false;
        FFmpeg ffmpeg = null;
        FFprobe ffprobe = null;
        String movieDir = blogProperties.getCommonPath() + "/movie";
        String fileName = FilenameUtils.getBaseName(movieName);
        String inputPath = movieDir + "/upload/";
        String HlsPath = movieDir + "/hls/" + fileName + "/";

        try {
            String osName = System.getProperty("os.name");
            File folder = new File(HlsPath);
            if (!folder.exists()) {
                folder.mkdir(); //폴더 생성합니다.
            }
//            if (osName.toLowerCase().contains("unix") || osName.toLowerCase().contains("linux")) {
                ffmpeg = new FFmpeg(blogProperties.getFfmpegPath() + "/ffmpeg");
                ffprobe = new FFprobe(blogProperties.getFfmpegPath() + "/ffprobe");
//            } else {
//                ffmpeg = new FFmpeg(blogProperties.getFfmpegPath() + "/ffmpeg.exe");
//                ffprobe = new FFprobe(blogProperties.getFfmpegPath() + "/ffprobe.exe");
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        FFmpegBuilder builder = new FFmpegBuilder()
                .overrideOutputFiles(true)
                .setInput(inputPath + movieName)
                .addOutput(HlsPath + fileName + ".m3u8")
//                .addExtraArgs("-acodec", "copy")
//                .addExtraArgs("-vcodec", "copy")
                .addExtraArgs("-profile:v", "baseline")
                .addExtraArgs("-level", "3.0")
                .addExtraArgs("-start_number", "0")
                .addExtraArgs("-hls_time", "10")
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-f", "hls")
//                .setVideoBitRate(1)
                .addExtraArgs("-safe", "0")
                .addExtraArgs("-preset", "ultrafast")
                .setVideoResolution(1920, 1080)
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done();

        builder.setVerbosity(FFmpegBuilder.Verbosity.INFO);
        System.out.println("익스큐트전");

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        System.out.println("job생성");
        FFmpegJob job = executor.createJob(builder);

        System.out.println("job런");
        job.run();
        System.out.println("job런완료");
        if (job.getState() == FFmpegJob.State.FINISHED) {
            System.out.println("리절트 트루");
            result = true;
        }


        return result;
    }

    public ResponseEntity<Resource> videoHlsM3U8(String movieName) {

        String movieDir = blogProperties.getCommonPath() + "/movie";
        String fileName = FilenameUtils.getBaseName(movieName);
        String HlsPath = movieDir + "/hls/" + fileName + "/";
        String fileFullPath = HlsPath + fileName + ".m3u8";
        System.out.println("FileSystemResource ㅇ요청할게");

        Resource resource = new FileSystemResource(fileFullPath);

        System.out.println("FileSystemResource 요청완료");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName + ".m3u8");
        headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));

        System.out.println("리턴할게");
        return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);


    }

    public ResponseEntity<Resource> videoHlsTs(String movieName, String tsName) {
        String movieDir = blogProperties.getCommonPath() + "/movie";
        String fileName = FilenameUtils.getBaseName(movieName);
        String HlsPath = movieDir + "/hls/" + fileName + "/";
        String fileFullPath = HlsPath + tsName + ".ts";
        System.out.println("FileSystemResource ㅇ요청할게");
        Resource resource = new FileSystemResource(fileFullPath);

        System.out.println("FileSystemResource 요청완료");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + tsName + ".ts");
        headers.setContentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE));

        System.out.println("리턴할게");
        return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);

    }

    public ResponseEntity<Resource> videoHlsTstest() {
        String fileFullPath = "test.ts";
        System.out.println("FileSystemResource ㅇ요청할게");
        Resource resource = new FileSystemResource(fileFullPath);

        System.out.println("FileSystemResource 요청완료");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + "test.ts");
        headers.setContentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE));

        System.out.println("리턴할게");
        return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);

    }

    public String videoHlsTstest2(String movieName, String tsName) {
        String movieDir = blogProperties.getCommonPath() + "/movie";
        String fileName = FilenameUtils.getBaseName(movieName);
        String HlsPath = movieDir + "/hls/" + fileName + "/";
        String fileFullPath = HlsPath + tsName + ".ts";
        System.out.println("리턴할게");
        return fileFullPath;

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

    public Map<String, Object> playVideo(Long videoId) {

        HashMap<String, Object> map = new HashMap<>();
        Storage storage = storageRepository.getById(videoId);
        StorageDto.StorageInfo res = new StorageDto.StorageInfo(storage);
        System.out.println(storage);

        map.put("statusCode", 200);
        ;
        map.put("data", res);

        return map;

    }

}

