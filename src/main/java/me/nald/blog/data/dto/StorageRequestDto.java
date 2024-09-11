package me.nald.blog.data.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

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
//    @NotBlank
//    private MultipartFile file;
//    private MultipartFile fileCover;
//    private MultipartFile fileVtt;


}