package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.data.persistence.entity.Account;
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

    @WithoutJwtCallable
    @GetMapping("/playVideo/{videoId}")
    public Callable<Object> playVideo(@PathVariable Long videoId) {
        return () -> storageService.playVideo(videoId);
    }


    @WithoutJwtCallable
    @GetMapping("/hls/{fileName}/{fileName}.m3u8")
    public ResponseEntity<Resource> videoHlsM3U8(@PathVariable String fileName) {

        return storageService.videoHlsM3U8(fileName);
    }

    @WithoutJwtCallable
    @GetMapping("/hls/{fileName}/{tsName}.ts")
    public ResponseEntity<Resource> videoHlsTs(@PathVariable String fileName, @PathVariable String tsName) {
        return  storageService.videoHlsTs(fileName, tsName);
   }

    @WithoutJwtCallable
    @GetMapping("/videoHls/{fileName}")
    public void videoHls(@PathVariable String fileName) {
        storageService.videoHls(fileName);
    }

    @WithoutJwtCallable
    @PostMapping("/download")
    public Callable<Object> downloads(HttpServletRequest request) {
        return () -> {
            Path filePath = storageService.downloads();
            try {
                Resource resource = new FileSystemResource(zipFilePath) {
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
        };
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
