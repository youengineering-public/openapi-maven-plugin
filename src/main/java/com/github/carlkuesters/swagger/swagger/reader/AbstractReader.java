package com.github.carlkuesters.swagger.swagger.reader;

import com.github.carlkuesters.swagger.reflection.AnnotatedClassService;
import com.github.carlkuesters.swagger.reflection.AnnotatedMethodService;
import com.github.carlkuesters.swagger.swagger.reader.jaxrs.AbstractReaderSwaggerExtension;
import com.google.common.collect.Lists;
import com.sun.jersey.api.core.InjectParam;
import io.swagger.annotations.*;
import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.jaxrs.ext.SwaggerExtensions;
import io.swagger.models.Path;
import io.swagger.models.Tag;
import io.swagger.models.*;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.BaseReaderUtils;
import io.swagger.util.ParameterProcessor;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

public abstract class AbstractReader {

    private final AnnotatedClassService annotatedClassService;
    protected final Log log;

    private String operationIdFormat;
    
    /**
     * Supported parameters: {{packageName}}, {{className}}, {{methodName}}, {{httpMethod}}
     * Suggested default value is: "{{className}}_{{methodName}}_{{httpMethod}}"
     */
    private static final String OPERATION_ID_FORMAT_DEFAULT = "{{methodName}}";

    public AbstractReader(AnnotatedClassService annotatedClassService, Log log) {
        this.annotatedClassService = annotatedClassService;
        this.log = log;
    }

    public void enrich(Swagger swagger) {
        initializeSwaggerExtensions(swagger);
        enrich(swagger, getApiClasses());
    }

    private void initializeSwaggerExtensions(Swagger swagger) {
        List<SwaggerExtension> swaggerExtensions = getSwaggerExtensions();
        for (SwaggerExtension swaggerExtension : swaggerExtensions) {
            if (swaggerExtension instanceof AbstractReaderSwaggerExtension) {
                AbstractReaderSwaggerExtension abstractReaderSwaggerExtension = (AbstractReaderSwaggerExtension) swaggerExtension;
                abstractReaderSwaggerExtension.setContext(this, swagger);
            }
        }
        SwaggerExtensions.setExtensions(swaggerExtensions);
    }

    protected abstract List<SwaggerExtension> getSwaggerExtensions();

    private Set<Class<?>> getApiClasses() {
        Set<Class<? extends Annotation>> apiAnnotationClasses = getApiAnnotationClasses();
        apiAnnotationClasses.add(Api.class);

        Set<Class<?>> apiClasses = new HashSet<>();
        for (Class<? extends Annotation> annotationClass : apiAnnotationClasses) {
            apiClasses.addAll(annotatedClassService.getAnnotatedClasses(annotationClass));
        }
        return apiClasses;
    }

    public abstract Set<Class<? extends Annotation>> getApiAnnotationClasses();

    public abstract void enrich(Swagger swagger, Set<Class<?>> classes);

    protected List<SecurityRequirement> getSecurityRequirements(Api api) {
        List<SecurityRequirement> securities = new ArrayList<>();
        if(api == null) {
            return securities;
        }

        for (Authorization auth : api.authorizations()) {
            if (auth.value().isEmpty()) {
                continue;
            }
            SecurityRequirement security = new SecurityRequirement();
            security.setName(auth.value());
            for (AuthorizationScope scope : auth.scopes()) {
                if (!scope.scope().isEmpty()) {
                    security.addScope(scope.scope());
                }
            }
            securities.add(security);
        }
        return securities;
    }

    protected void updateOperationParameters(List<Parameter> parentParameters, Map<String, String> regexMap, Operation operation) {
        if (parentParameters != null) {
            for (Parameter param : parentParameters) {
                operation.parameter(param);
            }
        }
        for (Parameter param : operation.getParameters()) {
            String pattern = regexMap.get(param.getName());
            if (pattern != null) {
                param.setPattern(pattern);
            }
        }
    }

    private Map<String, Property> parseResponseHeaders(ResponseHeader[] headers) {
        if (headers == null) {
            return null;
        }
        Map<String, Property> responseHeaders = null;
        for (ResponseHeader header : headers) {
            if (header.name().isEmpty()) {
                continue;
            }
            if (responseHeaders == null) {
                responseHeaders = new HashMap<>();
            }
            Class<?> cls = header.response();

            if (!cls.equals(Void.class) && !cls.equals(void.class)) {
                Property property = ModelConverters.getInstance().readAsProperty(cls);
                if (property != null) {
                    Property responseProperty;

                    if (header.responseContainer().equalsIgnoreCase("list")) {
                        responseProperty = new ArrayProperty(property);
                    } else if (header.responseContainer().equalsIgnoreCase("map")) {
                        responseProperty = new MapProperty(property);
                    } else {
                        responseProperty = property;
                    }
                    responseProperty.setDescription(header.description());
                    responseHeaders.put(header.name(), responseProperty);
                }
            }
        }
        return responseHeaders;
    }

    protected void updatePath(Swagger swagger, String operationPath, String httpMethod, Operation operation) {
        if (httpMethod == null) {
            return;
        }
        Path path = swagger.getPath(operationPath);
        if (path == null) {
            path = new Path();
            swagger.path(operationPath, path);
        }
        path.set(httpMethod, operation);
    }

    protected void updateTagsForOperation(Swagger swagger, Operation operation, ApiOperation apiOperation) {
        if (apiOperation == null) {
            return;
        }
        for (String tag : apiOperation.tags()) {
            if (!tag.isEmpty()) {
                operation.tag(tag);
                swagger.tag(new Tag().name(tag));
            }
        }
    }

    protected boolean canReadApi(boolean readHidden, Api api) {
        return (api == null) || (readHidden) || (!api.hidden());
    }

    protected void updateOperationProtocols(ApiOperation apiOperation, Operation operation) {
        if(apiOperation == null) {
            return;
        }
        String[] protocols = apiOperation.protocols().split(",");
        for (String protocol : protocols) {
            String trimmed = protocol.trim();
            if (!trimmed.isEmpty()) {
                operation.scheme(Scheme.forValue(trimmed));
            }
        }
    }

    protected Map<String, Tag> updateTagsForApi(Swagger swagger, Map<String, Tag> parentTags, Api api) {
        // the value will be used as a tag for 2.0 UNLESS a Tags annotation is present
        Map<String, Tag> tagsMap = new HashMap<>();
        for (Tag tag : extractTags(api)) {
            tagsMap.put(tag.getName(), tag);
        }
        if (parentTags != null) {
            tagsMap.putAll(parentTags);
        }
        for (Tag tag : tagsMap.values()) {
            swagger.tag(tag);
        }
        return tagsMap;
    }

    private Set<Tag> extractTags(Api api) {
        Set<Tag> output = new LinkedHashSet<>();
        if(api == null) {
            return output;
        }

        boolean hasExplicitTags = false;
        for (String tag : api.tags()) {
            if (!tag.isEmpty()) {
                hasExplicitTags = true;
                output.add(new Tag().name(tag));
            }
        }
        if (!hasExplicitTags) {
            // derive tag from api path + description
            String tagString = api.value().replace("/", "");
            if (!tagString.isEmpty()) {
                Tag tag = new Tag().name(tagString);
                if (!api.description().isEmpty()) {
                    tag.description(api.description());
                }
                output.add(tag);
            }
        }
        return output;
    }

    protected void updateOperation(String[] apiConsumes, String[] apiProduces, Map<String, Tag> tags, List<SecurityRequirement> securities, Operation operation) {
        if (operation == null) {
            return;
        }
        if (operation.getConsumes() == null) {
            for (String mediaType : apiConsumes) {
                operation.consumes(mediaType);
            }
        }
        if (operation.getProduces() == null) {
            for (String mediaType : apiProduces) {
                operation.produces(mediaType);
            }
        }

        if (operation.getTags() == null) {
            for (String tagString : tags.keySet()) {
                operation.tag(tagString);
            }
        }
        for (SecurityRequirement security : securities) {
            operation.security(security);
        }
    }

    private boolean hasValidAnnotations(List<Annotation> parameterAnnotations) {
        // Because method parameters can contain parameters that are valid, but
        // not part of the API contract, first check to make sure the parameter
        // has at lease one annotation before processing it.  Also, check a
        // whitelist to make sure that the annotation of the parameter is
        // compatible with spring-maven-plugin

        List<Type> validParameterAnnotations = new ArrayList<>();
        validParameterAnnotations.add(ModelAttribute.class);
        validParameterAnnotations.add(BeanParam.class);
        validParameterAnnotations.add(InjectParam.class);
        validParameterAnnotations.add(ApiParam.class);
        validParameterAnnotations.add(PathParam.class);
        validParameterAnnotations.add(QueryParam.class);
        validParameterAnnotations.add(HeaderParam.class);
        validParameterAnnotations.add(FormParam.class);
        validParameterAnnotations.add(RequestParam.class);
        validParameterAnnotations.add(RequestBody.class);
        validParameterAnnotations.add(PathVariable.class);
        validParameterAnnotations.add(RequestHeader.class);
        validParameterAnnotations.add(RequestPart.class);
        validParameterAnnotations.add(CookieValue.class);


        boolean hasValidAnnotation = false;
        for (Annotation potentialAnnotation : parameterAnnotations) {
            if (validParameterAnnotations.contains(potentialAnnotation.annotationType())) {
                hasValidAnnotation = true;
                break;
            }
        }

        return hasValidAnnotation;
    }

    protected List<Parameter> getParameters(Swagger swagger, Type type, List<Annotation> annotations) {
        return getParameters(swagger, type, annotations, new HashSet<>());
    }

    public List<Parameter> getParameters(Swagger swagger, Type type, List<Annotation> annotations, Set<Type> typesToSkip) {
        if (!hasValidAnnotations(annotations) || isApiParamHidden(annotations)) {
            return Collections.emptyList();
        }

        Iterator<SwaggerExtension> chain = SwaggerExtensions.chain();
        List<Parameter> parameters = new ArrayList<>();
        Class<?> cls = TypeUtils.getRawType(type, type);
        log.debug("Looking for path/query/header/form/cookie params in " + cls);

        if (chain.hasNext()) {
            SwaggerExtension extension = chain.next();
            log.debug("trying extension " + extension);
            parameters = extension.extractParameters(annotations, type, typesToSkip, chain);
        }

        if (!parameters.isEmpty()) {
            for (Parameter parameter : parameters) {
                ParameterProcessor.applyAnnotations(swagger, parameter, type, annotations);
            }
        } else {
            log.debug("Looking for body params in " + cls);
            if (!typesToSkip.contains(type)) {
                Parameter param = ParameterProcessor.applyAnnotations(swagger, null, type, annotations);
                if (param != null) {
                    // parameters is guaranteed to be empty at this point, replace it with a mutable collection
                    parameters = Lists.newArrayList();
                    parameters.add(param);
                }
            }
        }
        return parameters;
    }

    private boolean isApiParamHidden(List<Annotation> parameterAnnotations) {
        for (Annotation parameterAnnotation : parameterAnnotations) {
            if (parameterAnnotation instanceof ApiParam) {
                return ((ApiParam) parameterAnnotation).hidden();
            }
        }

        return false;
    }

    protected void updateApiResponse(Swagger swagger, Operation operation, ApiResponses responseAnnotation) {
        boolean contains200 = false;
        boolean contains2xx = false;
        for (ApiResponse apiResponse : responseAnnotation.value()) {
            Map<String, Property> responseHeaders = parseResponseHeaders(apiResponse.responseHeaders());
            Class<?> responseClass = apiResponse.response();
            Response response = new Response()
                    .description(apiResponse.message())
                    .headers(responseHeaders);

            if (responseClass.equals(Void.class)) {
                if (operation.getResponses() != null) {
                    Response apiOperationResponse = operation.getResponses().get(String.valueOf(apiResponse.code()));
                    if (apiOperationResponse != null) {
                        response.setSchema(apiOperationResponse.getSchema());
                    }
                }
            } else if (TypeUtil.isPrimitive(responseClass)) {
                Property property = ModelConverters.getInstance().readAsProperty(responseClass);
                if (property != null) {
                    response.setSchema(ResponseContainerUtil.withContainer(property, apiResponse.responseContainer()));
                }
            } else {
                Map<String, Model> models = ModelConverters.getInstance().read(responseClass);
                for (String key : models.keySet()) {
                    final Property schema = new RefProperty().asDefault(key);
                    response.setSchema(ResponseContainerUtil.withContainer(schema, apiResponse.responseContainer()));
                    swagger.model(key, models.get(key));
                }
                models = ModelConverters.getInstance().readAll(responseClass);
                for (Map.Entry<String, Model> entry : models.entrySet()) {
                    swagger.model(entry.getKey(), entry.getValue());
                }

                if (response.getSchema() == null) {
                    Map<String, Response> responses = operation.getResponses();
                    if (responses != null) {
                        Response apiOperationResponse = responses.get(String.valueOf(apiResponse.code()));
                        if (apiOperationResponse != null) {
                            response.setSchema(apiOperationResponse.getSchema());
                        }
                    }
                }
            }

            if (apiResponse.code() == 0) {
                operation.defaultResponse(response);
            } else {
                operation.response(apiResponse.code(), response);
            }
            if (apiResponse.code() == 200) {
                contains200 = true;
            } else if (apiResponse.code() > 200 && apiResponse.code() < 300) {
                contains2xx = true;
            }
        }
        if (!contains200 && contains2xx) {
            Map<String, Response> responses = operation.getResponses();
            //technically should not be possible at this point, added to be safe
            if (responses != null) {
                responses.remove("200");
            }
        }
    }

    protected String[] updateOperationProduces(String[] parentProduces, String[] apiProduces, Operation operation) {
        if (parentProduces != null) {
            Set<String> both = new LinkedHashSet<>(Arrays.asList(apiProduces));
            both.addAll(Arrays.asList(parentProduces));
            if (operation.getProduces() != null) {
                both.addAll(operation.getProduces());
            }
            apiProduces = both.toArray(new String[both.size()]);
        }
        return apiProduces;
    }

    protected String[] updateOperationConsumes(String[] parentConsumes, String[] apiConsumes, Operation operation) {
        if (parentConsumes != null) {
            Set<String> both = new LinkedHashSet<>(Arrays.asList(apiConsumes));
            both.addAll(Arrays.asList(parentConsumes));
            if (operation.getConsumes() != null) {
                both.addAll(operation.getConsumes());
            }
            apiConsumes = both.toArray(new String[both.size()]);
        }
        return apiConsumes;
    }

    private void readImplicitParameters(Swagger swagger, Method method, Operation operation) {
        ApiImplicitParams implicitParams = AnnotationUtils.findAnnotation(method, ApiImplicitParams.class);
        if (implicitParams == null) {
            return;
        }
        for (ApiImplicitParam param : implicitParams.value()) {
            Class<?> cls;
            try {
                cls = param.dataTypeClass() == Void.class ?
                        Class.forName(param.dataType()) :
                        param.dataTypeClass();
            } catch (ClassNotFoundException e) {
                cls = method.getDeclaringClass();
            }

            Parameter p = readImplicitParam(swagger, param, cls);
            if (p != null) {
                if (p instanceof BodyParameter) {
                    Iterator<Parameter> iterator = operation.getParameters().iterator();
                    while(iterator.hasNext()) {
                        Parameter parameter = iterator.next();
                        if (parameter instanceof BodyParameter) {
                            iterator.remove();
                        }
                    }
                }
                operation.addParameter(p);
            }
        }
    }

    private Parameter readImplicitParam(Swagger swagger, ApiImplicitParam param, Class<?> apiClass) {
        Parameter parameter;
        if (param.paramType().equalsIgnoreCase("path")) {
            parameter = new PathParameter();
        } else if (param.paramType().equalsIgnoreCase("query")) {
            parameter = new QueryParameter();
        } else if (param.paramType().equalsIgnoreCase("form") || param.paramType().equalsIgnoreCase("formData")) {
            parameter = new FormParameter();
        } else if (param.paramType().equalsIgnoreCase("body")) {
            parameter = new BodyParameter();
        } else if (param.paramType().equalsIgnoreCase("header")) {
            parameter = new HeaderParameter();
        } else {
            return null;
        }

        return ParameterProcessor.applyAnnotations(swagger, parameter, apiClass, Arrays.asList(new Annotation[]{param}));
    }

    private void processOperationDecorator(Operation operation, Method method) {
        final Iterator<SwaggerExtension> chain = SwaggerExtensions.chain();
        if (chain.hasNext()) {
            SwaggerExtension extension = chain.next();
            extension.decorateOperation(operation, method, chain);
        }
    }

    protected Operation parseMethod(Swagger swagger, Method method, String httpMethod) {
        int responseCode = 200;
        Operation operation = new Operation();
        ApiOperation apiOperation = findMergedAnnotation(method, ApiOperation.class);

        String operationId = getOperationId(method, httpMethod);

        String responseContainer = null;

        Type responseClassType = null;
        Map<String, Property> defaultResponseHeaders = null;

        if (apiOperation != null) {
            if (apiOperation.hidden()) {
                return null;
            }
            if (!apiOperation.nickname().isEmpty()) {
                operationId = apiOperation.nickname();
            }

            defaultResponseHeaders = parseResponseHeaders(apiOperation.responseHeaders());
            operation.summary(apiOperation.value()).description(apiOperation.notes());

            Map<String, Object> customExtensions = BaseReaderUtils.parseExtensions(apiOperation.extensions());
            operation.setVendorExtensions(customExtensions);

            if (!apiOperation.response().equals(Void.class) && !apiOperation.response().equals(void.class)) {
                responseClassType = apiOperation.response();
            }
            if (!apiOperation.responseContainer().isEmpty()) {
                responseContainer = apiOperation.responseContainer();
            }
            List<SecurityRequirement> securities = new ArrayList<>();
            for (Authorization auth : apiOperation.authorizations()) {
                if (!auth.value().isEmpty()) {
                    SecurityRequirement security = new SecurityRequirement();
                    security.setName(auth.value());
                    for (AuthorizationScope scope : auth.scopes()) {
                        if (!scope.scope().isEmpty()) {
                            security.addScope(scope.scope());
                        }
                    }
                    securities.add(security);
                }
            }
            for (SecurityRequirement sec : securities) {
                operation.security(sec);
            }
            responseCode = apiOperation.code();
        }
        operation.operationId(operationId);

        if (responseClassType == null) {
            // pick out response from method declaration
            log.debug("picking up response class from method " + method);
            responseClassType = method.getGenericReturnType();
        }
        responseClassType = resolveMethodType(responseClassType);
        boolean hasApiAnnotation = false;
        if (responseClassType instanceof Class) {
            hasApiAnnotation = findAnnotation((Class) responseClassType, Api.class) != null;
        }
        if ((responseClassType != null)
                && !responseClassType.equals(Void.class)
                && !responseClassType.equals(void.class)
                && !hasApiAnnotation
                && hasResponseContent(responseClassType, method, httpMethod)) {
            if (TypeUtil.isPrimitive(responseClassType)) {
                Property property = ModelConverters.getInstance().readAsProperty(responseClassType);
                if (property != null) {
                    Property responseProperty = ResponseContainerUtil.withContainer(property, responseContainer);
                    operation.response(responseCode, new Response()
                            .description("successful operation")
                            .schema(responseProperty)
                            .headers(defaultResponseHeaders));
                }
            } else {
                Map<String, Model> models = ModelConverters.getInstance().read(responseClassType);
                if (models.isEmpty()) {
                    Property p = ModelConverters.getInstance().readAsProperty(responseClassType);
                    operation.response(responseCode, new Response()
                            .description("successful operation")
                            .schema(p)
                            .headers(defaultResponseHeaders));
                }
                for (String key : models.keySet()) {
                    Property responseProperty = ResponseContainerUtil.withContainer(new RefProperty().asDefault(key), responseContainer);
                    operation.response(responseCode, new Response()
                            .description("successful operation")
                            .schema(responseProperty)
                            .headers(defaultResponseHeaders));
                    swagger.model(key, models.get(key));
                }
            }
            Map<String, Model> models = ModelConverters.getInstance().readAll(responseClassType);
            for (Map.Entry<String, Model> entry : models.entrySet()) {
                swagger.model(entry.getKey(), entry.getValue());
            }
        }

        List<String> consumes = getConsumes(method);
        if (consumes.size() > 0) {
            operation.consumes(consumes);
        }
        List<String> produces = getProduces(method);
        if (produces.size() > 0) {
            operation.produces(produces);
        }

        ApiResponses responseAnnotation = findMergedAnnotation(method, ApiResponses.class);
        if (responseAnnotation != null) {
            updateApiResponse(swagger, operation, responseAnnotation);
        }
        Map<Integer, String> responseDescriptions = getResponseDescriptions(method);
        updateResponseStati(operation, responseDescriptions);

        if (findAnnotation(method, Deprecated.class) != null) {
            operation.deprecated(true);
        }

        // process parameters

        Class[] parameterTypes = method.getParameterTypes();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Annotation[][] paramAnnotations = AnnotatedMethodService.findAllParamAnnotations(method);

        DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        String[] defaultParameterNames = parameterNameDiscoverer.getParameterNames(method);

        for (int i = 0; i < parameterTypes.length; i++) {
            Type type = genericParameterTypes[i];
            List<Annotation> annotations = Arrays.asList(paramAnnotations[i]);
            List<Parameter> parameters = getParameters(swagger, type, annotations);

            for (Parameter parameter : parameters) {
                if(parameter.getName().isEmpty()) {
                    parameter.setName(defaultParameterNames[i]);
                }
                parameter = replaceBodyArrayModelForOctetStream(operation, parameter);
                operation.parameter(parameter);
            }
        }

        if (operation.getResponses() == null) {
            operation.defaultResponse(new Response().description("successful operation"));
        }

        this.readImplicitParameters(swagger, method, operation);

        processOperationDecorator(operation, method);

        return operation;
    }

    private String getOperationId(Method method, String httpMethod) {
  		if (this.operationIdFormat == null) {
  			this.operationIdFormat = OPERATION_ID_FORMAT_DEFAULT;
  		}
  		
  		String packageName = method.getDeclaringClass().getPackage().getName();
  		String className = method.getDeclaringClass().getSimpleName();
  		String methodName = method.getName();
        
  		StrBuilder sb = new StrBuilder(this.operationIdFormat);
  		sb.replaceAll("{{packageName}}", packageName);
  		sb.replaceAll("{{className}}", className);
  		sb.replaceAll("{{methodName}}", methodName);
  		sb.replaceAll("{{httpMethod}}", httpMethod);
  		
  		return sb.toString();
    }

    private Parameter replaceBodyArrayModelForOctetStream(Operation operation, Parameter parameter) {
        if (parameter instanceof BodyParameter
                && operation.getConsumes() != null
                && operation.getConsumes().contains("application/octet-stream")) {
            BodyParameter bodyParam = (BodyParameter) parameter;
            Model schema = bodyParam.getSchema();
            if (schema instanceof ArrayModel) {
                ArrayModel arrayModel = (ArrayModel) schema;
                Property items = arrayModel.getItems();
                if (items != null && items.getFormat() == "byte" && items.getType() == "string") {
                    ModelImpl model = new ModelImpl();
                    model.setFormat("byte");
                    model.setType("string");
                    bodyParam.setSchema(model);
                }
            }
        }
        return parameter;
    }

    protected void updateResponseStati(Operation operation, Map<Integer, String> responseDescriptions) {
        for (Map.Entry<Integer, String> responseDescriptionEntry : responseDescriptions.entrySet()) {
            Integer code = responseDescriptionEntry.getKey();
            String description = responseDescriptionEntry.getValue();
            Response response = operation.getResponses().get(code);
            if (response != null) {
                if (StringUtils.isNotEmpty(description)) {
                    response.setDescription(description);
                }
            } else {
                operation.response(code, new Response().description(description));
            }
        }
    }

	public void setOperationIdFormat(String operationIdFormat) {
		this.operationIdFormat = operationIdFormat;
	}

	protected Type resolveMethodType(Type methodType) {
        return methodType;
    }

    protected abstract boolean hasResponseContent(Type responseClassType, Method method, String httpMethod);

    protected abstract Map<Integer, String> getResponseDescriptions(Method method);

    protected abstract List<String> getConsumes(Method method);

    protected abstract List<String> getProduces(Method method);
}
