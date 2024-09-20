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
    private String fileType;
    private String fileDesc;
    private Boolean fileAuth;
    private Boolean downloadable;
//    @NotBlank
//    private MultipartFile file;
//    private MultipartFile fileCover;
//    private MultipartFile fileVtt;


}