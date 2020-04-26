package com.github.carlkuesters.openapi.document.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.carlkuesters.openapi.GenerateException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class SecuritySchemesGenerator {

    static Map<String, SecurityScheme> generateFromFile(String jsonPath) throws GenerateException {
        Map<String, SecurityScheme> securitySchemeDefinitions = new HashMap<>();
        Map<String, JsonNode> definitionNodes = loadJsonFile(jsonPath);
        for (Map.Entry<String, JsonNode> entry : definitionNodes.entrySet()) {
            try {
                SecurityScheme securityScheme = Json.mapper().readValue(entry.getValue().toString(), SecurityScheme.class);
                securitySchemeDefinitions.put(entry.getKey(), securityScheme);
            } catch (IOException ex) {
                throw new GenerateException("Generating security definition of key " + entry.getKey() + " failed.", ex);
            }
        }
        return securitySchemeDefinitions;
    }

    private static Map<String, JsonNode> loadJsonFile(String jsonPath) throws GenerateException {
        Map<String, JsonNode> definitionNodes = new HashMap<>();
        try {
            JsonNode root = Json.mapper().readTree(new FileInputStream(jsonPath));
            Iterator<Map.Entry<String, JsonNode>> rootFields = root.fields();
            while (rootFields.hasNext()) {
                Map.Entry<String, JsonNode> field = rootFields.next();
                definitionNodes.put(field.getKey(), field.getValue());
            }
        } catch (IOException ex) {
            throw new GenerateException(ex);
        }
        return definitionNodes;
    }
}
