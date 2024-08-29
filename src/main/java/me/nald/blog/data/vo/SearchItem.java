package me.nald.blog.data.vo;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SearchItem {
    private int limit;
    private int offset;
    private String type;
    private String searchText;
    private String sort;
    private Boolean withCover;


}
