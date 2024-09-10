package me.nald.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.dto.StorageRequestDto;
import me.nald.blog.data.dto.StorageResponseDto;
import me.nald.blog.data.entity.QStorage;
import me.nald.blog.data.entity.Storage;
import me.nald.blog.data.vo.SearchItem;
import me.nald.blog.data.vo.YN;
import me.nald.blog.exception.NotFoundException;
import me.nald.blog.exception.UnauthorizedException;
import me.nald.blog.repository.StorageRepository;
import me.nald.blog.response.ResponseCode;
import me.nald.blog.response.ResponseObject;
import me.nald.blog.util.FileUtils;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static me.nald.blog.util.BooleanBuilderUtils.checkContainCondition;
import static me.nald.blog.util.BooleanBuilderUtils.checkEqualCondition;


@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StorageService {


  private final JPAQueryFactory queryFactory;
  private final BlogProperties blogProperties;
  private final StorageRepository storageRepository;

  public ResponseObject getVideoList(SearchItem searchItem) {
    ResponseObject response = new ResponseObject();
    QStorage storage = QStorage.storage;
    BooleanBuilder builder = new BooleanBuilder();
    checkContainCondition(builder, searchItem.getSearchText(), storage.fileName);
    checkEqualCondition(builder,searchItem.getType(),storage.fileType);
    Pageable pageable = (searchItem.getPageNumber() != null && searchItem.getPageSize() != null)
            ? PageRequest.of(searchItem.getPageNumber(), searchItem.getPageSize(), Sort.by("fileName").ascending())
            : null;

    JPAQuery<Storage> query = queryFactory
            .selectFrom(storage)
            .distinct()
            .from(storage)
            .where(builder);

    // 페이지네이션 처리
    if (pageable != null) {
      query.offset(pageable.getOffset())
              .limit(pageable.getPageSize());
    }

    // 정렬 처리
    if ("random".equals(searchItem.getSort())) {
      query.orderBy(Expressions.numberTemplate(Double.class, "function('rand')").asc());
    } else {
      query.orderBy(storage.fileName.asc().nullsLast());
    }

    // 결과 리스트 생성
    List<StorageResponseDto.StorageInfo> list = query.fetch().stream()
            .map(StorageResponseDto.StorageInfo::new)
            .collect(Collectors.toList());

    // 응답 데이터 구성
    Map<String, Object> data = new HashMap<>();
    data.put("list", list);
    data.put("total", list.size());
    response.putData(data);
    return response;
  }

  public ResponseObject getVideoDetail(Long videoId) {
    ResponseObject response = new ResponseObject();
    Storage storage = storageRepository.getReferenceById(videoId);
    StorageResponseDto.StorageInfo res = new StorageResponseDto.StorageInfo(storage);
    response.putData(res);
    return response;
  }

  @Transactional
  public void requestConvertVideoHls(Long videoId) {
    Storage storage = storageRepository.getReferenceById(videoId);
    storage.setStatus(Storage.Status.Progressing);
    storageRepository.save(storage);
    convertVideoHls(storage);
  }

  public ResponseObject getConvertVideoStatus(Long videoId) {
    ResponseObject response = new ResponseObject();
    Storage storage = storageRepository.getReferenceById(videoId);
    response.put("status", storage.getStatus().toString());

    return response;
  }

  @Transactional
  public void convertVideoHls(Storage storage) {
    if (storage.getFileSrc() != null) {
      throw new NotFoundException(log, ResponseCode.FILE_ALREADY_EXISTS);
    } else {
      try {
        String movieDir = blogProperties.getCommonPath() + "/movie";
        String fileName = FilenameUtils.getBaseName(storage.getDownloadSrc());
        String inputPath = movieDir + storage.getDownloadSrc();
        String hlsPath = "/hls/" + fileName + "/";

        File folder = new File(movieDir + hlsPath);
        if (!folder.exists()) {
          folder.mkdir();
        }
        FFmpeg ffmpeg = new FFmpeg(blogProperties.getFfmpegPath() + "/ffmpeg");
        FFprobe ffprobe = new FFprobe(blogProperties.getFfmpegPath() + "/ffprobe");

        FFmpegBuilder builder = new FFmpegBuilder()
                .overrideOutputFiles(true) //오버라이드
                .setInput(inputPath) //파일 경로
                .addOutput(movieDir + hlsPath + fileName + ".m3u8") //저장위치
//                        .disableSubtitle() // 자막제거

                .setAudioChannels(2) // 오디오 채널 ( 1 : 모노 , 2 : 스테레오 )
                .addExtraArgs("-profile:v", "baseline")
                .addExtraArgs("-level", "3.0")
                .addExtraArgs("-start_number", "0")
                .addExtraArgs("-hls_time", "15") // 몇초로 짜를까
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-f", "hls")
                .addExtraArgs("-safe", "0")
                .addExtraArgs("-preset", "ultrafast")
//                        .setVideoResolution(1920, 1080)  //해상도 용량이 너무 커져 ㅠㅠ
                .setVideoResolution(1280, 720)
//                        .setVideoResolution(854, 480)
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)

                //고화질할때 주석처리 하는 애들
//                        .setVideoCodec("libx264") //비디오 코덱
//                        .setVideoBitRate(1464800) //비트레이트
//                        .setVideoFrameRate(30) //프레임

                .done();

        builder.setVerbosity(FFmpegBuilder.Verbosity.INFO);
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        FFmpegJob job = executor.createJob(builder);
        System.out.println("before run job state!!! is " + job.getState());
        job.run();
        storage.setFileSrc(hlsPath + fileName + ".m3u8");
        storage.setStatus(Storage.Status.Converted);
        storageRepository.save(storage);
      } catch (IOException e) {
        storage.setStatus(Storage.Status.Inactive);
        storageRepository.save(storage);
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

  public ResponseEntity<Resource> videoVtt(Long videoId) {
    Storage storage = storageRepository.getReferenceById(videoId);
    String movieDir = blogProperties.getCommonPath() + "/movie";
    String fileFullPath = movieDir + storage.getVttSrc();
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
    Storage storage = storageRepository.getReferenceById(videoId);
    if (storage.getFileDownload().isN()) {
      throw new UnauthorizedException(log, ResponseCode.ACCESS_DENIED);
    }
    Path filePath = Paths.get(blogProperties.getCommonPath() + "/movie" + storage.getDownloadSrc());
    try {
      Resource resource = new FileSystemResource(filePath) {
        @NotNull
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

  public File multipartFileToFile(MultipartFile multipartFile) throws IOException {
    File file = new File(Objects.requireNonNull(multipartFile.getOriginalFilename()));
    multipartFile.transferTo(file);
    return file;
  }


  @Transactional
  public ResponseObject uploadVideo(StorageRequestDto info) throws IOException {
    String movieDir = blogProperties.getCommonPath() + "/movie";
    HashMap<String, String> phoneticSymbol = new ObjectMapper()
            .readValue(new ClassPathResource("phoneticSymbol.json").getInputStream(), HashMap.class);
    ResponseObject response = new ResponseObject();

    /** when filename is Korean, translate to English  */
    String nameToEng = "";
    for (String fileChar : info.getFileName().split("")) {
      if (Pattern.matches("^[0-9a-zA-Z]*$", fileChar)) {
        nameToEng += fileChar;
      } else if (phoneticSymbol.get(fileChar) != null) {
        nameToEng += phoneticSymbol.get(fileChar);
      }
      if (nameToEng.length() > 20) break;
    }
    String saveFileName = "/" + nameToEng + System.currentTimeMillis() + (int) (Math.random() * 100);
    String uploadPath = "/upload" + saveFileName;
    HashMap<String, Object> map = new HashMap<>();
    CompletableFuture<Storage> future = CompletableFuture.supplyAsync(() -> {
      Storage storageInfo = Storage.createStorage(
              info.getFileName(),
              info.getFileSize(),
              null,
              info.getCategory(),
              null,
              info.getDescription(),
              null,
              null,
              YN.convert(info.getFileAuth()),
              YN.convert(info.getFileDownload())
      );
      try {
        if (info.getFileVtt() != null) {
          FileUtils.createDirectoriesIfNotExists(movieDir + uploadPath);
          MultipartFile multipartFileVtt = info.getFileVtt();
          //TODO 확장자 .vtt하드코딩해놨음;
          storageInfo.setVttSrc(uploadPath + saveFileName + getMultiFileExt(multipartFileVtt));
          Path path = Paths.get(movieDir + uploadPath + saveFileName + ".vtt").toAbsolutePath();
          multipartFileVtt.transferTo(path.toFile());
          Files.delete(path);
        }
        if (info.getFile() != null) {
          storageInfo.setDownloadSrc(uploadPath + saveFileName + getMultiFileExt(info.getFile()));
          FileUtils.createDirectoriesIfNotExists(movieDir + uploadPath);
          try (
                  FileOutputStream fos = new FileOutputStream(movieDir + storageInfo.getDownloadSrc());
                  InputStream is = info.getFile().getInputStream();

          ) {
            int readCount = 0;
            byte[] buffer = new byte[4096];
            while ((readCount = is.read(buffer)) != -1) {
              fos.write(buffer, 0, readCount);
            }
          } catch (Exception ex) {
            throw new RuntimeException("file Save Error");
          }

          multipartFileToFile(info.getFile()).delete();

          MultipartFile commonsMultipartFile = info.getFile();
//                    DiskFileItem diskFileItem = (DiskFileItem) commonsMultipartFile.getFileItem();
//                    String tempFilePath = diskFileItem.getStoreLocation().getPath();
//                    String tempFilenamePrefix = extractFilenamePrefix(tempFilePath);
          // stream으로 나눠받은 tmp파일 모두 삭제
//                    try {
//                        FileUtils.deleteFilesStartingWith(tempFilenamePrefix);
//                        System.out.println("모든 파일 삭제 완료.");
//                    } catch (IOException e) {
//                        System.err.println("파일 삭제 중 오류 발생: " + e.getMessage());
//                    }
        }

        if (info.getFileCover() != null) {
          //이거는 s3에 업로드하는 방식으로 변경해야함
//                    String[] imageInfo = info.getFileCover().split(",");
//                    String extension = imageInfo[0].replace("data:image/", "").replace(";base64", "");
//
//                    storageInfo.setFileCover(uploadPath + saveFileName + "." + extension);
          File file = new File(movieDir + storageInfo.getFileCover());
//                    OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
//                    outputStream.write(data);
          file.delete();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      storageInfo.setStatus(Storage.Status.Progressing);
      storageRepository.save(storageInfo);
      response.putData(storageInfo);
      return storageInfo;
    });
    future.thenAccept(this::convertVideoHls);
    return response;
  }

  @Transactional
  public ResponseObject deleteVideo(List<Long> seqList){
    storageRepository.deleteAllById(seqList);
    /** TODO: delete existing files */
    return new ResponseObject();
  }

}

