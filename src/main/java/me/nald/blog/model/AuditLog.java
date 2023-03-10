package me.nald.blog.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.util.Constants;
import me.nald.blog.util.HttpServletRequestUtil;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Setter
@Getter
@Slf4j
@Builder
public class AuditLog {
    private String serviceName;
    private String userId;
    private String method;
    private String api;

    public static AuditLog from(HttpServletRequest request) {
        return AuditLog.builder()
                .serviceName("blog")
                .userId(Optional.ofNullable(request)
                        .map(req -> req.getAttribute(Constants.USER_ID))
                        .map(object -> object.toString())
                        .orElse("")
                )
                .method(Optional.ofNullable(request)
                        .map(req -> req.getMethod())
                        .orElse("")
                )
                .api(new StringBuilder(Optional.ofNullable(request).map(req -> req.getRequestURI()).orElse(""))
                        .append(Optional.ofNullable(request)
                                .map(HttpServletRequestUtil::getQueryParamsAsString)
                                .map(queryParams -> StringUtils.isEmpty(queryParams) ? "" : "?" + queryParams)
                                .orElse(""))
                        .toString()
                ).build();
    }
//
//    public synchronized void write() {
//        log.trace("{}", makeLine());
//    }

//    private String makeLine() {
//        List<String> inLine = new ArrayList<>();
//
//        inLine.add(serviceName);
//        inLine.add(userId);
//        inLine.add(method);
//        inLine.add(api);
//
//        return String.join("\t", inLine);
//    }
}
