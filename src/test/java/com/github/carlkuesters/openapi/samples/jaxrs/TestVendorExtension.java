package com.github.carlkuesters.openapi.samples.jaxrs;


import io.swagger.v3.jaxrs2.ext.AbstractOpenAPIExtension;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Iterator;

public class TestVendorExtension extends AbstractOpenAPIExtension {

    private static final String RESPONSE_STATUS_401 = "401";
    private static final String RESPONSE_DESCRIPTION = "Some vendor error description";

    @Override
    public void decorateOperation(Operation operation, Method method, Iterator<OpenAPIExtension> chain) {
        final TestVendorAnnotation annotation = method.getAnnotation(TestVendorAnnotation.class);
        if (annotation != null) {
            ApiResponses responses = operation.getResponses();
            ApiResponse response = new ApiResponse();
            response.setDescription(RESPONSE_DESCRIPTION);
            responses.put(RESPONSE_STATUS_401, response);
        }
        if (chain.hasNext()) {
            chain.next().decorateOperation(operation, method, chain);
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestVendorAnnotation {}
}
