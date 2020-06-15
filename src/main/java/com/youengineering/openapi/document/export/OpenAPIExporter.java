package com.youengineering.openapi.document.export;

import com.youengineering.openapi.GenerateException;
import com.youengineering.openapi.config.OutputConfig;
import com.youengineering.openapi.config.OutputFormat;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OpenAPIExporter {

    public static void write(OpenAPI openAPI, OutputConfig outputConfig) throws GenerateException {
        createDirectoryIfNotExists(outputConfig.getDirectory());
        for (OutputFormat outputFormat : outputConfig.getFormats()) {
            try {
                String fileContent = null;
                switch (outputFormat) {
                    case yaml:
                        fileContent = Yaml.pretty().writeValueAsString(openAPI);
                        break;
                    case json:
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
                        ObjectWriter jsonWriter = mapper.writer(new DefaultPrettyPrinter());
                        fileContent = jsonWriter.writeValueAsString(openAPI);
                        break;
                }
                String filePath = outputConfig.getDirectory() + "/" + outputConfig.getFileName() + "." + outputFormat.name();
                Files.write(Paths.get(filePath), fileContent.getBytes());
            } catch (IOException ex) {
                throw new GenerateException(String.format("Writing file [%s] failed.", outputFormat), ex);
            }
        }
    }

    private static void createDirectoryIfNotExists(String directoryPath) throws GenerateException {
        File directory = new File(directoryPath);
        if (directory.isFile()) {
            throw new GenerateException(String.format("Output directory[%s] must be a directory!", directoryPath));
        }
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new GenerateException(String.format("Creating output directory[%s] failed.", directoryPath));
            }
        }
    }
}
