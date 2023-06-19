package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.annotation.RequireAuthSuper;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.data.model.StorageRequest;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.model.SearchItem;
import me.nald.blog.service.AccountService;
import me.nald.blog.service.StorageService;
import me.nald.blog.util.Constants;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@RestController
@RequestMapping("/storage")
public class StorageController {


    private final StorageService storageService;

    @GetMapping("/videoList")
    public Callable<Object> getVideoList(SearchItem searchItem) {
        return () -> storageService.getVideoList(searchItem);
    }

    // 단 건 비디오 상세정보
    @GetMapping("/getVideoDetail/{videoId}")
    public Callable<Object> getVideoDetail(@PathVariable Long videoId) {
        return () -> storageService.getVideoDetail(videoId);
    }

    // 스트리밍 최초 정보 로드
    @WithoutJwtCallable
    @GetMapping("/hls/{fileName}/{fileName}.m3u8")
    public ResponseEntity<Resource> videoHlsM3U8(@PathVariable String fileName) {
        return storageService.videoHlsM3U8(fileName);
    }
    
    // 스트리밍 파일 호출
    @WithoutJwtCallable
    @GetMapping("/hls/{fileName}/{tsName}.ts")
    public ResponseEntity<Resource> videoHlsTs(@PathVariable String fileName, @PathVariable String tsName) {
        return  storageService.videoHlsTs(fileName, tsName);
    }

    // 자막 로드
    @WithoutJwtCallable
    @GetMapping("/vtt/{fileName}_{language}.vtt")
    public ResponseEntity<Resource> videoVtt(@PathVariable String fileName,  @PathVariable String language) {
        return storageService.videoVtt(fileName, language);
    }


    // 파일 타입 변환
    @GetMapping("/convertVideoHls/{fileName}")
    public void convertVideoHls(@PathVariable String fileName) {
        storageService.convertVideoHls(fileName);
    }

    // 다운로드 파일
    @RequireAuthSuper
    @GetMapping("/download/{videoId}")
    public ResponseEntity<Resource> downloads(@PathVariable Long videoId) {
        return storageService.downloads(videoId);
    }


    // 업로드
    @RequireAuthSuper
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, value = "/uploadLocal")
        public Callable<Object> uploadVideo(@RequestPart StorageRequest info,
                                            @RequestPart(value="file", required=true) MultipartFile file,
                                            @RequestPart(value="fileCover", required=false) MultipartFile fileCover,
                                            @RequestPart(value="fileVtt", required=false) MultipartFile fileVtt,
                                               HttpServletRequest request) {
        info.setFile(file);
        info.setFileVtt(fileVtt);
        info.setFileCover(fileCover);
        return () -> storageService.uploadVideo(info);
    }


//    @AdminCallable
//    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public Callable<ResponseObject> uploadVideo(@RequestPart(value="files", required=false) List<MultipartFile> files,
//                                               @RequestPart(value = "body") CreateAdminNotice body,
//                                               HttpServletRequest request) {
//        return () -> {
//            if (request.getHeader(Constants.REQUEST_HEADER_KEY_USER_GROUP) == null){
//                throw new NotFoundUserGroupException(log);
//            }
//            body.setGroupId(Integer.valueOf(request.getHeader(Constants.REQUEST_HEADER_KEY_USER_GROUP)));
//            body.setUserId(String.valueOf(request.getAttribute(Constants.REQUEST_ATTRIBUTE_KEY_USER_ID)));
//            adminNoticeService.createNotice(files, body);
//            ResponseObject responseObject = new ResponseObject();
//            responseObject.putResult(null);
//            return responseObject;
//        };
//    }


}
