package me.nald.blog.response;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.HashMap;

@Getter
@Setter
@ToString
public class Response {

    private Status status;
    private int statusCode;

    public Response() {
        status = Status.OK;
        statusCode = HttpStatus.OK.value();
    }

    public Response status(Status status) {
        setStatus(status);
        return this;
    }

    public Response statusCode(int statusCode) {
        setStatusCode(statusCode);
        return this;
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

    public enum Status {
        OK,
        FAIL,
        INTERNAL_SERVER_ERROR;

        public boolean isOk() {
            return OK.equals(this) ? true : false;
        }
    }

}
