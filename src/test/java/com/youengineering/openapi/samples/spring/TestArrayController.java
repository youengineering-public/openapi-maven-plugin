package com.youengineering.openapi.samples.spring;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(value = "/array")
public class TestArrayController {

    @GetMapping(value = "/primitiveArrayWithoutAnnotations")
    public int[] testPrimitiveArrayWithoutAnnotations() {
        return new int[0];
    }

    @GetMapping(value = "/boxedArrayWithoutAnnotations")
    public short[] testBoxedArrayWithoutAnnotations() {
        return new short[0];
    }

    @GetMapping(value = "/objectArrayWithoutAnnotations")
    public TestResponse[] testObjectArrayWithoutAnnotations() {
        return new TestResponse[0];
    }

    @GetMapping(value = "/boxedListWithoutAnnotations")
    public List<Float> testBoxedListWithoutAnnotations() {
        return new LinkedList<>();
    }

    @GetMapping(value = "/objectListWithoutAnnotations")
    public List<TestResponse> testObjectListWithoutAnnotations() {
        return new LinkedList<>();
    }

    @GetMapping(value = "/boxedSetWithoutAnnotations")
    public Set<Double> testBoxedSetWithoutAnnotations() {
        return new HashSet<>();
    }

    @GetMapping(value = "/objectSetWithoutAnnotations")
    public Set<TestResponse> testObjectSetWithoutAnnotations() {
        return new HashSet<>();
    }

    @ApiResponses(value = {@ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TestResponse.class))))})
    @GetMapping(value = "/listWithAnnotationApiResponses")
    public ResponseEntity<List<TestResponse>> testListWithAnnotationApiResponses() {
        return ResponseEntity.ok(new LinkedList<>());
    }
}
