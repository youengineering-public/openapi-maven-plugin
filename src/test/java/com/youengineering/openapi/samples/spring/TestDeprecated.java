package com.youengineering.openapi.samples.spring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/deprecated")
public class TestDeprecated {

    @PostMapping("/operationAnnotationJava")
    @Deprecated
    public void testDeprecatedOperationAnnotationJava() {

    }

    @PostMapping("/operationAnnotationOpenAPI")
    @Deprecated
    @Operation(deprecated = true)
    public void testDeprecatedOperationAnnotationOpenAPI() {

    }

    @PostMapping("/parameterAnnotationJava")
    public String testDeprecatedParameterAnnotationJava(@Deprecated @RequestParam String text) {
        return text;
    }

    @PostMapping("/parameterAnnotationOpenAPI")
    public String testDeprecatedParameterAnnotation(@Parameter(deprecated = true) @RequestParam String text) {
        return text;
    }

    @PostMapping("/apiResponseHeaderAnnotation")
    @Operation(responses = {@ApiResponse(
            responseCode = "200",
            headers = @Header(name = "myHeader", deprecated = true, schema = @Schema(implementation = String.class)),
            content = @Content(schema = @Schema(implementation = String.class))
    )})
    public String testDeprecatedApiResponseHeaderAnnotation() {
        return null;
    }
}
