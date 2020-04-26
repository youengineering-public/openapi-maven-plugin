package com.github.carlkuesters.swagger.swagger.reader;

import io.swagger.v3.jaxrs2.ext.AbstractOpenAPIExtension;
import io.swagger.v3.oas.models.OpenAPI;

public abstract class AbstractReaderSwaggerExtension extends AbstractOpenAPIExtension {

    protected AbstractReader reader;
    protected OpenAPI swagger;

    public void setContext(AbstractReader reader, OpenAPI swagger) {
        this.reader = reader;
        this.swagger = swagger;
    }
}
