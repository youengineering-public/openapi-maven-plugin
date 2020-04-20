package com.github.carlkuesters.swagger.mavenplugin.swagger.generator;

import com.github.carlkuesters.swagger.mavenplugin.GenerateException;
import com.github.carlkuesters.swagger.mavenplugin.config.ContentConfig;
import com.github.carlkuesters.swagger.mavenplugin.swagger.reader.AbstractReader;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;

public class SwaggerGenerator {

    private ContentConfig contentConfig;

    public SwaggerGenerator(ContentConfig contentConfig) {
        this.contentConfig = contentConfig;
    }

    public Swagger generateSwagger(AbstractReader reader) throws GenerateException {
        Swagger swagger = new Swagger();
        swagger.setInfo(contentConfig.getInfo());
        if (contentConfig.getSchemes() != null) {
            for (String scheme : contentConfig.getSchemes()) {
                swagger.scheme(Scheme.forValue(scheme));
            }
        }
        swagger.setHost(contentConfig.getHost());
        swagger.setBasePath(contentConfig.getBasePath());
        swagger.setExternalDocs(contentConfig.getExternalDocs());
        if (contentConfig.getSecurityDefinitionsPath() != null) {
            swagger.setSecurityDefinitions(SecurityDefinitionsGenerator.generateFromFile(contentConfig.getSecurityDefinitionsPath()));
        }

        reader.setOperationIdFormat(contentConfig.getOperationIdFormat());
        reader.enrich(swagger);

        SwaggerUtil.sort(swagger);
        return swagger;
    }
}
