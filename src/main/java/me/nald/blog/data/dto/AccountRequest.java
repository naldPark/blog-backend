package me.nald.blog.data.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import jakarta.validation.constraints.NotNull;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {

    @NotBlank
    private String accountId;
    @NotBlank
    private String accountName;
    @NotBlank
    private String password;
    @NotBlank
    private String authority;
    @NotBlank
    private String email;

}