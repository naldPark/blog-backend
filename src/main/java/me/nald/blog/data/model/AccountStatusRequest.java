package me.nald.blog.data.model;

import lombok.*;

import java.util.List;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatusRequest {

    private List<String> userIds;
    private int status;

}