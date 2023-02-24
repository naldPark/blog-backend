package me.nald.blog.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.exception.Errors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;

import static me.nald.blog.exception.ErrorSpec.*;
import static me.nald.blog.exception.ErrorSpec.PermissionDenied;
import static me.nald.blog.util.Constants.*;

@Component
@AllArgsConstructor
public class Util {

    private static BlogProperties blogProperties;

    private static final Logger log = LoggerFactory.getLogger(Util.class);

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


    public static String getJWTToken(Account user) {
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
            payloads.put("user_name", user.getAccountName());
            payloads.put("authority", user.getAuthority());
            payloads.put("jti", UUID.randomUUID().toString());
            payloads.put("user_id", user.getAccountId());

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

    public static String extractUserIdFromJwt(HttpServletRequest request) {
        try {
            String jwtToken = request.getHeader("Authorization");
            String tokenStr = jwtToken.substring("Bearer ".length());
            String[] tmp = tokenStr.split("\\.");
            String base64EncodedBody = tmp[1];
            org.apache.commons.codec.binary.Base64 base64Url = new org.apache.commons.codec.binary.Base64(true);
            JSONObject body = new JSONObject(new String(base64Url.decode(base64EncodedBody)));
            if (body.getLong("exp") * 1000 > System.currentTimeMillis()) {
                String userId = body.getString(USER_ID);
                if (!Util.verifyToken(tokenStr, userId, blogProperties.getPublicKey())) {
                    throw Errors.of(AccessDeniedException, "Invalid token");
                }
                request.setAttribute(AUTHORITY, body.getInt(AUTHORITY));
                request.setAttribute(USER_ID, body.getString(USER_ID));
                return userId;
            } else {
                throw Errors.of(AccessDeniedException, "Expired token");
            }
        } catch (Exception e) {
            throw Errors.of(AccessDeniedException, "Token parsing error");
        }
    }

    static RSAPublicKey rsaPublicKey;

    public static boolean verifyToken(String token, String userId, String key) {
        boolean result = true;
        try {
            if (rsaPublicKey == null) {
                rsaPublicKey = publicKeyFromString(key);
            }
            Algorithm algorithm = Algorithm.RSA256(rsaPublicKey, null);
            JWTVerifier verifier = JWT.require(algorithm).withClaim("user_id", userId).build();
            verifier.verify(token);
        } catch (Exception e) {
            log.error("verifyToken Error id: {}, token: {}", userId, token, e);
            result = false;
        }
        return result;
    }

    public static RSAPublicKey publicKeyFromString(String key) {
        try {
            byte[] pubKeyBytes = java.util.Base64.getDecoder().decode(key);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pubKeyBytes);
            return (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
        } catch (Exception e) {
            log.error("Error loading public key", e);
        }
        return null;
    }

}
