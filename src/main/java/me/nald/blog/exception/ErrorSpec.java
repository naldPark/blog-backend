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
    PermissionDenied(UNAUTHORIZED, AUTH.code(2)),
    UserNotFound(NOT_FOUND, AUTH.code(3)),
    UserIdBlocked(FORBIDDEN, AUTH.code(4)),
    AuthCodeNotFound(NOT_FOUND, AUTH.code(5)),
    AuthTypeOfSsoNotFound(NOT_FOUND, AUTH.code(6)),
    UserIdDeleted(FORBIDDEN, AUTH.code(7)),
    TokenNotFound(UNAUTHORIZED, AUTH.code(8)),
    GroupIdDeleted(FORBIDDEN, AUTH.code(8)),

    // PASSWORD
    PasswordIsExpired(MOVED_PERMANENTLY, PASSWORD.code(1)),
    PasswordExceedFailedCount(UNAUTHORIZED, PASSWORD.code(2)),
    PasswordNeedTobeChanged(FOUND, PASSWORD.code(3)),
    PasswordIsNotMatched(UNAUTHORIZED, PASSWORD.code(4)),
    PasswordIsLocked(METHOD_NOT_ALLOWED, PASSWORD.code(5)),
    InvalidPassword(BAD_REQUEST, INVALID.code(6)),
    AccountIsLocked(UNAUTHORIZED, PASSWORD.code(7)),
    PasswordIsNotMatchedNoCount(UNAUTHORIZED, PASSWORD.code(8)),


    // INVALID
    InvalidParameterValue(BAD_REQUEST, INVALID.code(1)),
    InvalidFormatValue(BAD_REQUEST, INVALID.code(2)),
    InvalidAccountId(FORBIDDEN, INVALID.code(3)),
    InvalidDuplicatedAccountId(FORBIDDEN, INVALID.code(4)),
    InvalidGroupName(FORBIDDEN, INVALID.code(5)),
    InvalidEmail(BAD_REQUEST, INVALID.code(6)),
    InvalidDuplicateEmail(FORBIDDEN, INVALID.code(7)),
    InvalidDeleteGroup(FORBIDDEN, INVALID.code(8)),
    InvalidDeleteUserInGroup(FORBIDDEN, INVALID.code(9)),
    InvalidDecryptingError(BAD_REQUEST, INVALID.code(10)),	

    // ETC
    DuplicateNameFound(BAD_REQUEST, ETC.code(1)),
    ExecuteFailedError(BAD_REQUEST, ETC.code(2)),
    HaveAlreadyDeletedException(CONFLICT, ETC.code(3)),
    InternalFailure(INTERNAL_SERVER_ERROR, ETC.code(4)),
    NotFoundError(NOT_FOUND, ETC.code(5)),
    DuplicateColumnFound(BAD_REQUEST, ETC.code(6)),
    PipelineIntegrationError(INTERNAL_SERVER_ERROR, ETC.code(7));

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
