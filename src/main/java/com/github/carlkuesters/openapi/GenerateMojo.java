package com.github.carlkuesters.openapi;

import com.github.carlkuesters.openapi.document.generator.OpenAPIPreparator;
import com.github.carlkuesters.openapi.document.reader.jaxrs.JaxrsReader;
import com.github.carlkuesters.openapi.document.reader.spring.SpringReader;
import com.github.carlkuesters.openapi.config.Framework;
import com.github.carlkuesters.openapi.reflection.AnnotatedClassService;
import com.github.carlkuesters.openapi.document.export.OpenAPIExporter;
import com.github.carlkuesters.openapi.document.generator.OpenAPIGenerator;
import com.github.carlkuesters.openapi.document.reader.AbstractReader;
import com.github.carlkuesters.openapi.config.ContentConfig;
import com.github.carlkuesters.openapi.config.OutputConfig;
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
