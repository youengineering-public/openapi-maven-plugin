package com.youengineering.openapi.samples.jaxrs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PetName {

    private final String name;

    @JsonCreator
    public static PetName fromString(@JsonProperty("name") String name) {

        return new PetName(name);
    }

    public PetName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
