package me.nald.blog.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import static me.nald.blog.util.Constants.*;

@Slf4j
@Component
public class ErrorException {

    private static ErrorMessages errorMessages;


    private static class ExceptionContainer<E> {
        private Supplier<E> supplier;

        ExceptionContainer(Supplier<E> supplier) {
            this.supplier = supplier;
        }

        E createContents() {
            return supplier.get();
        }
    }


    @Getter
    public static class Exception extends RuntimeException {
        private final HttpStatus httpStatus;
        private final int code;
        private final String name;

        private Exception (HttpStatus httpStatus, int code, String name, String message) {
            super(message);
            this.httpStatus = httpStatus;
            this.code = code;
            this.name = name;
        }

        private Exception(ErrorSpec spec, String message) {
            this(spec.getHttpStatus(), spec.getCode(), spec.name(), message);
        }

    }

    public static Exception of (ErrorSpec spec, String... args) {
        ErrorMessages delegate = getErrorMessagesDelegate();
        ExceptionContainer<Exception> exceptionContainer = new ExceptionContainer<>(() -> new Exception(spec,
                Arrays.stream(args)
                        .reduce(delegate.getErrorMessage(spec), (m, a) -> m.replaceFirst("%s", a))
                        .replaceAll("%s", ""))
        );
        return exceptionContainer.createContents();
    }

    public static ErrorMessages getErrorMessagesDelegate() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        request.getHeader(REQ_HEADER_LANG);
        return errorMessages;
    }
}
