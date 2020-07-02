package com.youengineering.openapi.samples.spring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/responses")
public class TestResponsesController {

    @GetMapping("/annotationOperation")
    @Operation(responses = {@ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TestResponse.class)))})
    public ResponseEntity<?> testAnnotationOperation() {
        return ResponseEntity.ok(new TestResponse("myText", new TestInnerResponse(42)));
    }

    @GetMapping("/annotationApiResponses")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TestResponse.class)))})
    public ResponseEntity<?> testAnnotationApiResponses() {
        return ResponseEntity.ok(new TestResponse("myText", new TestInnerResponse(42)));
    }
}
