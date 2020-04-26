package com.github.carlkuesters.swagger.swagger.generator;

import com.github.carlkuesters.swagger.GenerateException;
import com.github.carlkuesters.swagger.config.ContentConfig;
import com.github.carlkuesters.swagger.swagger.reader.AbstractReader;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;

public class SwaggerGenerator {

    private ContentConfig contentConfig;

    public SwaggerGenerator(ContentConfig contentConfig) {
        this.contentConfig = contentConfig;
    }

    public OpenAPI generateSwagger(AbstractReader reader) throws GenerateException {
        OpenAPI swagger = new OpenAPI();
        swagger.setInfo(contentConfig.getInfo());
        swagger.addServersItem(new Server().url(contentConfig.getServerUrl()));
        swagger.setExternalDocs(contentConfig.getExternalDocs());
        Components components = new Components();
        if (contentConfig.getSecurityDefinitionsPath() != null) {
            components.setSecuritySchemes(SecuritySchemesGenerator.generateFromFile(contentConfig.getSecurityDefinitionsPath()));
        }
        swagger.setComponents(components);
        swagger.setPaths(new Paths());
        reader.enrich(swagger);
        return swagger;
    }
}
