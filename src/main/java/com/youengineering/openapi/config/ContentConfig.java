package com.youengineering.openapi.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.info.Info;
import lombok.Getter;

import java.util.List;

@Getter
public class ContentConfig {

    private Info info;
    private String serverUrl;
    // Supported parameters: {{packageName}}, {{className}}, {{methodName}}, {{httpMethod}}
    private String operationIdFormat = "{{className}}_{{methodName}}_{{httpMethod}}";
    private String defaultSuccessfulOperationDescription = "Successful operation";
    private String securityDefinitionsPath;
    private ExternalDocumentation externalDocs;

    private List<String> modelConverters;

}
