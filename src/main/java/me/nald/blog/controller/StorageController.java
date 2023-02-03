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
    @PostMapping("/download")
    public Callable<Object> postWorkspacesBackupDownloads(HttpServletRequest request) {
        return () -> {
            Path zipFilePath = storageService.postWorkspaceDownloads();
            try {
                Resource resource = new FileSystemResource(zipFilePath) {
                    @Override
                    public InputStream getInputStream() throws IOException {
                        return new FileInputStream(zipFilePath.toFile()) {
                            @Override
                            public void close() throws IOException {
                                super.close();
//                                Files.delete(zipFilePath);
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


    @WithoutJwtCallable
    @GetMapping(value = "/streamingVideo")
    public void viewMp4Stream(HttpServletRequest request, HttpServletResponse response, int videoId) throws IOException {
        System.out.println("어섭쇼video2");
        Map<Integer, String> videoPath = new HashMap<>();
        videoPath.put(1, "sample.mp4");
        videoPath.put(2, "HarryPotterAndTheSorcerersStone.mp4");

        File file = new File(Constants.STORAGE_FILE_PATH + videoPath.get(videoId));
        RandomAccessFile randomFile = new RandomAccessFile(file, "r");
        long rangeStart = 0; //요청 범위의 시작 위치
        long rangeEnd = 0; //요청 범위의 끝 위치
        boolean isPart = false; //부분 요청일 경우 true, 전체 요청의 경우 false
        try { //동영상 파일 크기
            long movieSize = randomFile.length(); //스트림 요청 범위, request의 헤더에서 range를 읽는다.

            String range = request.getHeader("range");
            if (range != null) {
                if (range.endsWith("-")) {
                    range = range + (movieSize - 1);
                }
                int idxm = range.trim().indexOf("-");
                rangeStart = Long.parseLong(range.substring(6, idxm));
                rangeEnd = Long.parseLong(range.substring(idxm + 1));
                if (rangeEnd >= movieSize) {
                    rangeEnd = movieSize - 1;
                }
                if (rangeStart > 0) {
                    isPart = true;
                }
            } else {
                rangeStart = 0;
                rangeEnd = movieSize - 1;
            }
            long partSize = rangeEnd - rangeStart + 1;
            response.reset();
            response.setStatus(isPart ? 206 : 200);
            response.setContentType("video/mp4");
            response.setHeader("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + movieSize);
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Length", "" + partSize);
            response.setHeader("totalsize", "" + movieSize);
            OutputStream out = response.getOutputStream();
            randomFile.seek(rangeStart);
            int bufferSize = 8 * 1024;
            byte[] buf = new byte[bufferSize];
            do {
                int block = partSize > bufferSize ? bufferSize : (int) partSize;
                int len = randomFile.read(buf, 0, block);
                out.write(buf, 0, len);
                partSize -= block;
            } while (partSize > 0);
        } catch (IOException e) {
        } finally {
            randomFile.close();
        }
    }

    @WithoutJwtCallable
    @GetMapping("/hls/{fileName}/{fileName}.m3u8")
    public ResponseEntity<Resource> videoHlsM3U8(@PathVariable String fileName) {

         ResponseEntity<Resource> test = storageService.videoHlsM3U8(fileName);

        System.out.println("리턴직전 컨트롤러"+test);

        return test;

//        return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);
    }


    @WithoutJwtCallable
    @GetMapping("/hls/{fileName}/{tsName}.ts")
    public ResponseEntity<Resource> videoHlsTs(@PathVariable String fileName, @PathVariable String tsName) {

        ResponseEntity<Resource> test = storageService.videoHlsTs(fileName, tsName);

        System.out.println("리턴직전 ts 컨트롤러"+test);

        return test;
   }


    @WithoutJwtCallable
    @GetMapping("/hls/test.ts")
    public ResponseEntity<Resource> videoHlsTstest() {

        ResponseEntity<Resource> test = storageService.videoHlsTstest(fileName, tsName);

        System.out.println("리턴직전 ts 컨트롤러"+test);

        return test;
    }


    @WithoutJwtCallable
    @GetMapping("/videoHls/{fileName}")
    public void videoHls(@PathVariable String fileName) {
        storageService.videoHls(fileName);
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

    @WithoutJwtCallable
    @GetMapping("/playVideo/{videoId}")
    public Callable<Object> playVideo(@PathVariable Long videoId) {
        return () -> storageService.playVideo(videoId);
    }



}
