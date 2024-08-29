package me.nald.blog.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.data.entity.Account;
import me.nald.blog.data.vo.AccountVo;
import me.nald.blog.exception.InternalServerErrorException;
import me.nald.blog.exception.UnauthorizedException;
import me.nald.blog.response.ResponseCode;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import static me.nald.blog.util.Constants.AUTHORITY;
import static me.nald.blog.util.Constants.USER_ID;

@Slf4j
@Component
@AllArgsConstructor
public class SecurityUtils {

  private static BlogProperties blogProperties;

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


  /**
   * private key로 복호화
   **/
  public static String decrypt(String encryptedPassword) throws Exception {

    PrivateKey privateKey = getPrivateKeyFromString(blogProperties.getPrivateKey());

    String[] chunks = encryptedPassword.split(":");
    /* original password */
    StringBuilder rowPasswordData = new StringBuilder();
    Arrays.stream(chunks)
            .map(chunk -> decryptRSA(chunk, privateKey))
            .forEach(rowPasswordData::append);

    return hashPassword(rowPasswordData.toString());
  }

  /**
   * public key로 암호화
   **/
  public static byte[] encryptRSA(byte[] data, PublicKey publicKey) throws Exception {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
    return cipher.doFinal(data);
  }


  public static String decryptRSA(String encryptedText, PrivateKey privateKey) {
    try {
      Cipher cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.DECRYPT_MODE, privateKey);
      byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
      return new String(decryptedBytes);
    } catch (GeneralSecurityException e) {
      throw new InternalServerErrorException(log, ResponseCode.UNKNOWN_ERROR);
    }
  }

  private static PrivateKey getPrivateKeyFromString(String key) throws Exception {
    String privateKeyPEM = key
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

    byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
    return keyFactory.generatePrivate(keySpec);
  }

  public static byte[] encryptAES(byte[] data, SecretKey secretKey) throws Exception {
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, secretKey);
    return cipher.doFinal(data);
  }

  public static byte[] decryptAES(byte[] encryptedData, SecretKey secretKey) throws Exception {
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.DECRYPT_MODE, secretKey);
    return cipher.doFinal(encryptedData);
  }

  public static SecretKey generateAESKey() throws Exception {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    return keyGen.generateKey();
  }

  public static PublicKey getPublicKeyFromString(String key) throws Exception {
    String publicKeyPEM = key
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

    byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
    return keyFactory.generatePublic(keySpec);
  }

  public static String hashPassword(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(encodedHash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private static String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

}
