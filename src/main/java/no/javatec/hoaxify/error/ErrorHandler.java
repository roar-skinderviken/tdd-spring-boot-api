package no.javatec.hoaxify.error;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequiredArgsConstructor
public class ErrorHandler implements ErrorController {

    private static final String ERROR_PATH = "/error";
    private final ErrorAttributes errorAttributes;

    @RequestMapping(ERROR_PATH)
    ApiError handleError(WebRequest webRequest) {
        var attrs = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE));
        return new ApiError(
                (Integer) attrs.get("status"),
                (String) attrs.get("message"),
                (String) attrs.get("path"));
    }

    public String getErrorPath() {
        return ERROR_PATH;
    }
}
