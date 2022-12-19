package me.nald.blog.exception;

import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component("errorMessagesEn")
public class ErrorMessageEn implements ErrorMessages {

    @Override
    public String getErrorMessage(ErrorSpec spec) {
        return Arrays.asList(EN.values()).stream()
                .filter(en -> en.spec.equals(spec))
                .findAny()
                .map(en -> en.message)
                .orElse("");
    }

    private enum EN {

        InvalidPassword(ErrorSpec.InvalidPassword, "Invalid password"),
        PermissionDenied(ErrorSpec.PermissionDenied, "Permission Denied"),
        AccessDeniedException(ErrorSpec.AccessDeniedException, "You do not have sufficient access to perform this action. %s"),
        InvalidParameterValue(ErrorSpec.InvalidParameterValue, "An invalid or out-of-range value was supplied for the input parameter. %s");

        private ErrorSpec spec;
        private String message;

        EN(ErrorSpec spec, String message) {
            this.spec = spec;
            this.message = message;
        }
    }
}
