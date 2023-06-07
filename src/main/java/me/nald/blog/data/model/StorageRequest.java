package me.nald.blog.data.model;

import lombok.*;
import me.nald.blog.data.vo.YN;

import javax.persistence.Column;
import javax.validation.constraints.NotBlank;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StorageRequest {

//    @NotBlank
//    private String accountId;
    @NotBlank
    private String fileName;
    @NotBlank
    private Long fileSize;
    @NotBlank
    private String fileType;
    @NotBlank
    private String fileCover;
    @NotBlank
    private YN fileAuth;

}