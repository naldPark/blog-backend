package me.nald.blog.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.entity.Account;
import me.nald.blog.data.vo.AccountVo;
import me.nald.blog.exception.AuthException;
import me.nald.blog.response.ResponseCode;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static me.nald.blog.util.Constants.AUTHORITY;
import static me.nald.blog.util.Constants.USER_ID;

@Component
@Slf4j
@AllArgsConstructor
public class CommonUtils {

  private static BlogProperties blogProperties;

  private static final Gson gson = new Gson();

  @Autowired
  public void setBlogProperties(BlogProperties blogProperties) {
    this.blogProperties = blogProperties;
  }


  public static String getJWTToken(Account user) {
    String jwt = "";
    String privateKey = blogProperties.getPrivateKey();
    String publicKey = blogProperties.getPublicKey();

    try {
      Map<String, Object> headers = new HashMap<>();
      headers.put("typ", "JWT");
      headers.put("alg", "RS256");

      // 만료 시간 설정 (1시간 후)
      long timeValue = blogProperties.getTokenExpiredTime();
      long expiredTimeMillis = 1000 * 60L * 60L * timeValue;
      Date now = new Date();
      Date expirationDate = new Date(now.getTime() + expiredTimeMillis);
      Map<String, Object> payloads = new HashMap<>();
      Algorithm algorithm = Algorithm.RSA256(toPublicKey(publicKey), toPrivateKey(privateKey));
      payloads.put("user_name", user.getAccountName());
      payloads.put("user_email", user.getEmail());
      payloads.put("authority", user.getAuthority());
      payloads.put("jti", UUID.randomUUID().toString());
      payloads.put("user_id", user.getAccountId());

      com.auth0.jwt.JWTCreator.Builder builder = JWT.create()
              .withHeader(headers)
              .withIssuedAt(new Date())
              .withExpiresAt(expirationDate);

      for (Map.Entry<String, Object> entry : payloads.entrySet()) {
        builder.withClaim(entry.getKey(), entry.getValue().toString());
      }

      jwt = builder.sign(algorithm);

    } catch (Exception e) {
      e.printStackTrace();
    }

    return jwt;
  }

  private static Object toKey(String stringKey, boolean isPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    byte[] keyBytes = Base64.getDecoder().decode(stringKey);

    KeySpec keySpec;
    if (isPrivateKey) {
      keySpec = new PKCS8EncodedKeySpec(keyBytes);
      return keyFactory.generatePrivate(keySpec);
    } else {
      keySpec = new X509EncodedKeySpec(keyBytes);
      return keyFactory.generatePublic(keySpec);
    }
  }

  public static RSAPrivateKey toPrivateKey(String stringPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    return (RSAPrivateKey) toKey(stringPrivateKey, true);
  }

  public static RSAPublicKey toPublicKey(String stringPublicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    return (RSAPublicKey) toKey(stringPublicKey, false);
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

  public static List<HashMap<String, Object>> stringListToHashMapList(List<String> list) {
    return list.stream()
            .map(jsonString -> gson.fromJson(jsonString, new TypeToken<HashMap<String, Object>>() {
            }))
            .collect(Collectors.toList());
  }

  public static AccountVo extractUserIdFromJwt(HttpServletRequest request) throws NoSuchAlgorithmException, InvalidKeySpecException {
    String jwtToken = request.getHeader("Authorization");
    String[] tmp = jwtToken.split("\\.");
    String base64EncodedBody = tmp[1];
    org.apache.commons.codec.binary.Base64 base64Url = new org.apache.commons.codec.binary.Base64(true);
    JSONObject body = new JSONObject(new String(base64Url.decode(base64EncodedBody)));
    if (body.getLong("exp") * 1000 > System.currentTimeMillis()) {
      AccountVo jwtInfo = AccountVo.jsonToObj(body);
      String userId = body.getString(USER_ID);
      if (!CommonUtils.verifyToken(jwtToken, userId, blogProperties.getPublicKey())) {
        new AuthException(log, ResponseCode.INVALID_AUTH_TOKEN);
      }
      request.setAttribute(AUTHORITY, body.getInt(AUTHORITY));
      request.setAttribute(USER_ID, body.getString(USER_ID));
      return jwtInfo;
    } else {
      throw new AuthException(log, ResponseCode.EXPIRED_AUTH_TOKEN);
    }
  }

  public static boolean verifyToken(String token, String userId, String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
    boolean result = true;
    String publicKey = blogProperties.getPublicKey();
    RSAPublicKey rsaPublicKey = toPublicKey(publicKey);
    Algorithm algorithm = Algorithm.RSA256(rsaPublicKey, null);
    JWTVerifier verifier = JWT.require(algorithm).withClaim("user_id", userId).build();
    verifier.verify(token);
    return result;
  }

  public static String dataToAge(LocalDateTime date) {
    LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    long seconds = Duration.between(date, now).getSeconds();

    return switch ((seconds >= 86_400) ? 1 :
            (seconds >= 3_600) ? 2 :
                    (seconds >= 60) ? 3 : 4) {
      case 1 -> String.format("%dd", seconds / 86_400);
      /** underbar는 자릿수를 구분하기 위한 기호 **/
      case 2 -> String.format("%dh", seconds / 3_600);
      case 3 -> String.format("%dm", seconds / 60);
      case 4 -> String.format("%ds", seconds);
      default -> throw new IllegalStateException("Unexpected value: " + seconds);
    };
  }
}
