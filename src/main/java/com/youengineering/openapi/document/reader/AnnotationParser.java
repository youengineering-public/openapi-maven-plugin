package com.youengineering.openapi.document.reader;

import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.links.LinkParameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.*;
import java.util.function.Function;

class AnnotationParser {

    static Operation parseOperation(io.swagger.v3.oas.annotations.Operation operationAnnotation) {
        if (operationAnnotation.hidden()) {
            return null;
        }
        Operation operation = new Operation();
        operation.setTags(parseTags(operationAnnotation.tags()));
        operation.setSummary(parseEmptyableText(operationAnnotation.summary()));
        operation.setDescription(parseEmptyableText(operationAnnotation.description()));
        operation.setRequestBody(parseRequestBody(operationAnnotation.requestBody()));
        operation.setExternalDocs(parseExternalDocumentation(operationAnnotation.externalDocs()));
        operation.setOperationId(parseEmptyableText(operationAnnotation.operationId()));
        operation.setParameters(parseParameters(operationAnnotation.parameters()));
        operation.setResponses(parseApiResponses(operationAnnotation.responses()));
        operation.setDeprecated(operationAnnotation.deprecated());
        operation.setSecurity(parseSecurityRequirements(operationAnnotation.security()));
        operation.setServers(parseServers(operationAnnotation.servers()));
        operation.setExtensions(parseExtensions(operationAnnotation.extensions()));
        // TODO: Read and use ignoreJsonView
        return operation;
    }

    private static RequestBody parseRequestBody(io.swagger.v3.oas.annotations.parameters.RequestBody requestBodyAnnotation) {
        if (requestBodyAnnotation.content().length > 0) {
            RequestBody requestBody = new RequestBody();
            requestBody.setDescription(parseEmptyableText(requestBodyAnnotation.description()));
            requestBody.setContent(parseContent(requestBodyAnnotation.content()));
            requestBody.setRequired(requestBodyAnnotation.required());
            requestBody.setExtensions(parseExtensions(requestBodyAnnotation.extensions()));
            requestBody.set$ref(parseEmptyableText(requestBodyAnnotation.ref()));
            return requestBody;
        }
        return null;
    }

    private static List<Parameter> parseParameters(io.swagger.v3.oas.annotations.Parameter[] parameterAnnotations) {
        return parseList(parameterAnnotations, AnnotationParser::parseParameter);
    }

    private static Parameter parseParameter(io.swagger.v3.oas.annotations.Parameter parameterAnnotation) {
        if (parameterAnnotation.hidden()) {
            return null;
        }
        Parameter parameter = new Parameter();
        parameter.setName(parseEmptyableText(parameterAnnotation.name()));
        parameter.setIn(parameterAnnotation.in().toString());
        parameter.setDescription(parseEmptyableText(parameterAnnotation.description()));
        parameter.setRequired(parameterAnnotation.required());
        parameter.setDeprecated(parameterAnnotation.deprecated());
        parameter.setAllowEmptyValue(parameterAnnotation.allowEmptyValue());
        parameter.setExamples(parseExamples(parameterAnnotation.examples()));
        parameter.setExtensions(parseExtensions(parameterAnnotation.extensions()));
        Content content = parseContent(parameterAnnotation.content());
        if (content != null) {
            parameter.setContent(content);
        } else {
            Schema<?> schema = parseSchema(parameterAnnotation.schema(), parameterAnnotation.array());
            parameter.setSchema(schema);
            // "Ignored if the properties content or array are specified."
            if (!(schema instanceof ArraySchema)) {
                parameter.setStyle(parseParameterStyle(parameterAnnotation.style()));
                parameter.setExplode(parseParameterExplode(parameterAnnotation.explode()));
                parameter.setAllowReserved(parameterAnnotation.allowReserved());
                parameter.setExample(parseEmptyableText(parameterAnnotation.example()));
            }
        }
        return parameter;
    }

    private static Parameter.StyleEnum parseParameterStyle(ParameterStyle parameterStyle) {
        if (parameterStyle == ParameterStyle.DEFAULT) {
            return null;
        }
        return Parameter.StyleEnum.valueOf(parameterStyle.name());
    }

    private static Boolean parseParameterExplode(Explode explode) {
        switch (explode) {
            case FALSE:
                return Boolean.FALSE;
            case TRUE:
                return Boolean.TRUE;
            case DEFAULT:
            default:
                return null;
        }
    }

    static ApiResponses parseApiResponses(io.swagger.v3.oas.annotations.responses.ApiResponse[] apiResponseAnnotations) {
        ApiResponses apiResponses = new ApiResponses();
        for (io.swagger.v3.oas.annotations.responses.ApiResponse apiResponseAnnotation : apiResponseAnnotations) {
            ApiResponse apiResponse = new ApiResponse();
            // Description is mandatory here -> Don't map empty to null
            apiResponse.setDescription(apiResponseAnnotation.description());
            apiResponse.setHeaders(parseHeaders(apiResponseAnnotation.headers()));
            apiResponse.setContent(parseContent(apiResponseAnnotation.content()));
            apiResponse.setLinks(parseLinks(apiResponseAnnotation.links()));
            apiResponse.setExtensions(parseExtensions(apiResponseAnnotation.extensions()));
            apiResponse.set$ref(parseEmptyableText(apiResponseAnnotation.ref()));
            apiResponses.put(apiResponseAnnotation.responseCode(), apiResponse);
        }
        return apiResponses;
    }

    private static Map<String, Header> parseHeaders(io.swagger.v3.oas.annotations.headers.Header[] headerAnnotations) {
        Map<String, Header> headers = null;
        if (headerAnnotations.length > 0) {
            headers = new HashMap<>();
            for (io.swagger.v3.oas.annotations.headers.Header headerAnnotation : headerAnnotations) {
                Header header = parseHeader(headerAnnotation);
                headers.put(headerAnnotation.name(), header);
            }
        }
        return headers;
    }

    private static Header parseHeader(io.swagger.v3.oas.annotations.headers.Header headerAnnotation) {
        Header header = new Header();
        header.setDescription(parseEmptyableText(headerAnnotation.description()));
        header.setSchema(parseSchema(headerAnnotation.schema()));
        header.setRequired(headerAnnotation.required());
        header.setDeprecated(headerAnnotation.deprecated());
        header.set$ref(parseEmptyableText(headerAnnotation.ref()));
        return header;
    }

    private static Content parseContent(io.swagger.v3.oas.annotations.media.Content[] contentAnnotations) {
        if (contentAnnotations.length > 0) {
            Content content = new Content();
            for (io.swagger.v3.oas.annotations.media.Content contentAnnotation : contentAnnotations) {
                String mediaTypeKey = contentAnnotation.mediaType();
                if (mediaTypeKey.isEmpty()) {
                    mediaTypeKey = "*/*";
                }
                MediaType mediaType = parseMediaType(contentAnnotation);
                content.put(mediaTypeKey, mediaType);
            }
            return content;
        }
        return null;
    }

    private static MediaType parseMediaType(io.swagger.v3.oas.annotations.media.Content contentAnnotation) {
        MediaType mediaType = new MediaType();
        mediaType.setSchema(parseSchema(contentAnnotation.schema(), contentAnnotation.array()));
        mediaType.setExamples(parseExamples(contentAnnotation.examples()));
        mediaType.setEncoding(parseEncodings(contentAnnotation.encoding()));
        mediaType.setExtensions(parseExtensions(contentAnnotation.extensions()));
        return mediaType;
    }

    private static Schema<?> parseSchema(io.swagger.v3.oas.annotations.media.Schema schemaAnnotation, io.swagger.v3.oas.annotations.media.ArraySchema arraySchemaAnnotation) {
        if (arraySchemaAnnotation.schema().implementation() != Void.class) {
            return parseArraySchema(arraySchemaAnnotation);
        } else {
            return parseSchema(schemaAnnotation);
        }
    }

    private static ArraySchema parseArraySchema(io.swagger.v3.oas.annotations.media.ArraySchema arraySchemaAnnotation) {
        ArraySchema arraySchema = new ArraySchema();
        arraySchema.setItems(parseSchema(arraySchemaAnnotation.schema()));
        arraySchema.setMinItems(arraySchema.getMinItems());
        arraySchema.setMaxItems(arraySchema.getMaxItems());
        arraySchema.setUniqueItems(arraySchema.getUniqueItems());
        arraySchema.setExtensions(parseExtensions(arraySchemaAnnotation.extensions()));
        readSchema(arraySchemaAnnotation.arraySchema(), arraySchema);
        return arraySchema;
    }

    private static Schema<?> parseSchema(io.swagger.v3.oas.annotations.media.Schema schemaAnnotation) {
        Schema<?> schema;
        Class<?> implementation = schemaAnnotation.implementation();
        if (implementation != Void.class) {
            ResolvedSchema resolvedSchema = TypeUtil.getResolvedSchema(implementation);
            schema = resolvedSchema.schema;
        } else {
            schema = new Schema<>();
        }
        readSchema(schemaAnnotation, schema);
        return schema;
    }

    private static void readSchema(io.swagger.v3.oas.annotations.media.Schema schemaAnnotation, Schema<?> targetSchema) {
        targetSchema.setName(parseEmptyableText(schemaAnnotation.name()));
        targetSchema.setDescription(parseEmptyableText(schemaAnnotation.description()));
        targetSchema.setExtensions(parseExtensions(schemaAnnotation.extensions()));
        // TODO: Map rest of properties
    }

    private static Map<String, Example> parseExamples(ExampleObject[] exampleObjectAnnotations) {
        Map<String, Example> examples = null;
        if (exampleObjectAnnotations.length > 0) {
            examples = new HashMap<>();
            for (ExampleObject exampleObjectAnnotation : exampleObjectAnnotations) {
                Example example = parseExample(exampleObjectAnnotation);
                examples.put(exampleObjectAnnotation.name(), example);
            }
        }
        return examples;
    }

    private static Example parseExample(ExampleObject exampleObjectAnnotation) {
        Example example = new Example();
        example.setSummary(parseEmptyableText(exampleObjectAnnotation.summary()));
        example.setDescription(parseEmptyableText(exampleObjectAnnotation.description()));
        example.setValue(parseEmptyableText(exampleObjectAnnotation.value()));
        example.setExternalValue(parseEmptyableText(exampleObjectAnnotation.externalValue()));
        example.set$ref(parseEmptyableText(exampleObjectAnnotation.ref()));
        example.setExtensions(parseExtensions(exampleObjectAnnotation.extensions()));
        return example;
    }

    private static Map<String, Encoding> parseEncodings(io.swagger.v3.oas.annotations.media.Encoding[] encodingAnnotations) {
        Map<String, Encoding> encodings = null;
        if (encodingAnnotations.length > 0) {
            encodings = new HashMap<>();
            for (io.swagger.v3.oas.annotations.media.Encoding encodingAnnotation : encodingAnnotations) {
                Encoding encoding = parseEncoding(encodingAnnotation);
                encodings.put(encodingAnnotation.name(), encoding);
            }
        }
        return encodings;
    }

    private static Encoding parseEncoding(io.swagger.v3.oas.annotations.media.Encoding encodingAnnotation) {
        Encoding encoding = new Encoding();
        encoding.setContentType(parseEmptyableText(encodingAnnotation.contentType()));
        encoding.setHeaders(parseHeaders(encodingAnnotation.headers()));
        // TODO: Map encoding.setStyle(encodingAnnotation.style());
        encoding.setExplode(encodingAnnotation.explode());
        encoding.setAllowReserved(encodingAnnotation.allowReserved());
        encoding.setExtensions(parseExtensions(encodingAnnotation.extensions()));
        return encoding;
    }

    private static Map<String, Link> parseLinks(io.swagger.v3.oas.annotations.links.Link[] linkAnnotations) {
        Map<String, Link> links = null;
        if (linkAnnotations.length > 0) {
            links = new HashMap<>();
            for (io.swagger.v3.oas.annotations.links.Link linkAnnotation : linkAnnotations) {
                Link link = parseLink(linkAnnotation);
                links.put(linkAnnotation.name(), link);
            }
        }
        return links;
    }

    private static Link parseLink(io.swagger.v3.oas.annotations.links.Link linkAnnotation) {
        Link link = new Link();
        link.operationRef(parseEmptyableText(linkAnnotation.operationRef()));
        link.operationId(parseEmptyableText(linkAnnotation.operationId()));
        link.setParameters(parseLinkParameters(linkAnnotation.parameters()));
        link.setRequestBody(linkAnnotation.requestBody()); // TODO: Check
        link.setDescription(parseEmptyableText(linkAnnotation.description()));
        link.set$ref(parseEmptyableText(linkAnnotation.ref()));
        link.setExtensions(parseExtensions(linkAnnotation.extensions()));
        link.setServer(parseServer(linkAnnotation.server()));
        return link;
    }

    private static Map<String, String> parseLinkParameters(LinkParameter[] linkParameterAnnotations) {
        Map<String, String> linkParameters = null;
        if (linkParameterAnnotations.length > 0) {
            linkParameters = new HashMap<>();
            for (LinkParameter linkParameterAnnotation : linkParameterAnnotations) {
                linkParameters.put(linkParameterAnnotation.name(), linkParameterAnnotation.expression());
            }
        }
        return linkParameters;
    }

    private static List<Server> parseServers(io.swagger.v3.oas.annotations.servers.Server[] serverAnnotations) {
        return parseList(serverAnnotations, AnnotationParser::parseServer);
    }

    private static Server parseServer(io.swagger.v3.oas.annotations.servers.Server serverAnnotation) {
        Server server = new Server();
        server.setUrl(parseEmptyableText(serverAnnotation.url()));
        server.setDescription(parseEmptyableText(serverAnnotation.description()));
        server.setVariables(parseServerVariables(serverAnnotation.variables()));
        server.setExtensions(parseExtensions(serverAnnotation.extensions()));
        return server;
    }

    private static ServerVariables parseServerVariables(io.swagger.v3.oas.annotations.servers.ServerVariable[] serverVariableAnnotations) {
        ServerVariables serverVariables = null;
        if (serverVariableAnnotations.length > 0) {
            serverVariables = new ServerVariables();
            for (io.swagger.v3.oas.annotations.servers.ServerVariable serverVariableAnnotation : serverVariableAnnotations) {
                ServerVariable serverVariable = parseServerVariable(serverVariableAnnotation);
                serverVariables.put(serverVariableAnnotation.name(), serverVariable);
            }
        }
        return serverVariables;
    }

    private static ServerVariable parseServerVariable(io.swagger.v3.oas.annotations.servers.ServerVariable serverVariableAnnotation) {
        ServerVariable serverVariable = new ServerVariable();
        serverVariable.setDescription(parseEmptyableText(serverVariableAnnotation.description()));
        serverVariable.setExtensions(parseExtensions(serverVariableAnnotation.extensions()));
        return serverVariable;
    }

    private static List<String> parseTags(String[] tags) {
        return parseList(tags, tag -> tag);
    }

    static Tag parseTag(io.swagger.v3.oas.annotations.tags.Tag tagAnnotation) {
        Tag tag = new Tag();
        tag.setName(parseEmptyableText(tagAnnotation.name()));
        tag.setDescription(parseEmptyableText(tagAnnotation.description()));
        tag.setExternalDocs(parseExternalDocumentation(tagAnnotation.externalDocs()));
        tag.setExtensions(parseExtensions(tagAnnotation.extensions()));
        return tag;
    }

    private static ExternalDocumentation parseExternalDocumentation(io.swagger.v3.oas.annotations.ExternalDocumentation externalDocumentationAnnotation) {
        if (externalDocumentationAnnotation.description().isEmpty()
         && externalDocumentationAnnotation.url().isEmpty()
         && externalDocumentationAnnotation.extensions().length == 0) {
            return null;
        }
        ExternalDocumentation externalDocumentation = new ExternalDocumentation();
        externalDocumentation.setDescription(parseEmptyableText(externalDocumentationAnnotation.description()));
        externalDocumentation.setUrl(parseEmptyableText(externalDocumentationAnnotation.url()));
        externalDocumentation.setExtensions(parseExtensions(externalDocumentationAnnotation.extensions()));
        return externalDocumentation;
    }

    private static List<SecurityRequirement> parseSecurityRequirements(io.swagger.v3.oas.annotations.security.SecurityRequirement[] securityRequirementAnnotations) {
        return parseList(securityRequirementAnnotations, AnnotationParser::parseSecurityRequirement);
    }

    static SecurityRequirement parseSecurityRequirement(io.swagger.v3.oas.annotations.security.SecurityRequirement securityRequirementAnnotation) {
        SecurityRequirement securityRequirement = new SecurityRequirement();
        String name = securityRequirementAnnotation.name();
        List<String> scopes = Arrays.asList(securityRequirementAnnotation.scopes());
        securityRequirement.addList(name, scopes);
        return securityRequirement;
    }

    private static Map<String, Object> parseExtensions(Extension[] extensionAnnotations) {
        Map<String, Object> extensions = null;
        if (extensionAnnotations.length > 0) {
            extensions = new HashMap<>();
            for (Extension extensionAnnotation : extensionAnnotations) {
                Map<String, String> extensionProperties = new HashMap<>();
                for (ExtensionProperty extensionPropertyAnnotation : extensionAnnotation.properties()) {
                    extensionProperties.put(extensionPropertyAnnotation.name(), extensionPropertyAnnotation.value());
                }
                extensions.put("x-" + extensionAnnotation.name(), extensionProperties);
            }
        }
        return extensions;
    }

    private static <A, B> List<B> parseList(A[] array, Function<A, B> mappingFunction) {
        if (array.length > 0) {
            ArrayList<B> list = new ArrayList<>(array.length);
            for (A entry : array) {
                list.add(mappingFunction.apply(entry));
            }
            return list;
        }
        return null;
    }

    private static String parseEmptyableText(String text) {
        return (text.isEmpty() ? null : text);
    }
}
