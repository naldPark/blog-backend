package me.nald.blog.model;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;

@Slf4j
@Data
public class SearchItem {
    private int limit;
    private int offset;
    private String type;
    private String searchText;
    private String sort;


}
