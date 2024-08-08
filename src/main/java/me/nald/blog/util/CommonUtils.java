package me.nald.blog.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.entity.Account;
import me.nald.blog.data.vo.AccountVO;
import me.nald.blog.exception.Errors;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static me.nald.blog.exception.ErrorSpec.AccessDeniedException;
import static me.nald.blog.util.Constants.AUTHORITY;
import static me.nald.blog.util.Constants.USER_ID;

@Component
@AllArgsConstructor
public class CommonUtils {

  private static BlogProperties blogProperties;

  private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);

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

  private static RSAPrivateKey toPrivateKey(String stringPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    byte[] bytePrivateKey = Base64.getDecoder().decode(stringPrivateKey.getBytes());
    PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bytePrivateKey);
    RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);
    return privateKey;
  }

  private static RSAPublicKey toPublicKey(String stringPublicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    byte[] publicKeyBytes = Base64.getDecoder().decode(stringPublicKey);
    X509EncodedKeySpec keySpecPublic = new X509EncodedKeySpec(publicKeyBytes);
    RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpecPublic);
    return publicKey;
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

    List<HashMap<String, Object>> mapList = new ArrayList<>();
    JsonParser parser = new JsonParser();
    list.stream().forEach(f -> {
              mapList.add(new Gson().fromJson(parser.parse(f).getAsJsonObject().toString(), HashMap.class));
            }
    );
    return mapList;
  }

  public static AccountVO extractUserIdFromJwt(HttpServletRequest request) throws NoSuchAlgorithmException, InvalidKeySpecException {
    String jwtToken = request.getHeader("Authorization");
    String[] tmp = jwtToken.split("\\.");
    String base64EncodedBody = tmp[1];
    org.apache.commons.codec.binary.Base64 base64Url = new org.apache.commons.codec.binary.Base64(true);
    JSONObject body = new JSONObject(new String(base64Url.decode(base64EncodedBody)));
    if (body.getLong("exp") * 1000 > System.currentTimeMillis()) {
      AccountVO jwtInfo = AccountVO.jsonToObj(body);
      String userId = body.getString(USER_ID);
      if (!CommonUtils.verifyToken(jwtToken, userId, blogProperties.getPublicKey())) {
        throw Errors.of(AccessDeniedException, "Invalid token");
      }
      request.setAttribute(AUTHORITY, body.getInt(AUTHORITY));
      request.setAttribute(USER_ID, body.getString(USER_ID));
      return jwtInfo;
    } else {
      throw Errors.of(AccessDeniedException, "Expired token");
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

  public static String dataToAge(Date date) {
    SimpleDateFormat sformat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
    String dateToString = sformat.format(date);
    sformat.setTimeZone(TimeZone.getTimeZone("UTC"));
    Long runningTime = null;
    try {
      runningTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - sformat.parse(dateToString).getTime();
    } catch (ParseException e) {
      e.printStackTrace();
    }
    String result = "";
    if (TimeUnit.MILLISECONDS.toDays(runningTime) > 0) {
      result = String.format("%dd", TimeUnit.MILLISECONDS.toDays(runningTime));
    } else if (TimeUnit.MILLISECONDS.toHours(runningTime) > 0) {
      result = String.format("%dh", TimeUnit.MILLISECONDS.toHours(runningTime));
    } else if (TimeUnit.MILLISECONDS.toMinutes(runningTime) > 0) {
      result = String.format("%dm", TimeUnit.MILLISECONDS.toMinutes(runningTime));
    } else if (TimeUnit.MILLISECONDS.toSeconds(runningTime) > 0) {
      result = String.format("%ds", TimeUnit.MILLISECONDS.toSeconds(runningTime));
    }
    return result;
  }

}
