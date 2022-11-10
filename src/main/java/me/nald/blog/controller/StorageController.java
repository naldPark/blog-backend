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
import org.springframework.http.*;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

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

//    @WithoutJwtCallable
//    @GetMapping(path = "/video")
//    public ResponseEntity<StreamingResponseBody> video() {
//        File file = new File("C:\\naldstorage\\sample.mp4");
//        if (!file.isFile()) {
//            return ResponseEntity.notFound().build();
//        }
//
//        StreamingResponseBody streamingResponseBody = new StreamingResponseBody() {
//            @Override
//            public void writeTo(OutputStream outputStream) throws IOException {
//                try {
//                    final InputStream inputStream = new FileInputStream(file);
//
//                    byte[] bytes = new byte[1024];
//                    int length;
//                    while ((length = inputStream.read(bytes)) >= 0) {
//                        outputStream.write(bytes, 0, length);
//                    }
//                    inputStream.close();
//                    outputStream.flush();
//
//                } catch (final Exception e) {
//                    log.error("Exception while reading and streaming data {} ", e);
//                }
//            }
//        };
//
//        final HttpHeaders responseHeaders = new HttpHeaders();
//        responseHeaders.add("Content-Type", "video/mp4");
//        responseHeaders.add("Content-Length", Long.toString(file.length()));
//
//        return ResponseEntity.ok().headers(responseHeaders).body(streamingResponseBody);
//    }

    @GetMapping(path = "/video", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Resource video() throws FileNotFoundException, IOException {
        return new ByteArrayResource(FileCopyUtils.copyToByteArray(new FileInputStream("/naldstorage/movie/HarryPotterAndTheSorcerersStone.mp4")));
    }
}
