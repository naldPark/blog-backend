package me.nald.blog.data.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StorageRequestDto {

    @NotBlank
    private String fileName;
    @NotBlank
    private Long fileSize;
    @NotBlank
    private String category;
    private String description;
    private Boolean fileAuth;
    private Boolean fileDownload;
    @NotBlank
    private MultipartFile file;
    private MultipartFile fileCover;
    private MultipartFile fileVtt;


}