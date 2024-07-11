package me.nald.blog.data.dto;

import lombok.*;

import java.util.List;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatusRequestDto {

    private List<String> userIds;
    private int status;

}