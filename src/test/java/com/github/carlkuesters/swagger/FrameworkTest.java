package com.github.carlkuesters.swagger;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.carlkuesters.swagger.config.Framework;
import com.github.carlkuesters.swagger.config.OutputFormat;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtensions;
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

class FrameworkTest extends AbstractMojoTestCase {

    FrameworkTest(Framework framework) {
        this.framework = framework;
    }
    private Framework framework;
    private List<OpenAPIExtension> previousExtensions;

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

    protected GenerateMojo createGenerateMojo() throws Exception {
        File pomFile = new File(getBasedir(), "src/test/resources/" + framework.name() + "/pom.xml");
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession( new DefaultRepositorySystemSession() );
        MavenProject project = lookup( ProjectBuilder.class ).build( pomFile, configuration ).getProject();
        return (GenerateMojo) this.lookupConfiguredMojo(project, "generate");
    }

    protected void assetGeneratedFile_Json() throws Exception {
        JsonNode actualNode = Json.mapper().readTree(getSwaggerFile("generated", OutputFormat.json));
        JsonNode expectedNode = Json.mapper().readTree(getSwaggerFile("expected", OutputFormat.json));
        assertEquals(expectedNode, actualNode);
    }

    protected void assetGeneratedFile_Yaml() throws Exception {
        String actualContent = FileUtils.readFileToString(getSwaggerFile("generated", OutputFormat.yaml));
        String expectedContent = FileUtils.readFileToString(getSwaggerFile("expected", OutputFormat.yaml));
        assertEquals(expectedContent, actualContent);
    }

    private File getSwaggerFile(String fileName, OutputFormat outputFormat) {
        return new File(getBasedir(), "src/test/resources/" + framework.name() + "/" + fileName + "." + outputFormat.name());
    }
}
