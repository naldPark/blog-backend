package me.nald.blog.response;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.HashMap;

@Getter
@Setter
@ToString
public class Response {

    private HttpStatus status;
    private int statusCode;

    public Response() {
        status = HttpStatus.OK;
        statusCode = HttpStatus.OK.value();
    }

    public Response(HttpStatus status) {
        this.status = status;
        this.statusCode = status.value();
    }

    public static Response of() {
        return new Response();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class CommonRes {
        private int statusCode;
        private Object data;

        @Builder
        private CommonRes(int statusCode, Object data) {
            this.statusCode = statusCode;
            this.data = data;
        }
    }
}
