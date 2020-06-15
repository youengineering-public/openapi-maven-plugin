package com.youengineering.openapi.document.reader;

import io.swagger.v3.jaxrs2.ext.AbstractOpenAPIExtension;
import io.swagger.v3.oas.models.OpenAPI;

public abstract class AbstractReaderOpenAPIExtension extends AbstractOpenAPIExtension {

    protected AbstractReader reader;
    protected OpenAPI openAPI;

    public void setContext(AbstractReader reader, OpenAPI openAPI) {
        this.reader = reader;
        this.openAPI = openAPI;
    }
}
