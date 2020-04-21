package com.github.carlkuesters.swagger.swagger.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.carlkuesters.swagger.GenerateException;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.BasicAuthDefinition;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.util.Json;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class SecurityDefinitionsGenerator {

    private static SecuritySchemeDefinition[] REFERENCE_DEFINITIONS = {
        new OAuth2Definition(),
        new BasicAuthDefinition(),
        new ApiKeyAuthDefinition()
    };

    static Map<String, SecuritySchemeDefinition> generateFromFile(String jsonPath) throws GenerateException {
        Map<String, SecuritySchemeDefinition> securitySchemeDefinitions = new HashMap<>();
        Map<String, JsonNode> definitionNodes = loadJsonFile(jsonPath);
        for (Map.Entry<String, JsonNode> entry : definitionNodes.entrySet()) {
            SecuritySchemeDefinition securitySchemeDefinition = generateFromJsonNode(entry.getValue());
            if (securitySchemeDefinition != null) {
                securitySchemeDefinitions.put(entry.getKey(), securitySchemeDefinition);
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

    private static SecuritySchemeDefinition generateFromJsonNode(JsonNode node) throws GenerateException {
        String type = node.get("type").asText();
        try {
            for (SecuritySchemeDefinition referenceDefinition : REFERENCE_DEFINITIONS) {
                if (referenceDefinition.getType().equals(type)) {
                    return Json.mapper().readValue(node.traverse(), referenceDefinition.getClass());
                }
            }
            throw new GenerateException("Unknown security definition type: " + type);
        } catch (IOException ex) {
            throw new GenerateException("Generating security definition of type " + type + " failed.", ex);
        }
    }
}
