
package me.nald.blog.data.vo;

import lombok.*;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import static me.nald.blog.util.Constants.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVo {

    private String accountId;
    private String accountName;
    private int authority;
    private String accountEmail;


    public static AccountVo jsonToObj(JSONObject body) throws JSONException {
        return AccountVo.builder()
                        .accountId(body.getString(USER_ID))
                        .accountName(body.getString(USER_NAME))
                        .authority(body.getInt(AUTHORITY))
                        .accountEmail(body.getString(USER_EMAIL))
                        .build();
    }
}