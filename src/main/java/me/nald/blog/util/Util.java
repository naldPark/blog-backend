package me.nald.blog.util;

import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.exception.ErrorSpec;
import org.springframework.beans.factory.annotation.Autowired;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

@Component
@AllArgsConstructor
public class Util {

    private static BlogProperties blogProperties;

    @Autowired
    public void setBlogProperties(BlogProperties blogProperties) {
        this.blogProperties = blogProperties;
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




}
