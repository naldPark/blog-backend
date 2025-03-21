package me.nald.blog.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import me.nald.blog.annotation.PermissionCallable;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.dto.StorageRequestDto;
import me.nald.blog.data.vo.Authority;
import me.nald.blog.data.vo.SearchItem;
import me.nald.blog.response.ResponseObject;
import me.nald.blog.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.Callable;

@AllArgsConstructor
@RestController
@RequestMapping("/storage")
public class StorageController {


  private final StorageService storageService;

  @GetMapping("/video/list")
  public Callable<ResponseObject> getVideoList(SearchItem searchItem) {
    return () -> storageService.getVideoList(searchItem);
  }

  // 단 건 비디오 상세정보
  @GetMapping("/video/detail/{videoId}")
  public Callable<ResponseObject> getVideoDetail(@PathVariable Long videoId) {
    return () -> storageService.getVideoDetail(videoId);
  }

  // 스트리밍 최초 정보 로드
  @WithoutJwtCallable
  @GetMapping("{fileName}/hls/{fileName}.m3u8")
  public ResponseEntity<Resource> videoHlsM3U8(@PathVariable String fileName) {
    return storageService.videoHlsM3U8(fileName);
  }

  // 스트리밍 파일 호출
  @WithoutJwtCallable
  @GetMapping("{fileName}/hls/{tsName}.ts")
  public ResponseEntity<Resource> videoHlsTs(@PathVariable String fileName, @PathVariable String tsName) {

    return storageService.videoHlsTs(fileName, tsName);
  }

  // 자막 로드
  @WithoutJwtCallable
  @GetMapping("/vtt/{videoId}.vtt")
  public ResponseEntity<Resource> videoVtt(@PathVariable Long videoId) {
    return storageService.videoVtt(videoId);
  }

  // 파일 타입 변환
  @GetMapping("/convert/{videoId}")
  public void convertVideoHls(@PathVariable Long videoId) {
    storageService.requestConvertVideoHls(videoId);
  }

  // 업로드 상태 확인
  @GetMapping("/status/{videoId}")
  public Callable<ResponseObject> getConvertVideoStatus(@PathVariable Long videoId) {
    return () -> storageService.getConvertVideoStatus(videoId);
  }

  // 다운로드 파일
  @GetMapping("/download/{videoId}")
  public ResponseEntity<Resource> downloads(@PathVariable Long videoId) {
    return storageService.downloads(videoId);
  }

  @PermissionCallable(authority = Authority.SUPER)
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, value = "/uploadLocal")
  public Callable<ResponseObject> uploadVideo(
          @RequestPart StorageRequestDto info,
          @RequestPart(value = "videoFile") MultipartFile videoFile,
          @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
          @RequestPart(value = "vttFile", required = false) MultipartFile vttFile) {

    return () -> storageService.uploadVideo(info, videoFile, coverFile, vttFile);
  }

  @PermissionCallable(authority = Authority.SUPER)
  @DeleteMapping
  public Callable<ResponseObject> deleteVideos(@Valid @RequestBody List<Long> videoIds) {
    return () -> storageService.deleteVideo(videoIds);
  }

  @PermissionCallable(authority = Authority.SUPER)
  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,value="")
  public Callable<ResponseObject> editVideo(@RequestPart StorageRequestDto info,
                                           @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
                                           @RequestPart(value = "vttFile", required = false) MultipartFile vttFile) {
    return () -> storageService.editVideo(info,coverFile,vttFile);
  }

}
