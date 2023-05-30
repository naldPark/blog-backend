
package me.nald.blog.data.vo;

import lombok.*;
import me.nald.blog.data.persistence.entity.Account;
import org.json.JSONObject;

import java.util.Optional;

import static me.nald.blog.util.Constants.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVO {

    private String accountId;
    private String accountName;
    private int authority;


    public static AccountVO jsonToObj(JSONObject body) {
        return AccountVO.builder()
                        .accountId(body.getString(USER_ID))
                        .accountName(body.getString(USER_NAME))
                        .authority(body.getInt(AUTHORITY))
                        .build();
    }
}