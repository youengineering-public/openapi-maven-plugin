package com.github.carlkuesters.swagger;

import com.github.carlkuesters.swagger.config.Framework;

public class JaxrsTest extends FrameworkTest {

    public JaxrsTest() {
        super(Framework.jaxrs);
    }

    public void testGenerateFiles() throws Exception {
        // Given
        GenerateMojo mojo = createGenerateMojo();

        // When
        mojo.execute();

        // Then
        assetGeneratedFile_Json();
        assetGeneratedFile_Yaml();
    }
}
