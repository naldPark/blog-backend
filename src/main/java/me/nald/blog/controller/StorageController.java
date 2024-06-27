package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import me.nald.blog.annotation.RequireAuthSuper;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.dto.StorageDto;
import me.nald.blog.data.model.StorageRequest;
import me.nald.blog.model.SearchItem;
import me.nald.blog.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.concurrent.Callable;

@AllArgsConstructor
@RestController
@RequestMapping("/storage")
public class StorageController {


    private final StorageService storageService;

    @GetMapping("/videoList")
    public Callable<Object> getVideoList(SearchItem searchItem) {

       StorageDto.getStorageList a =  storageService.getVideoList(searchItem);

        System.out.println("10번 반환직전" + new Date());
        return () -> a;
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

        return storageService.videoHlsTs(fileName, tsName);
    }

    // 자막 로드
    @WithoutJwtCallable
    @GetMapping("/vtt/{videoId}.vtt")
    public ResponseEntity<Resource> videoVtt(@PathVariable Long videoId) {
        System.out.println("날드 안눙");
        return storageService.videoVtt(videoId);
    }

    // 파일 타입 변환
    @RequireAuthSuper
    @GetMapping("/convertVideoHls/{videoId}")
    public void convertVideoHls(@PathVariable Long videoId) {
        storageService.requestConvertVideoHls(videoId);
    }

    // 업로드 상태 확인
    @GetMapping("/getConvertVideoStatus/{videoId}")
    public Callable<Object> getConvertVideoStatus(@PathVariable Long videoId) {
        return () ->  storageService.getConvertVideoStatus(videoId);
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
                                        @RequestPart(value = "file", required = true) MultipartFile file,
                                        @RequestPart(value = "fileVtt", required = false) MultipartFile fileVtt,
                                        HttpServletRequest request) {
        info.setFile(file);

        info.setFileVtt(fileVtt);
        return () -> storageService.uploadVideo(info);
    }


}
