package com.youengineering.openapi.document.generator;

import com.youengineering.openapi.GenerateException;
import com.youengineering.openapi.config.ContentConfig;
import com.youengineering.openapi.document.reader.AbstractReader;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;

public class OpenAPIGenerator {

    private ContentConfig contentConfig;

    public OpenAPIGenerator(ContentConfig contentConfig) {
        this.contentConfig = contentConfig;
    }

    public OpenAPI generateOpenAPI(AbstractReader reader) throws GenerateException {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(contentConfig.getInfo());
        if (contentConfig.getServerUrl() != null) {
            openAPI.addServersItem(new Server().url(contentConfig.getServerUrl()));
        }
        openAPI.setExternalDocs(contentConfig.getExternalDocs());
        Components components = new Components();
        if (contentConfig.getSecurityDefinitionsPath() != null) {
            components.setSecuritySchemes(SecuritySchemesGenerator.generateFromFile(contentConfig.getSecurityDefinitionsPath()));
        }
        openAPI.setComponents(components);
        openAPI.setPaths(new Paths());
        reader.enrich(openAPI);
        return openAPI;
    }
}
