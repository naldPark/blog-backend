package me.nald.blog.response;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ResponseCode {
  SUCCESS(0, "Success", ""),

  // HTTP_CODE 400 - Invalid Parameter
  INVALID_PARAMETER(1000, "Invalid Parameter", "error_bad_request_001"),
  MISSING_PARAMETER(1001, "Missing Parameter", "error_bad_request_002"),
  MALFORMED_JSON(1002, "Malformed JSON Request", "error_bad_request_003"),
  INVALID_ID_PARAMETER(1003, "Invalid Id Parameter", "error_bad_request_004"), // 이미 사용중인 id입니다.
  PASSWORD_NOT_MATCH(1004, "Password Not Match", "error_bad_request_005"), // 비밀번호가 일치하지 않습니다.
  BAD_TOKEN(1005, "Bad Token", "error_bad_request_006"), // 토큰이 잘못 됨.


  // HTTP_CODE 401 - Unauthorized
  NO_AUTH_TOKEN(2001, "No Auth Token", "error_error_unauthorized_002_001"),
  INVALID_AUTH_TOKEN(2002, "Invalid Auth Token", "error_unauthorized_002"),
  EXPIRED_AUTH_TOKEN(2003, "Expired Auth Token", "error_error_unauthorized_003"),
  UNKNOWN_AUTH_ERROR(2000, "Unknown Auth Error", "error_error_unauthorized_004"),

  // HTTP_CODE 403 - Forbidden
  NOT_ALLOWED(3000, "Not Allowed", "error_forbidden_001"),
  ACCESS_DENIED(3001, "Access Denied", "error_forbidden_002"),
  USER_BLOCKED(3002, "Your id has been blocked", "error_forbidden_003"),
  USER_INACTIVE(3003, "The account is not able to login", "error_forbidden_004"),

  // HTTP_CODE 404 - Not Found
  NOT_FOUND(4000, "Not Found", "error_not_found_001"),
  RESOURCE_NOT_FOUND(4001, "Resource Not Found", "error_not_found_002"),
  USER_NOT_FOUND(4002, "User Not Found", "error_not_found_003"), // 사용자를 찾을 수 없습니다.

  // HTTP_CODE 405 - Method Not Allowed
  METHOD_NOT_ALLOWED(5000, "Method Not Allowed", "error_not_allowed_001"),
  UNSUPPORTED_MEDIA_TYPE(5001, "Unsupported Media Type", "error_not_allowed_002"),
  FILE_ALREADY_EXISTS(5002, "File Already Exists", "error_not_allowed_003"),
  ID_DUPLICATE(5003, "Id duplicate", "error_not_allowed_004"),
  TOO_MANY_MAIL_SEND_REQUESTS(5004, "only 5 times available per day", "error_not_allowed_005"),

  // HTTP_CODE 500 - Internal Server Error
  UNKNOWN_ERROR(-1, "Unknown Error", "error_internal_server_001"), // 알 수 없는 오류

  // HTTP_CODE 502 - Bad Gateway
  BAD_GATEWAY(7000, "Bad Gateway", "error_bad_gateway_001"),

  // HTTP_CODE 504 - Gateway Timeout
  GATEWAY_TIMEOUT(8000, "Gateway Timeout", "error_timeout_001"),

  // 그외...
  VALIDATION_ERROR(9000, "Validation Error", "error_etc_001");


  private final int code;
  private String message;
  private final String i18nCode;

  private static final Map<Integer, ResponseCode> findByProfile =
          Collections.unmodifiableMap(Stream.of(values())
                  .collect(Collectors.toMap(ResponseCode::getCode, Function.identity())));

  ResponseCode(int code, String message, String i18nCode) {
    this.code = code;
    this.message = message;
    this.i18nCode = i18nCode;
  }


  public int getCode() {
    return this.code;
  }

  public String getMessage() {
    return this.message;
  }

  public String getI18nCode() {
    return this.i18nCode;
  }

  public static ResponseCode valueOf(int code) {
    return Optional.ofNullable(findByProfile.get(code)).orElse(UNKNOWN_ERROR);
  }

  @Override
  public String toString() {
    return this.message;
  }
}
