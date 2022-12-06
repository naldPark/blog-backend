package me.nald.blog.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.exception.ErrorSpec;
import org.springframework.beans.factory.annotation.Autowired;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@AllArgsConstructor
public class Util {

    private static BlogProperties blogProperties;

    @Autowired
    public void setBlogProperties(BlogProperties blogProperties) {
        this.blogProperties = blogProperties;
    }

    public static String getUUID() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        while (Character.isDigit(uuid.charAt(0))) {
            uuid = UUID.randomUUID().toString().replace("-", "");
        }
        String[] splits = uuid.split("(?<=\\G.{" + 4 + "})");
        return String.join("-", splits);
    }

    public static String getUUIDWithoutDash() {
        return UUID.randomUUID().toString().replace("-", "");
    }


    public static String getJWTToken(AccountDto.LoginInfo loginInfo) {
        String jwt = "";
        String privateKey = blogProperties.getPrivateKey();

        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("typ", "JWT");
            headers.put("alg", "RS256");

            long timeValue = blogProperties.getTokenExpiredTime();
            long expiredTime = 1000 * 60L * 60L * timeValue;

            Date exp = new Date();
            exp.setTime(exp.getTime() + expiredTime);

            Map<String, Object> payloads = new HashMap<>();

            payloads.put("exp", exp);
            payloads.put("account_id", loginInfo.getAccountName());
            payloads.put("authorities", loginInfo.getAuthority());
            payloads.put("jti", UUID.randomUUID().toString());
            payloads.put("client_id", loginInfo.getAccountId());

            jwt = Jwts.builder()
                    .setHeader(headers)
                    .setClaims(payloads)
                    .setExpiration(new Date(System.currentTimeMillis() + expiredTime))
                    .signWith(SignatureAlgorithm.RS256, toPrivateKey(privateKey))
                    .compact();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return jwt;
    }

    private static PrivateKey toPrivateKey(String stringPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //평문으로 전달받은 개인키를 개인키객체로 만드는 과정
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] bytePrivateKey = Base64.getDecoder().decode(stringPrivateKey.getBytes());
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bytePrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
        return privateKey;
    }

    public static String encryptSHA256(String str) {
        String sha = "";
        try {
            MessageDigest sh = MessageDigest.getInstance("SHA-256");
            sh.update(str.getBytes());
            byte[] byteData = sh.digest();
            StringBuilder sb = new StringBuilder();
            for (byte byteDatum : byteData) {
                sb.append(Integer.toString((byteDatum & 0xff) + 0x100, 16).substring(1));
            }
            sha = sb.toString();
        } catch (NoSuchAlgorithmException e) {
        }
        return sha;
    }

    public static HashMap stringToHashMap(String data) {
        return new Gson().fromJson(data.toString(), HashMap.class);
    }

    public static List<HashMap<String, Object>> stringListToHashMapList(List<String> list) {

        List<HashMap<String, Object>> mapList = new ArrayList<>();
        JsonParser parser = new JsonParser();
        list.stream().forEach(f -> {
            mapList.add(new Gson().fromJson(parser.parse(f).getAsJsonObject().toString(), HashMap.class));
                }
        );
        return mapList;
    }


}
