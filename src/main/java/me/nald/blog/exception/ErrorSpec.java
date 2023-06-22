package me.nald.blog.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static me.nald.blog.exception.ErrorSpec.Group.*;
import static org.springframework.http.HttpStatus.*;

@Getter
@AllArgsConstructor
public enum ErrorSpec {
    // AUTH
    AccessDeniedException(UNAUTHORIZED, AUTH.code(1)),
    PermissionDenied(FORBIDDEN, AUTH.code(2)),
    UserNotFound(NOT_FOUND, AUTH.code(3)),
    UserIdBlocked(FORBIDDEN, AUTH.code(4)),
    AuthCodeNotFound(NOT_FOUND, AUTH.code(5)),
    UserIdDeleted(FORBIDDEN, AUTH.code(6)),
    TokenNotFound(UNAUTHORIZED, AUTH.code(7)),
    GroupIdDeleted(FORBIDDEN, AUTH.code(8)),


    // PASSWORD
    PasswordIsExpired(MOVED_PERMANENTLY, PASSWORD.code(1)),
    PasswordExceedFailedCount(BAD_REQUEST, PASSWORD.code(2)),
    PasswordNeedTobeChanged(FOUND, PASSWORD.code(3)),
    PasswordIsNotMatched(BAD_REQUEST, PASSWORD.code(4)),
    PasswordIsLocked(METHOD_NOT_ALLOWED, PASSWORD.code(5)),
    InvalidPassword(BAD_REQUEST, INVALID.code(6)),
    DuplicatedId(BAD_REQUEST, INVALID.code(7)),

    InvalidParameterValue(BAD_REQUEST, INVALID.code(1)),
    AlreadyExists(BAD_REQUEST, INVALID.code(2));

    private final HttpStatus httpStatus;
    private final int code;

    @Getter
    public enum Group {
        AUTH(1),
        PASSWORD(2),
        INVALID(3),
        ETC(20);

        private final int code;

        Group(int code) {
            this.code = code * 1000000;
        }

        public int code(int value) {
            return code + value;
        }

    }
}
