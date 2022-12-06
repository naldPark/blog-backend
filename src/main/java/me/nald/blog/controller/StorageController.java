package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.service.AccountService;
import me.nald.blog.service.StorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@RestController
@RequestMapping("/storage")
@Slf4j
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
            } catch(Exception e) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        };
    }


    @GetMapping(path = "/video", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Resource videod() throws FileNotFoundException, IOException {
        return new ByteArrayResource(FileCopyUtils.copyToByteArray(new FileInputStream("C:\\naldstorage\\sample.mp4")));
    }

    @WithoutJwtCallable
    @GetMapping(value="/video2")
    public void viewMp4Stream (HttpServletRequest request , HttpServletResponse response)throws IOException {
        System.out.println("어섭쇼video2");
//        File file = new File("/nfs/movie/HarryPotterAndTheSorcerersStone.mp4");
        File file = new File("C:\\naldstorage\\sample.mp4");
        RandomAccessFile randomFile = new RandomAccessFile(file, "r");
        long rangeStart = 0; //요청 범위의 시작 위치
        long rangeEnd = 0; //요청 범위의 끝 위치
        boolean isPart=false; //부분 요청일 경우 true, 전체 요청의 경우 false
        try{ //동영상 파일 크기
            long movieSize = randomFile.length(); //스트림 요청 범위, request의 헤더에서 range를 읽는다.
            String range = request.getHeader("range");
            if(range!=null) {
                if (range.endsWith("-")) {
                    range = range + (movieSize - 1);
                }
                int idxm = range.trim().indexOf("-");
                rangeStart = Long.parseLong(range.substring(6, idxm));
                rangeEnd = Long.parseLong(range.substring(idxm + 1));
                if (rangeStart > 0) {
                    isPart = true;
                }
            }
            else {
                rangeStart =0;
                rangeEnd = movieSize -1;
            }
            long partSize = rangeEnd - rangeStart + 1;
            response.reset();
            response.setStatus(isPart ? 206 : 200);
            response.setContentType("video/mp4");
            response.setHeader("Content-Range", "bytes "+rangeStart+"-"+rangeEnd+"/"+movieSize);
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Length", ""+partSize);
            OutputStream out = response.getOutputStream();
            randomFile.seek(rangeStart);
            int bufferSize = 8*1024;
            byte[] buf = new byte[bufferSize];
            do{
                int block = partSize > bufferSize ? bufferSize : (int)partSize;
                int len = randomFile.read(buf, 0, block);
                out.write(buf, 0, len);
                partSize -= block;
            }while(partSize > 0);
        }catch(IOException e){
        }finally{
            randomFile.close();
        }
    }

    @RequestMapping(value = "/video3", method = RequestMethod.GET)
    public ResponseEntity<ResourceRegion> videoRegion(@RequestHeader HttpHeaders headers) throws Exception {
        System.out.println("어섭쇼video3");
        String path = "C:\\naldstorage\\sample.mp4";
        Resource resource = new FileSystemResource(path);

        long chunkSize = 1024 * 1024;
        long contentLength = resource.contentLength();

        ResourceRegion region;

        try {
            HttpRange httpRange = headers.getRange().stream().findFirst().get();
            long start = httpRange.getRangeStart(contentLength);
            long end = httpRange.getRangeEnd(contentLength);
            long rangeLength = Long.min(chunkSize, end -start + 1);

            log.info("start === {} , end == {}", start, end);

            region = new ResourceRegion(resource, start, rangeLength);
        } catch (Exception e) {
            long rangeLength = Long.min(chunkSize, contentLength);
            region = new ResourceRegion(resource, 0, rangeLength);
        }

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES))
                .contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .header("Accept-Ranges", "bytes")
                .eTag(path)
                .body(region);

    }
}
