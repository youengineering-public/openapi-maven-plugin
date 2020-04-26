package com.github.carlkuesters.openapi.samples.jaxrs;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a nested {@code @BeanParam} target that is injected by constructor.
 * 
 * @see MyNestedBean
 */
@Getter
@Setter
public class MyConstructorInjectedNestedBean {

    /**
     * Note: This property will not be found by the reader, which seems to be a limitation of io.openapi.jaxrs itself.
     */
    private final String constructorInjectedHeader;
    
    // @Inject would typically go here in real life, telling e.g. Jersey to use constructor injection
    public MyConstructorInjectedNestedBean(
            @Parameter(description = "Header injected at constructor")
            @HeaderParam("constructorInjectedHeader")
            @DefaultValue("foo")
            String constructorInjectedHeader
    ) {
        this.constructorInjectedHeader = constructorInjectedHeader;
    }
}
