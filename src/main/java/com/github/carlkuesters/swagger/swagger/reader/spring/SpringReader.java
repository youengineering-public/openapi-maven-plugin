package com.github.carlkuesters.swagger.swagger.reader.spring;

import com.github.carlkuesters.swagger.config.ContentConfig;
import com.github.carlkuesters.swagger.reflection.AnnotatedClassService;
import com.github.carlkuesters.swagger.swagger.reader.AbstractReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.maven.plugin.logging.Log;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

public class SpringReader extends AbstractReader {

    private final SpringExceptionHandlerReader exceptionHandlerReader;

    public SpringReader(AnnotatedClassService annotatedClassService, Log log, ContentConfig contentConfig) {
        super(annotatedClassService, log, contentConfig);
        exceptionHandlerReader = new SpringExceptionHandlerReader(log);
    }

    @Override
    protected List<OpenAPIExtension> getSwaggerExtensions() {
        return Lists.newArrayList(new SpringSwaggerExtension(log));
    }

    @Override
    public Set<Class<? extends Annotation>> getApiAnnotationClasses() {
        return Sets.newHashSet(RestController.class, ControllerAdvice.class);
    }

    @Override
    public void enrich(OpenAPI swagger, Set<Class<?>> classes) {
        exceptionHandlerReader.processExceptionHandlers(classes);
        for (Class<?> controllerClass : classes) {
            enrichWithControllerClass(swagger, controllerClass);
        }
    }

    private void enrichWithControllerClass(OpenAPI swagger, Class<?> controllerClass) {
        if (controllerClass.isAnnotationPresent(Hidden.class)) {
            return;
        }
        String[] controllerPaths = SpringPathUtil.getControllerPaths(controllerClass);
        for (String controllerPath : controllerPaths) {
            for (Method method : controllerClass.getMethods()) {
                // Skip methods introduced by compiler
                if (method.isSynthetic()) {
                    continue;
                }
                RequestMapping methodRequestMapping = findMergedAnnotation(method, RequestMapping.class);
                if (methodRequestMapping != null) {
                    String[] methodPaths = SpringPathUtil.getPaths(methodRequestMapping);
                    for (String methodPath : methodPaths) {
                        String fullPath = getFullPath(controllerPath, methodPath);
                        RequestMethod[] requestMethods = methodRequestMapping.method();
                        enrichWithEndpoint(swagger, controllerClass, method, fullPath, requestMethods);
                    }
                }
            }
        }
    }

    private String getFullPath(String controllerPath, String methodPath) {
        String fullPath = controllerPath;
        if (methodPath.length() > 0) {
            if (!methodPath.startsWith("/")) {
                fullPath += "/";
            }
            fullPath += methodPath;
        }
        return fullPath;
    }

    private void enrichWithEndpoint(OpenAPI swagger, Class<?> controllerClass, Method method, String fullPath, RequestMethod[] requestMethods) {
        if (method.isAnnotationPresent(Hidden.class)) {
            return;
        }
        io.swagger.v3.oas.annotations.Operation operationAnnotation = findMergedAnnotation(method, io.swagger.v3.oas.annotations.Operation.class);
        if ((operationAnnotation != null) && operationAnnotation.hidden()) {
            return;
        }
        for (RequestMethod requestMethod : requestMethods) {
            String httpMethodName = requestMethod.toString().toLowerCase();
            parseAndAddMethod(swagger, controllerClass, method, httpMethodName, fullPath, new ArrayList<>());
        }
    }

    @Override
    protected Type resolveResponseType(Type methodType) {
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

    @Override
    protected Collection<String> getConsumes(AnnotatedElement annotatedElement) {
        RequestMapping requestMapping = findAnnotation(annotatedElement, RequestMapping.class);
        List<String> list = new LinkedList<>();
        if (requestMapping != null) {
            Collections.addAll(list, requestMapping.consumes());
        }
        return list;
    }

    @Override
    protected Collection<String> getProduces(AnnotatedElement annotatedElement) {
        RequestMapping requestMapping = findAnnotation(annotatedElement, RequestMapping.class);
        List<String> list = new LinkedList<>();
        if (requestMapping != null) {
            Collections.addAll(list, requestMapping.produces());
        }
        return list;
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
