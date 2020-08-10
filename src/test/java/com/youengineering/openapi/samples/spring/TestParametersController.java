package com.youengineering.openapi.samples.spring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/parameters")
public class TestParametersController {

    @PostMapping("/annotationOperationParameter")
    @Operation(parameters = { @Parameter(in = ParameterIn.HEADER, name = "myName", description = "myDescription", schema = @Schema(implementation = String.class)) } )
    public void testAnnotationOperationParameter() {

    }
}
