package com.github.carlkuesters.swagger.swagger.generator;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;

public class SwaggerPreparator {

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
