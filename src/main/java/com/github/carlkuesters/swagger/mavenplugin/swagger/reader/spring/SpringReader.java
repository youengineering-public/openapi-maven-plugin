package com.github.carlkuesters.swagger.mavenplugin.swagger.reader.spring;

import com.github.carlkuesters.swagger.mavenplugin.reflection.AnnotatedClassService;
import com.github.carlkuesters.swagger.mavenplugin.swagger.reader.AbstractReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.models.Operation;
import io.swagger.models.SecurityRequirement;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.util.PathUtils;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

public class SpringReader extends AbstractReader {

    private final SpringExceptionHandlerReader exceptionHandlerReader;

    public SpringReader(AnnotatedClassService annotatedClassService, Log log) {
        super(annotatedClassService, log);
        exceptionHandlerReader = new SpringExceptionHandlerReader(log);
    }

    @Override
    protected List<SwaggerExtension> getSwaggerExtensions() {
        return Lists.newArrayList(new SpringSwaggerExtension(log));
    }

    @Override
    public Set<Class<? extends Annotation>> getApiAnnotationClasses() {
        return Sets.newHashSet(RestController.class, ControllerAdvice.class);
    }

    @Override
    public void enrich(Swagger swagger, Set<Class<?>> classes) {
        Map<String, SpringResource> resourceMap = SpringResourceExtractor.generateResourceMap(classes);
        exceptionHandlerReader.processExceptionHandlers(classes);
        for (SpringResource resource: resourceMap.values()) {
            enrichWithResource(swagger, resource);
        }
    }

    private void enrichWithResource(Swagger swagger, SpringResource resource) {
        List<Method> methods = resource.getMethods();
        Map<String, Tag> tags = new HashMap<>();

        List<SecurityRequirement> resourceSecurities = new ArrayList<>();

        // Add the description from the controller api
        Class<?> controller = resource.getControllerClass();
        RequestMapping controllerRM = findMergedAnnotation(controller, RequestMapping.class);

        String[] controllerProduces = new String[0];
        String[] controllerConsumes = new String[0];
        if (controllerRM != null) {
            controllerConsumes = controllerRM.consumes();
            controllerProduces = controllerRM.produces();
        }

        if (controller.isAnnotationPresent(Api.class)) {
            Api api = findMergedAnnotation(controller, Api.class);
            if (!canReadApi(false, api)) {
                return;
            }
            tags = updateTagsForApi(swagger, null, api);
            resourceSecurities = getSecurityRequirements(api);
        }

        // Collect api from method with @RequestMapping
        String resourcePath = resource.getControllerMapping();
        Map<String, List<Method>> endpointsByFullPath = groupEndpointsByFullPath(resourcePath, methods);

        for (String fullPath : endpointsByFullPath.keySet()) {
            for (Method method : endpointsByFullPath.get(fullPath)) {
                ApiOperation apiOperation = findMergedAnnotation(method, ApiOperation.class);
                if ((apiOperation != null) && apiOperation.hidden()) {
                    continue;
                }

                Map<String, String> regexMap = new HashMap<>();
                String operationPath = PathUtils.parsePath(fullPath, regexMap);

                // HTTP methods
                RequestMapping requestMapping = findMergedAnnotation(method, RequestMapping.class);
                for (RequestMethod requestMethod : requestMapping.method()) {
                    String httpMethod = requestMethod.toString().toLowerCase();
                    Operation operation = parseMethod(swagger, method, httpMethod);

                    updateOperationParameters(new ArrayList<>(), regexMap, operation);

                    updateOperationProtocols(apiOperation, operation);

                    String[] apiProduces = requestMapping.produces();
                    String[] apiConsumes = requestMapping.consumes();

                    apiProduces = (apiProduces.length == 0) ? controllerProduces : apiProduces;
                    apiConsumes = (apiConsumes.length == 0) ? controllerConsumes : apiConsumes;

                    apiConsumes = updateOperationConsumes(new String[0], apiConsumes, operation);
                    apiProduces = updateOperationProduces(new String[0], apiProduces, operation);

                    updateTagsForOperation(swagger, operation, apiOperation);
                    updateOperation(apiConsumes, apiProduces, tags, resourceSecurities, operation);
                    updatePath(swagger, operationPath, httpMethod, operation);
                }
            }
        }
    }

    private Map<String, List<Method>> groupEndpointsByFullPath(String resourcePath, List<Method> methods) {
        Map<String, List<Method>> methodsByFullPath = new HashMap<>();
        for (Method method : methods) {
            RequestMapping requestMapping = findMergedAnnotation(method, RequestMapping.class);
            if (requestMapping != null) {
                String fullPath = generateFullPath(resourcePath, requestMapping);
                methodsByFullPath.computeIfAbsent(fullPath, p -> new LinkedList<>()).add(method);
            }
        }
        return methodsByFullPath;
    }

    private String generateFullPath(String resourcePath, RequestMapping requestMapping) {
        String fullPath = resourcePath;
        if (requestMapping.value().length > 0) {
            String path = requestMapping.value()[0];
            if (StringUtils.isNotEmpty(path)) {
                fullPath += (path.startsWith("/") ? path : '/' + path);
            }
        }
        return fullPath;
    }

    @Override
    protected Type resolveMethodType(Type methodType) {
        if (methodType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) methodType;
            if (parameterizedType.getRawType().equals(ResponseEntity.class)) {
                return parameterizedType.getActualTypeArguments()[0];
            }
        }
        return methodType;
    }

    @Override
    protected boolean hasResponseContent(Type responseClassType, Method method, String httpMethod) {
        return !responseClassType.equals(ResponseEntity.class);
    }

    protected List<String> getConsumes(Method method) {
        RequestMapping requestMapping = findMergedAnnotation(method, RequestMapping.class);
        return Lists.newArrayList(requestMapping.consumes());
    }

    protected List<String> getProduces(Method method) {
        RequestMapping requestMapping = findMergedAnnotation(method, RequestMapping.class);
        return Lists.newArrayList(requestMapping.produces());
    }

    @Override
    protected Map<Integer, String> getResponseDescriptions(Method method) {
        Map<Integer, String> responseDescriptions = new HashMap<>();

        ResponseStatus defaultResponseStatus = findMergedAnnotation(method, ResponseStatus.class);
        if (defaultResponseStatus != null) {
            addResponseStatusDescription(responseDescriptions, defaultResponseStatus);
        }

        List<ResponseStatus> exceptionResponseStati = exceptionHandlerReader.getResponseStatiFromExceptions(method);
        for (ResponseStatus responseStatus : exceptionResponseStati) {
            addResponseStatusDescription(responseDescriptions, responseStatus);
        }

        return responseDescriptions;
    }

    private void addResponseStatusDescription(Map<Integer, String> responseDescriotions, ResponseStatus responseStatus) {
        int code = responseStatus.code().value();
        String description = defaultIfEmpty(responseStatus.reason(), responseStatus.code().getReasonPhrase());
        responseDescriotions.put(code, description);
    }
}
