package com.github.carlkuesters.swagger.mavenplugin.swagger.reader.jaxrs;

import com.github.carlkuesters.swagger.mavenplugin.swagger.reader.AbstractReader;
import io.swagger.jaxrs.ext.AbstractSwaggerExtension;
import io.swagger.models.Swagger;

public abstract class AbstractReaderSwaggerExtension extends AbstractSwaggerExtension {

    protected AbstractReader reader;
    protected Swagger swagger;

    public void setContext(AbstractReader reader, Swagger swagger) {
        this.reader = reader;
        this.swagger = swagger;
    }
}
