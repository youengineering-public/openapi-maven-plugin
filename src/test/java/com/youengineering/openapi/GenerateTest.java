package com.youengineering.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.youengineering.openapi.config.Framework;
import com.youengineering.openapi.config.OutputFormat;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtensions;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
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

public class GenerateTest extends AbstractMojoTestCase {

    private List<OpenAPIExtension> previousExtensions;
    private OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        previousExtensions = new ArrayList<>(OpenAPIExtensions.getExtensions());
    }

    @Override
    protected void tearDown() throws Exception {
    	super.tearDown();
        OpenAPIExtensions.setExtensions(previousExtensions);
    }

    public void testJaxrs() throws Exception {
        testFramework(Framework.jaxrs);
    }

    public void testSpring() throws Exception {
        testFramework(Framework.spring);
    }

    private void testFramework(Framework framework) throws Exception {
        // Given
        GenerateMojo mojo = createGenerateMojo(framework);

        // When
        mojo.execute();

        // Then
        assetGeneratedFile_Json(framework);
        assetGeneratedFile_Yaml(framework);
    }

    private GenerateMojo createGenerateMojo(Framework framework) throws Exception {
        File pomFile = new File(getBasedir(), "src/test/resources/" + framework.name() + "/pom.xml");
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession( new DefaultRepositorySystemSession() );
        MavenProject project = lookup( ProjectBuilder.class ).build( pomFile, configuration ).getProject();
        return (GenerateMojo) this.lookupConfiguredMojo(project, "generate");
    }

    private void assetGeneratedFile_Json(Framework framework) throws Exception {
        JsonNode actualNode = Json.mapper().readTree(getSwaggerFile(framework, "generated", OutputFormat.json));
        JsonNode expectedNode = Json.mapper().readTree(getSwaggerFile(framework, "expected", OutputFormat.json));
        assertEquals(expectedNode, actualNode);
    }

    private void assetGeneratedFile_Yaml(Framework framework) {
        OpenAPI actualOpenAPI = openAPIV3Parser.read(getSwaggerFile(framework, "generated", OutputFormat.yaml).getPath());
        OpenAPI expectedOpenAPI = openAPIV3Parser.read(getSwaggerFile(framework, "expected", OutputFormat.yaml).getPath());
        assertEquals(expectedOpenAPI, actualOpenAPI);
    }

    private File getSwaggerFile(Framework framework, String fileName, OutputFormat outputFormat) {
        return new File(getBasedir(), "src/test/resources/" + framework.name() + "/" + fileName + "." + outputFormat.name());
    }
}
