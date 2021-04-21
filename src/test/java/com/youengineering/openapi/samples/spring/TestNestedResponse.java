package com.youengineering.openapi.samples.spring;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Defines a paged slice of entries with nested data.")
public class TestNestedResponse {

    @ArraySchema(schema = @Schema(description = "The list of the entries with the nested data.", required = true))
    public List<TestNestedObject2> content;

    @Schema(description = "The number of the page/slice that was requested.", example = "1", required = true)
    public int page;

    @Schema(description = "The size of the page/slice that was requested.", example = "20", required = true)
    public int size;

    @Schema(description = "The number of results in this page/slice.", example = "5", required = true)
    public int numberResults;

    @ArraySchema(schema = @Schema( description = "The list of the links to the previous, self and next page/slice.", required = true))
    public List<TestNestedObject1> links;
}