package me.nald.blog.response;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class CommonResponse extends Response {

    private HashMap<String, Object> data;

    public CommonResponse() {
        super();
        data = new HashMap<>();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class OnPremiseTokenRes {
        private int statusCode;

        private Map<String, Object> data;

        @Builder
        private OnPremiseTokenRes(int statusCode, Map<String, Object > data) {
            this.statusCode = statusCode;
            this.data = data;
        }
    }
    
    private CommonResponse(HashMap<String, Object> data) {
        this();
        this.data.putAll(data);
    }

    public CommonResponse data(HashMap<String, Object> data) {
        setData(data);
        return this;
    }

    public CommonResponse putData(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public CommonResponse putResult(Object value) {
        data.put("result", value);
        return this;
    }

    public CommonResponse putMsgDto(Object value) {
        data.put("MsgDto", value);
        return this;
    }

    public static CommonResponse of() {
        return new CommonResponse();
    }

}
