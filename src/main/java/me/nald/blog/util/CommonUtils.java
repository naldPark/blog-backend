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
import me.nald.blog.exception.UnauthorizedException;
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
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static me.nald.blog.util.Constants.AUTHORITY;
import static me.nald.blog.util.Constants.USER_ID;

@Slf4j
@Component
@AllArgsConstructor
public class CommonUtils {

  private static BlogProperties blogProperties;

  private static final Gson gson = new Gson();

  @Autowired
  public void setBlogProperties(BlogProperties blogProperties) {
    this.blogProperties = blogProperties;
  }


  public static String getJWTToken(Account user) {
    try {
      String privateKey = blogProperties.getPrivateKey();
      String publicKey = blogProperties.getPublicKey();

      long expiredTimeMillis = 1000 * 60L * 60L * blogProperties.getTokenExpiredTime();
      Date expirationDate = new Date(System.currentTimeMillis() + expiredTimeMillis);

      Algorithm algorithm = Algorithm.RSA256(toPublicKey(publicKey), toPrivateKey(privateKey));

      return JWT.create()
              .withHeader(Map.of("typ", "JWT", "alg", "RS256"))
              .withIssuedAt(new Date())
              .withExpiresAt(expirationDate)
              .withClaim("user_name", user.getAccountName())
              .withClaim("user_email", user.getEmail())
              .withClaim("authority", user.getAuthority())
              .withClaim("jti", UUID.randomUUID().toString())
              .withClaim("user_id", user.getAccountId())
              .sign(algorithm);
    } catch (Exception e) {
      log.error("Failed to generate JWT token", e);
      return "";
    }
  }


  private static Object toKey(String stringKey, boolean isPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    byte[] keyBytes = Base64.getDecoder().decode(stringKey);

    return isPrivateKey ? keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes))
            : keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
  }

  public static RSAPrivateKey toPrivateKey(String stringPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    return (RSAPrivateKey) toKey(stringPrivateKey, true);
  }

  public static RSAPublicKey toPublicKey(String stringPublicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    return (RSAPublicKey) toKey(stringPublicKey, false);
  }


  public static String encryptSHA256(String str) {
    try {
      MessageDigest sh = MessageDigest.getInstance("SHA-256");
      byte[] byteData = sh.digest(str.getBytes());
      StringBuilder sb = new StringBuilder();
      for (byte byteDatum : byteData) {
        sb.append(String.format("%02x", byteDatum));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      log.error("SHA-256 encryption failed", e);
      return "";
    }
  }

  public static List<HashMap<String, Object>> stringListToHashMapList(List<String> list) {
    return list.stream()
            .map(jsonString -> gson.fromJson(jsonString, new TypeToken<HashMap<String, Object>>() {
            }))
            .collect(Collectors.toList());
  }

  public static AccountVo extractUserIdFromJwt(HttpServletRequest request) {
    try {


      String jwtToken = Optional.ofNullable(request.getHeader("Authorization"))
              .orElseThrow(() -> new UnauthorizedException(log, ResponseCode.INVALID_AUTH_TOKEN));

      String[] jwtParts = jwtToken.split("\\.");
      String base64EncodedBody = jwtParts[1];
      JSONObject body = new JSONObject(new String(Base64.getUrlDecoder().decode(base64EncodedBody)));

      if (body.getLong("exp") * 1000 > System.currentTimeMillis()) {
        AccountVo jwtInfo = AccountVo.jsonToObj(body);
        String userId = body.getString(USER_ID);
        verifyToken(jwtToken, userId);
        request.setAttribute(AUTHORITY, body.getInt(AUTHORITY));
        request.setAttribute(USER_ID, userId);
        return jwtInfo;
      } else {
        throw new UnauthorizedException(log, ResponseCode.EXPIRED_AUTH_TOKEN);
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new UnauthorizedException(log, ResponseCode.INVALID_AUTH_TOKEN);
    } catch (Exception e) {
      throw new UnauthorizedException(log, ResponseCode.UNKNOWN_AUTH_ERROR);
    }
  }

  public static boolean verifyToken(String token, String userId) {
    try {
      String publicKey = blogProperties.getPublicKey();
      Algorithm algorithm = Algorithm.RSA256(toPublicKey(publicKey), null);
      JWTVerifier verifier = JWT.require(algorithm).withClaim("user_id", userId).build();
      verifier.verify(token);
    } catch (InvalidKeySpecException e) {
      throw new UnauthorizedException(log, ResponseCode.INVALID_AUTH_TOKEN);
    } catch (NoSuchAlgorithmException e) {
      throw new UnauthorizedException(log, ResponseCode.UNKNOWN_AUTH_ERROR);
    }

    return true;
  }

  public static String dataToAge(LocalDateTime date) {
    long seconds = Duration.between(date, LocalDateTime.now(ZoneId.of("Asia/Seoul"))).getSeconds();

    return switch ((seconds >= 86_400) ? 1 : (seconds >= 3_600) ? 2 : (seconds >= 60) ? 3 : 4) {
      case 1 -> "%dd".formatted(seconds / 86_400);
      case 2 -> "%dh".formatted(seconds / 3_600);
      case 3 -> "%dm".formatted(seconds / 60);
      case 4 -> "%ds".formatted(seconds);
      default -> throw new IllegalStateException("Unexpected value: " + seconds);
    };
  }
}
