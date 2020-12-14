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

    static AnnotationParserResult<Operation> parseOperation(io.swagger.v3.oas.annotations.Operation operationAnnotation) {
        if (operationAnnotation.hidden()) {
            return null;
        }
        Operation operation = new Operation();
        Map<String, Schema> referencedSchemas = new HashMap<>();
        operation.setTags(parseTags(operationAnnotation.tags()));
        operation.setSummary(parseEmptyableText(operationAnnotation.summary()));
        operation.setDescription(parseEmptyableText(operationAnnotation.description()));
        AnnotationParserResult<RequestBody> requestBodyResult = parseRequestBody(operationAnnotation.requestBody());
        operation.setRequestBody(requestBodyResult.getResult());
        referencedSchemas.putAll(requestBodyResult.getReferencedSchemas());
        operation.setExternalDocs(parseExternalDocumentation(operationAnnotation.externalDocs()));
        operation.setOperationId(parseEmptyableText(operationAnnotation.operationId()));
        AnnotationParserResult<List<Parameter>> parametersResult = parseParameters(operationAnnotation.parameters());
        operation.setParameters(parametersResult.getResult());
        referencedSchemas.putAll(parametersResult.getReferencedSchemas());
        AnnotationParserResult<ApiResponses> apiResponsesResult = parseApiResponses(operationAnnotation.responses());
        operation.setResponses(apiResponsesResult.getResult());
        referencedSchemas.putAll(apiResponsesResult.getReferencedSchemas());
        operation.setDeprecated(operationAnnotation.deprecated());
        operation.setSecurity(parseSecurityRequirements(operationAnnotation.security()));
        operation.setServers(parseServers(operationAnnotation.servers()));
        operation.setExtensions(parseExtensions(operationAnnotation.extensions()));
        // TODO: Read and use ignoreJsonView
        return new AnnotationParserResult<>(operation, referencedSchemas);
    }

    private static AnnotationParserResult<RequestBody> parseRequestBody(io.swagger.v3.oas.annotations.parameters.RequestBody requestBodyAnnotation) {
        RequestBody requestBody = null;
        Map<String, Schema> referencedSchemas = new HashMap<>();
        if (requestBodyAnnotation.content().length > 0) {
            requestBody = new RequestBody();
            requestBody.setDescription(parseEmptyableText(requestBodyAnnotation.description()));
            AnnotationParserResult<Content> contentResult = parseContent(requestBodyAnnotation.content());
            requestBody.setContent(contentResult.getResult());
            referencedSchemas.putAll(contentResult.getReferencedSchemas());
            requestBody.setRequired(requestBodyAnnotation.required());
            requestBody.setExtensions(parseExtensions(requestBodyAnnotation.extensions()));
            requestBody.set$ref(parseEmptyableText(requestBodyAnnotation.ref()));
            return new AnnotationParserResult<>(requestBody, contentResult.getReferencedSchemas());
        }
        return new AnnotationParserResult<>(requestBody, referencedSchemas);
    }

    private static AnnotationParserResult<List<Parameter>> parseParameters(io.swagger.v3.oas.annotations.Parameter[] parameterAnnotations) {
        List<Parameter> parameters = null;
        Map<String, Schema> referencedSchemas = new HashMap<>();
        if (parameterAnnotations.length > 0) {
            parameters = new ArrayList<>(parameterAnnotations.length);
            for (io.swagger.v3.oas.annotations.Parameter parameterAnnotation : parameterAnnotations) {
                AnnotationParserResult<Parameter> parameterResult = parseParameter(parameterAnnotation);
                if (parameterResult != null) {
                    parameters.add(parameterResult.getResult());
                    referencedSchemas.putAll(parameterResult.getReferencedSchemas());
                }
            }
        }
        return new AnnotationParserResult<>(parameters, referencedSchemas);
    }

    private static AnnotationParserResult<Parameter> parseParameter(io.swagger.v3.oas.annotations.Parameter parameterAnnotation) {
        if (parameterAnnotation.hidden()) {
            return null;
        }
        Parameter parameter = new Parameter();
        Map<String, Schema> referencedSchemas = new HashMap<>();
        parameter.setName(parseEmptyableText(parameterAnnotation.name()));
        parameter.setIn(parameterAnnotation.in().toString());
        parameter.setDescription(parseEmptyableText(parameterAnnotation.description()));
        parameter.setRequired(parameterAnnotation.required());
        parameter.setDeprecated(parameterAnnotation.deprecated());
        parameter.setAllowEmptyValue(parameterAnnotation.allowEmptyValue());
        parameter.setExamples(parseExamples(parameterAnnotation.examples()));
        parameter.setExtensions(parseExtensions(parameterAnnotation.extensions()));
        AnnotationParserResult<Content> contentResult = parseContent(parameterAnnotation.content());
        referencedSchemas.putAll(contentResult.getReferencedSchemas());
        if (contentResult.getResult() != null) {
            parameter.setContent(contentResult.getResult());
        } else {
            AnnotationParserResult<? extends Schema<?>> schemaResult = parseSchema(parameterAnnotation.schema(), parameterAnnotation.array());
            parameter.setSchema(schemaResult.getResult());
            referencedSchemas.putAll(schemaResult.getReferencedSchemas());
            // "Ignored if the properties content or array are specified."
            if (!(schemaResult.getResult() instanceof ArraySchema)) {
                parameter.setStyle(parseParameterStyle(parameterAnnotation.style()));
                parameter.setExplode(parseParameterExplode(parameterAnnotation.explode()));
                parameter.setAllowReserved(parameterAnnotation.allowReserved());
                parameter.setExample(parseEmptyableText(parameterAnnotation.example()));
            }
        }
        return new AnnotationParserResult<>(parameter, referencedSchemas);
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

    static AnnotationParserResult<ApiResponses> parseApiResponses(io.swagger.v3.oas.annotations.responses.ApiResponse[] apiResponseAnnotations) {
        ApiResponses apiResponses = new ApiResponses();
        Map<String, Schema> referencedSchemas = new HashMap<>();
        for (io.swagger.v3.oas.annotations.responses.ApiResponse apiResponseAnnotation : apiResponseAnnotations) {
            ApiResponse apiResponse = new ApiResponse();
            // Description is mandatory here -> Don't map empty to null
            apiResponse.setDescription(apiResponseAnnotation.description());
            AnnotationParserResult<Map<String, Header>> headersResult = parseHeaders(apiResponseAnnotation.headers());
            apiResponse.setHeaders(headersResult.getResult());
            referencedSchemas.putAll(headersResult.getReferencedSchemas());
            AnnotationParserResult<Content> contentResult = parseContent(apiResponseAnnotation.content());
            apiResponse.setContent(contentResult.getResult());
            referencedSchemas.putAll(contentResult.getReferencedSchemas());
            apiResponse.setLinks(parseLinks(apiResponseAnnotation.links()));
            apiResponse.setExtensions(parseExtensions(apiResponseAnnotation.extensions()));
            apiResponse.set$ref(parseEmptyableText(apiResponseAnnotation.ref()));
            apiResponses.put(apiResponseAnnotation.responseCode(), apiResponse);
        }
        return new AnnotationParserResult<>(apiResponses, referencedSchemas);
    }

    private static AnnotationParserResult<Map<String, Header>> parseHeaders(io.swagger.v3.oas.annotations.headers.Header[] headerAnnotations) {
        Map<String, Header> headers = null;
        Map<String, Schema> referencedSchemas = new HashMap<>();
        if (headerAnnotations.length > 0) {
            headers = new HashMap<>();
            for (io.swagger.v3.oas.annotations.headers.Header headerAnnotation : headerAnnotations) {
                AnnotationParserResult<Header> headerResult = parseHeader(headerAnnotation);
                headers.put(headerAnnotation.name(), headerResult.getResult());
                referencedSchemas.putAll(headerResult.getReferencedSchemas());
            }
        }
        return new AnnotationParserResult<>(headers, referencedSchemas);
    }

    private static AnnotationParserResult<Header> parseHeader(io.swagger.v3.oas.annotations.headers.Header headerAnnotation) {
        Header header = new Header();
        header.setDescription(parseEmptyableText(headerAnnotation.description()));
        AnnotationParserResult<Schema<?>> schemaResult = parseSchema(headerAnnotation.schema());
        header.setSchema(schemaResult.getResult());
        header.setRequired(headerAnnotation.required());
        header.setDeprecated(headerAnnotation.deprecated());
        header.set$ref(parseEmptyableText(headerAnnotation.ref()));
        return new AnnotationParserResult<>(header, schemaResult.getReferencedSchemas());
    }

    private static AnnotationParserResult<Content> parseContent(io.swagger.v3.oas.annotations.media.Content[] contentAnnotations) {
        Content content = null;
        Map<String, Schema> referencedSchemas = new HashMap<>();
        if (contentAnnotations.length > 0) {
            content = new Content();
            for (io.swagger.v3.oas.annotations.media.Content contentAnnotation : contentAnnotations) {
                String mediaTypeKey = contentAnnotation.mediaType();
                if (mediaTypeKey.isEmpty()) {
                    mediaTypeKey = "*/*";
                }
                AnnotationParserResult<MediaType> mediaTypeResult = parseMediaType(contentAnnotation);
                content.put(mediaTypeKey, mediaTypeResult.getResult());
                referencedSchemas.putAll(mediaTypeResult.getReferencedSchemas());
            }
        }
        return new AnnotationParserResult<>(content, referencedSchemas);
    }

    private static AnnotationParserResult<MediaType> parseMediaType(io.swagger.v3.oas.annotations.media.Content contentAnnotation) {
        MediaType mediaType = new MediaType();
        Map<String, Schema> referencedSchemas = new HashMap<>();
        AnnotationParserResult<? extends Schema<?>> schemaResult = parseSchema(contentAnnotation.schema(), contentAnnotation.array());
        mediaType.setSchema(schemaResult.getResult());
        referencedSchemas.putAll(schemaResult.getReferencedSchemas());
        mediaType.setExamples(parseExamples(contentAnnotation.examples()));
        AnnotationParserResult<Map<String, Encoding>> encodingsResult = parseEncodings(contentAnnotation.encoding());
        mediaType.setEncoding(encodingsResult.getResult());
        referencedSchemas.putAll(encodingsResult.getReferencedSchemas());
        mediaType.setExtensions(parseExtensions(contentAnnotation.extensions()));
        return new AnnotationParserResult<>(mediaType, referencedSchemas);
    }

    private static AnnotationParserResult<? extends Schema<?>> parseSchema(io.swagger.v3.oas.annotations.media.Schema schemaAnnotation, io.swagger.v3.oas.annotations.media.ArraySchema arraySchemaAnnotation) {
        if (arraySchemaAnnotation.schema().implementation() != Void.class) {
            return parseArraySchema(arraySchemaAnnotation);
        } else {
            return parseSchema(schemaAnnotation);
        }
    }

    private static AnnotationParserResult<ArraySchema> parseArraySchema(io.swagger.v3.oas.annotations.media.ArraySchema arraySchemaAnnotation) {
        ArraySchema arraySchema = new ArraySchema();
        AnnotationParserResult<Schema<?>> schemaResult = parseSchema(arraySchemaAnnotation.schema());
        arraySchema.setItems(schemaResult.getResult());
        arraySchema.setMinItems(arraySchema.getMinItems());
        arraySchema.setMaxItems(arraySchema.getMaxItems());
        arraySchema.setUniqueItems(arraySchema.getUniqueItems());
        arraySchema.setExtensions(parseExtensions(arraySchemaAnnotation.extensions()));
        readSchema(arraySchemaAnnotation.arraySchema(), arraySchema);
        return new AnnotationParserResult<>(arraySchema, schemaResult.getReferencedSchemas());
    }

    private static AnnotationParserResult<Schema<?>> parseSchema(io.swagger.v3.oas.annotations.media.Schema schemaAnnotation) {
        Schema<?> schema;
        Map<String, Schema> referencedSchemas;
        Class<?> implementation = schemaAnnotation.implementation();
        if (implementation != Void.class) {
            ResolvedSchema resolvedSchema = TypeUtil.getResolvedSchema(implementation);
            schema = resolvedSchema.schema;
            referencedSchemas = resolvedSchema.referencedSchemas;

        } else {
            schema = new Schema<>();
            referencedSchemas = new HashMap<>();
        }
        readSchema(schemaAnnotation, schema);
        return new AnnotationParserResult<>(schema, referencedSchemas);
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

    private static AnnotationParserResult<Map<String, Encoding>> parseEncodings(io.swagger.v3.oas.annotations.media.Encoding[] encodingAnnotations) {
        Map<String, Encoding> encodings = null;
        Map<String, Schema> referencedSchemas = new HashMap<>();
        if (encodingAnnotations.length > 0) {
            encodings = new HashMap<>();
            for (io.swagger.v3.oas.annotations.media.Encoding encodingAnnotation : encodingAnnotations) {
                AnnotationParserResult<Encoding> encodingResult = parseEncoding(encodingAnnotation);
                encodings.put(encodingAnnotation.name(), encodingResult.getResult());
                referencedSchemas.putAll(encodingResult.getReferencedSchemas());
            }
        }
        return new AnnotationParserResult<>(encodings, referencedSchemas);
    }

    private static AnnotationParserResult<Encoding> parseEncoding(io.swagger.v3.oas.annotations.media.Encoding encodingAnnotation) {
        Encoding encoding = new Encoding();
        encoding.setContentType(parseEmptyableText(encodingAnnotation.contentType()));
        AnnotationParserResult<Map<String, Header>> headersResult = parseHeaders(encodingAnnotation.headers());
        encoding.setHeaders(headersResult.getResult());
        // TODO: Map encoding.setStyle(encodingAnnotation.style());
        encoding.setExplode(encodingAnnotation.explode());
        encoding.setAllowReserved(encodingAnnotation.allowReserved());
        encoding.setExtensions(parseExtensions(encodingAnnotation.extensions()));
        return new AnnotationParserResult<>(encoding, headersResult.getReferencedSchemas());
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
