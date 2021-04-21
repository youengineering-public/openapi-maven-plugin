package com.youengineering.openapi.samples.spring;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Defines an interesting object.")
public class TestNestedObject1 {

    @Schema(description = "The name of a relation.", example = "self", allowableValues = {"prev", "self", "next"}, required = true)
    public String rel;

    @Schema(description = "The link to a resource.", example = "https://mydomain.com/my-resource?page=0&size=20", required = true)
    public String href;
}