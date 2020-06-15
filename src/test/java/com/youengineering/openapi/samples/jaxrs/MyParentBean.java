package com.youengineering.openapi.samples.jaxrs;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

import javax.ws.rs.HeaderParam;

@Getter
@Setter
public class MyParentBean {

    @Parameter(description = "Header from parent")
    @HeaderParam("myParentHeader")
    private String myParentheader;

}
