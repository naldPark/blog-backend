package me.nald.blog.data.model;

import com.sun.istack.NotNull;
import lombok.*;


@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequest {

    @NotNull
    private String title;

    @NotNull
    private String content;

    @NotNull
    private String email;

    @NotNull
    private String name;

}