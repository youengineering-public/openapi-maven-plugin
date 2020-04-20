package com.github.carlkuesters.swagger.swagger.export;

import com.github.carlkuesters.swagger.GenerateException;
import com.github.carlkuesters.swagger.config.OutputConfig;
import com.github.carlkuesters.swagger.config.OutputFormat;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.models.Swagger;
import io.swagger.util.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SwaggerExporter {

    public static void write(Swagger swagger, OutputConfig outputConfig) throws GenerateException {
        createDirectoryIfNotExists(outputConfig.getDirectory());
        for (OutputFormat outputFormat : outputConfig.getFormats()) {
            try {
                String fileContent = null;
                switch (outputFormat) {
                    case yaml:
                        fileContent = Yaml.pretty().writeValueAsString(swagger);
                        break;
                    case json:
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
                        ObjectWriter jsonWriter = mapper.writer(new DefaultPrettyPrinter());
                        fileContent = jsonWriter.writeValueAsString(swagger);
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
