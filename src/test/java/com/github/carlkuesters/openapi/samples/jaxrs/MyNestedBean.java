package com.github.carlkuesters.openapi.samples.jaxrs;

import javax.ws.rs.HeaderParam;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a {@code @BeanParam} target that is nested within another bean.
 */
@Getter
@Setter
public class MyNestedBean {
    
    @Parameter(description = "Header from nested bean")
    @HeaderParam("myNestedBeanHeader")
    private String myNestedBeanHeader;

}
