package com.youengineering.openapi;

import com.youengineering.openapi.document.generator.OpenAPIPreparator;
import com.youengineering.openapi.document.reader.jaxrs.JaxrsReader;
import com.youengineering.openapi.document.reader.spring.SpringReader;
import com.youengineering.openapi.config.Framework;
import com.youengineering.openapi.reflection.AnnotatedClassService;
import com.youengineering.openapi.document.export.OpenAPIExporter;
import com.youengineering.openapi.document.generator.OpenAPIGenerator;
import com.youengineering.openapi.document.reader.AbstractReader;
import com.youengineering.openapi.config.ContentConfig;
import com.youengineering.openapi.config.OutputConfig;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter
    private String sourcePackage;
    @Parameter
    private Framework framework;
    @Parameter
    private ContentConfig content;
    @Parameter
    private OutputConfig output;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            AnnotatedClassService annotatedClassService = new AnnotatedClassService();
            annotatedClassService.initialize(project, sourcePackage);

            OpenAPIPreparator.loadModelConverters_Custom(content.getModelConverters());

            OpenAPIGenerator openAPIGenerator = new OpenAPIGenerator(content);
            AbstractReader reader = getReader(annotatedClassService);
            OpenAPI openAPI = openAPIGenerator.generateOpenAPI(reader);

            OpenAPIExporter.write(openAPI, output);
        } catch (GenerateException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private AbstractReader getReader(AnnotatedClassService annotatedClassService) throws GenerateException {
        if (framework != null) {
            Log log = getLog();
            switch (framework) {
                case jaxrs: return new JaxrsReader(annotatedClassService, log, content);
                case spring: return new SpringReader(annotatedClassService, log, content);
            }
        }
        throw new GenerateException("Invalid framework: " + framework);
    }
}
