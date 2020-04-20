package com.github.carlkuesters.swagger.mavenplugin.config;

import io.swagger.models.ExternalDocs;
import io.swagger.models.Info;
import lombok.Getter;

import java.util.List;

@Getter
public class ContentConfig {

    private Info info;
    private List<String> schemes;
    private String host;
    private String basePath;
    private String operationIdFormat;
    private String securityDefinitionsPath;
    private ExternalDocs externalDocs;

    private List<String> modelConverters;
    private String modelSubstitute;

}
