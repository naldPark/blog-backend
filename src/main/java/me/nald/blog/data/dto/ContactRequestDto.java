package me.nald.blog.data.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;


@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequestDto {

    @NotNull
    private String title;

    @NotNull
    private String content;

    @NotNull
    private String email;

    @NotNull
    private String name;

}