package me.nald.blog.exception;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import static java.util.regex.Matcher.quoteReplacement;
import java.util.Arrays;
import java.util.function.Supplier;

@Slf4j
@Component
public class Errors {

    private static ErrorMessages errorMessages;

    @Autowired
    public void setErrorMessages(ErrorMessages  errorMessages) {
        this.errorMessages = errorMessages;
    }

    public static CommonException of (ErrorSpec spec, String... args) {
            String temp=    Arrays.stream(args)
                        .reduce(errorMessages.getErrorMessage(spec),
                                (m, a) -> m.replaceFirst("%s", quoteReplacement(a))).replaceAll("%s", "");

        ExceptionContainer<CommonException> exceptionContainer = new ExceptionContainer<>(() -> new CommonException(spec,temp));

        return exceptionContainer.createContents();
    }

    @Getter
    public static class CommonException extends RuntimeException {
        private final HttpStatus httpStatus;
        private final int code;
        private final String name;

        private CommonException (HttpStatus httpStatus, int code, String name, String message) {
            super(message);
            this.httpStatus = httpStatus;
            this.code = code;
            this.name = name;
        }

        private CommonException(ErrorSpec spec, String message) {
            this(spec.getHttpStatus(), spec.getCode(), spec.name(), message);
        }
    }

    private static class ExceptionContainer<E> {
        private Supplier<E> supplier;

        ExceptionContainer(Supplier<E> supplier) {
            this.supplier = supplier;
        }

        E createContents() {
            return supplier.get();
        }
    }

}
