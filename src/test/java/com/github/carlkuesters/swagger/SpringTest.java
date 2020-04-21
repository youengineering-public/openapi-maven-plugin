package com.github.carlkuesters.swagger;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.carlkuesters.swagger.config.Framework;
import com.github.carlkuesters.swagger.config.OutputFormat;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.jaxrs.ext.SwaggerExtensions;
import io.swagger.util.Json;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SpringTest extends AbstractMojoTestCase {

    private List<SwaggerExtension> previousExtensions;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        previousExtensions = new ArrayList<>(SwaggerExtensions.getExtensions());
    }

    @Override
    protected void tearDown() throws Exception {
    	super.tearDown();
    	SwaggerExtensions.setExtensions(previousExtensions);
    }

    public void testGenerateSwagger() throws Exception {
        // Given
        GenerateMojo mojo = createGenerateMojo(Framework.spring);

        // When
        mojo.execute();

        // Then
        assetGeneratedFile_Json();
        assetGeneratedFile_Yaml();
    }

    private GenerateMojo createGenerateMojo(Framework framework) throws Exception {
        File pomFile = new File(getBasedir(), "src/test/resources/" + framework.name() + "/pom.xml");
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession( new DefaultRepositorySystemSession() );
        MavenProject project = lookup( ProjectBuilder.class ).build( pomFile, configuration ).getProject();
        return (GenerateMojo) this.lookupConfiguredMojo(project, "generate");
    }

    private void assetGeneratedFile_Json() throws Exception {
        JsonNode actualNode = Json.mapper().readTree(getSwaggerFile("generated", OutputFormat.json));
        JsonNode expectedNode = Json.mapper().readTree(getSwaggerFile("expected", OutputFormat.json));
        assertEquals(expectedNode, actualNode);
    }

    private void assetGeneratedFile_Yaml() throws Exception {
        String actualContent = FileUtils.readFileToString(getSwaggerFile("generated", OutputFormat.yaml));
        String expectedContent = FileUtils.readFileToString(getSwaggerFile("expected", OutputFormat.yaml));
        assertEquals(expectedContent, actualContent);
    }

    private File getSwaggerFile(String fileName, OutputFormat outputFormat) {
        return new File(getBasedir(), "src/test/resources/spring/" + fileName + "." + outputFormat.name());
    }
}
