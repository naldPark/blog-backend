package me.nald.blog.data.model;

import lombok.*;

import javax.validation.constraints.NotBlank;

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
    private int authority;
    @NotBlank
    private String email;

}