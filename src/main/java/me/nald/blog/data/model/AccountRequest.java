package me.nald.blog.data.model;

import lombok.*;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {

    private String accountId;
    private String accountName;
    private String password;
    private int authority;

}