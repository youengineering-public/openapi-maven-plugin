package com.carlkuesters.swagger.mavenplugin;

import com.carlkuesters.swagger.mavenplugin.swagger.generator.SwaggerPreparator;
import com.carlkuesters.swagger.mavenplugin.swagger.reader.jaxrs.JaxrsReader;
import com.carlkuesters.swagger.mavenplugin.swagger.reader.spring.SpringReader;
import com.carlkuesters.swagger.mavenplugin.config.Framework;
import com.carlkuesters.swagger.mavenplugin.reflection.AnnotatedClassService;
import com.carlkuesters.swagger.mavenplugin.swagger.export.SwaggerExporter;
import com.carlkuesters.swagger.mavenplugin.swagger.generator.SwaggerGenerator;
import com.carlkuesters.swagger.mavenplugin.swagger.reader.AbstractReader;
import com.carlkuesters.swagger.mavenplugin.config.ContentConfig;
import com.carlkuesters.swagger.mavenplugin.config.OutputConfig;
import io.swagger.models.Swagger;
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

            SwaggerPreparator.loadModelConverters_SubstituteFile(content.getModelSubstitute());
            SwaggerPreparator.loadModelConverters_Custom(content.getModelConverters());

            SwaggerGenerator swaggerGenerator = new SwaggerGenerator(content);
            AbstractReader reader = getReader(annotatedClassService);
            Swagger swagger = swaggerGenerator.generateSwagger(reader);

            SwaggerExporter.write(swagger, output);
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
                case jaxrs: return new JaxrsReader(annotatedClassService, log);
                case spring: return new SpringReader(annotatedClassService, log);
            }
        }
        throw new GenerateException("Invalid framework: " + framework);
    }
}
