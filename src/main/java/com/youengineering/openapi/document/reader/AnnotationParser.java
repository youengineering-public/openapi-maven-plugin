package com.youengineering.openapi.document.reader;

import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.links.LinkParameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationParser {

    static ApiResponses parseApiResponses(io.swagger.v3.oas.annotations.responses.ApiResponse[] apiResponseAnnotations) {
        ApiResponses apiResponses = new ApiResponses();
        for (io.swagger.v3.oas.annotations.responses.ApiResponse apiResponseAnnotation : apiResponseAnnotations) {
            ApiResponse apiResponse = new ApiResponse();
            apiResponse.setDescription(apiResponseAnnotation.description());
            apiResponse.setHeaders(parseHeaders(apiResponseAnnotation.headers()));
            apiResponse.setContent(parseContent(apiResponseAnnotation.content()));
            apiResponse.setLinks(parseLinks(apiResponseAnnotation.links()));
            apiResponse.setExtensions(parseExtensions(apiResponseAnnotation.extensions()));
            apiResponse.set$ref(apiResponseAnnotation.ref());
            apiResponses.put(apiResponseAnnotation.responseCode(), apiResponse);
        }
        return apiResponses;
    }

    static Map<String, Header> parseHeaders(io.swagger.v3.oas.annotations.headers.Header[] headerAnnotations) {
        Map<String, Header> headers = new HashMap<>();
        for (io.swagger.v3.oas.annotations.headers.Header headerAnnotation : headerAnnotations) {
            Header header = parseHeader(headerAnnotation);
            headers.put(headerAnnotation.name(), header);
        }
        return headers;
    }

    static Header parseHeader(io.swagger.v3.oas.annotations.headers.Header headerAnnotation) {
        Header header = new Header();
        header.setDescription(headerAnnotation.description());
        header.set$ref(headerAnnotation.ref());
        header.setRequired(headerAnnotation.required());
        header.setDeprecated(headerAnnotation.deprecated());
        return header;
    }

    static Content parseContent(io.swagger.v3.oas.annotations.media.Content[] contentAnnotations) {
        Content content = new Content();
        for (io.swagger.v3.oas.annotations.media.Content contentAnnotation : contentAnnotations) {
            MediaType mediaType = parseMediaType(contentAnnotation);
            content.put(contentAnnotation.mediaType(), mediaType);
        }
        return content;
    }

    static MediaType parseMediaType(io.swagger.v3.oas.annotations.media.Content contentAnnotation) {
        MediaType mediaType = new MediaType();
        mediaType.setSchema(parseSchema(contentAnnotation.schema()));
        mediaType.setExamples(parseExamples(contentAnnotation.examples()));
        mediaType.setEncoding(parseEncodings(contentAnnotation.encoding()));
        mediaType.setExtensions(parseExtensions(contentAnnotation.extensions()));
        return mediaType;
    }

    static Schema parseSchema(io.swagger.v3.oas.annotations.media.Schema schemaAnnotation) {
        Schema schema = new Schema();
        schema.setName(schemaAnnotation.name());
        // TODO
        return schema;
    }

    static Map<String, Example> parseExamples(ExampleObject[] exampleObjectAnnotations) {
        Map<String, Example> examples = new HashMap<>();
        for (ExampleObject exampleObjectAnnotation : exampleObjectAnnotations) {
            Example example = parseExample(exampleObjectAnnotation);
            examples.put(exampleObjectAnnotation.name(), example);
        }
        return examples;
    }

    static Example parseExample(ExampleObject exampleObjectAnnotation) {
        Example example = new Example();
        example.setSummary(exampleObjectAnnotation.summary());
        example.setDescription(exampleObjectAnnotation.description());
        example.setValue(exampleObjectAnnotation.value());
        example.setExternalValue(exampleObjectAnnotation.externalValue());
        example.set$ref(exampleObjectAnnotation.ref());
        example.setExtensions(parseExtensions(exampleObjectAnnotation.extensions()));
        return example;
    }

    static Map<String, Encoding> parseEncodings(io.swagger.v3.oas.annotations.media.Encoding[] encodingAnnotations) {
        Map<String, Encoding> encodings = new HashMap<>();
        for (io.swagger.v3.oas.annotations.media.Encoding encodingAnnotation : encodingAnnotations) {
            Encoding encoding = parseEncoding(encodingAnnotation);
            encodings.put(encodingAnnotation.name(), encoding);
        }
        return encodings;
    }

    static Encoding parseEncoding(io.swagger.v3.oas.annotations.media.Encoding encodingAnnotation) {
        Encoding encoding = new Encoding();
        encoding.setContentType(encodingAnnotation.contentType());
        encoding.setHeaders(parseHeaders(encodingAnnotation.headers()));
        // TODO: Map encoding.setStyle(encodingAnnotation.style());
        encoding.setExplode(encodingAnnotation.explode());
        encoding.setAllowReserved(encodingAnnotation.allowReserved());
        encoding.setExtensions(parseExtensions(encodingAnnotation.extensions()));
        return encoding;
    }

    static Map<String, Link> parseLinks(io.swagger.v3.oas.annotations.links.Link[] linkAnnotations) {
        Map<String, Link> links = new HashMap<>();
        for (io.swagger.v3.oas.annotations.links.Link linkAnnotation : linkAnnotations) {
            Link link = parseLink(linkAnnotation);
            links.put(linkAnnotation.name(), link);
        }
        return links;
    }

    static Link parseLink(io.swagger.v3.oas.annotations.links.Link linkAnnotation) {
        Link link = new Link();
        link.operationRef(linkAnnotation.operationRef());
        link.operationId(linkAnnotation.operationId());
        link.setParameters(parseLinkParameters(linkAnnotation.parameters()));
        link.setRequestBody(linkAnnotation.requestBody()); // TODO: Check
        link.setDescription(linkAnnotation.description());
        link.set$ref(linkAnnotation.ref());
        link.setExtensions(parseExtensions(linkAnnotation.extensions()));
        link.setServer(parseServer(linkAnnotation.server()));
        return link;
    }

    static Map<String, String> parseLinkParameters(LinkParameter[] linkParameterAnnotations) {
        Map<String, String> linkParameters = new HashMap<>();
        for (LinkParameter linkParameterAnnotation : linkParameterAnnotations) {
            linkParameters.put(linkParameterAnnotation.name(), linkParameterAnnotation.expression());
        }
        return linkParameters;
    }

    static Server parseServer(io.swagger.v3.oas.annotations.servers.Server serverAnnotation) {
        Server server = new Server();
        server.setUrl(serverAnnotation.url());
        server.setDescription(serverAnnotation.description());
        server.setVariables(parseServerVariables(serverAnnotation.variables()));
        server.setExtensions(parseExtensions(serverAnnotation.extensions()));
        return server;
    }

    static ServerVariables parseServerVariables(io.swagger.v3.oas.annotations.servers.ServerVariable[] serverVariableAnnotations) {
        ServerVariables serverVariables = new ServerVariables();
        for (io.swagger.v3.oas.annotations.servers.ServerVariable serverVariableAnnotation : serverVariableAnnotations) {
            ServerVariable serverVariable = parseServerVariable(serverVariableAnnotation);
            serverVariables.put(serverVariableAnnotation.name(), serverVariable);
        }
        return serverVariables;
    }

    static ServerVariable parseServerVariable(io.swagger.v3.oas.annotations.servers.ServerVariable serverVariableAnnotation) {
        ServerVariable serverVariable = new ServerVariable();
        serverVariable.setDescription(serverVariableAnnotation.description());
        serverVariable.setExtensions(parseExtensions(serverVariableAnnotation.extensions()));
        return serverVariable;
    }

    static Tag parseTag(io.swagger.v3.oas.annotations.tags.Tag tagAnnotation) {
        Tag tag = new Tag();
        tag.setName(tagAnnotation.name());
        tag.setDescription(tagAnnotation.description());
        tag.setDescription(tagAnnotation.description());
        tag.setExternalDocs(parseExternalDocumentation(tagAnnotation.externalDocs()));
        tag.setExtensions(parseExtensions(tagAnnotation.extensions()));
        return tag;
    }

    static ExternalDocumentation parseExternalDocumentation(io.swagger.v3.oas.annotations.ExternalDocumentation externalDocumentationAnnotation) {
        ExternalDocumentation externalDocumentation = new ExternalDocumentation();
        externalDocumentation.setDescription(externalDocumentationAnnotation.description());
        externalDocumentation.setUrl(externalDocumentationAnnotation.url());
        externalDocumentation.setExtensions(parseExtensions(externalDocumentationAnnotation.extensions()));
        return externalDocumentation;
    }

    static SecurityRequirement parseSecurityRequirement(io.swagger.v3.oas.annotations.security.SecurityRequirement securityRequirementAnnotation) {
        SecurityRequirement securityRequirement = new SecurityRequirement();
        String name = securityRequirementAnnotation.name();
        List<String> scopes = Arrays.asList(securityRequirementAnnotation.scopes());
        securityRequirement.addList(name, scopes);
        return securityRequirement;
    }

    static Map<String, Object> parseExtensions(Extension[] extensionAnnotations) {
        Map<String, Object> extensions = new HashMap<>();
        for (Extension extensionAnnotation : extensionAnnotations) {
            Map<String, String> extensionProperties = new HashMap<>();
            for (ExtensionProperty extensionPropertyAnnotation : extensionAnnotation.properties()) {
                extensionProperties.put(extensionPropertyAnnotation.name(), extensionPropertyAnnotation.value());
            }
            extensions.put(extensionAnnotation.name(), extensionProperties);
        }
        return extensions;
    }
}
