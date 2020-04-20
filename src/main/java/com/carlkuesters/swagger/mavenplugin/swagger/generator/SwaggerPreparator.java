package com.carlkuesters.swagger.mavenplugin.swagger.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.carlkuesters.swagger.mavenplugin.GenerateException;
import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverters;
import io.swagger.util.Json;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class SwaggerPreparator {

    public static void loadModelConverters_SubstituteFile(String modelSubstitute) throws GenerateException {
        if (modelSubstitute != null) {
            ObjectMapper objectMapper = Json.mapper();
            ModelModifier modelModifier = new ModelModifier(objectMapper);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(SwaggerPreparator.class.getResourceAsStream(modelSubstitute)))) {
                String line = reader.readLine();
                while (line != null) {
                    String[] classes = line.split(":");
                    if (classes.length != 2) {
                        throw new GenerateException("Bad format of override model file, it should be ${actualClassName}:${expectClassName}");
                    }
                    modelModifier.addModelSubstitute(classes[0].trim(), classes[1].trim());
                    line = reader.readLine();
                }
            } catch (IOException ex) {
                throw new GenerateException(ex);
            }
            ModelConverters.getInstance().addConverter(modelModifier);
        }
    }

    public static void loadModelConverters_Custom(List<String> modelConverters) throws MojoExecutionException {
        if (modelConverters == null) {
            return;
        }
        for (String modelConverter : modelConverters) {
            try {
                final Class<?> modelConverterClass = Class.forName(modelConverter);
                if (ModelConverter.class.isAssignableFrom(modelConverterClass)) {
                    final ModelConverter modelConverterInstance = (ModelConverter) modelConverterClass.newInstance();
                    ModelConverters.getInstance().addConverter(modelConverterInstance);
                } else {
                    throw new MojoExecutionException(String.format("Class %s has to be a subclass of %s", modelConverterClass.getName(), ModelConverter.class));
                }
            } catch (ClassNotFoundException ex) {
                throw new MojoExecutionException(String.format("Could not find custom model converter %s", modelConverter), ex);
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new MojoExecutionException(String.format("Unable to instantiate custom model converter %s", modelConverter), ex);
            }
        }
    }
}
