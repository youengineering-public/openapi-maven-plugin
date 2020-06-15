package com.youengineering.openapi.samples.jaxrs;

public class PetId {

    private final long id;

    public PetId(long id) {
        this.id = id;
    }

    public long value() {
        return id;
    }
}
