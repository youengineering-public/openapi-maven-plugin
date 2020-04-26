package com.github.carlkuesters.swagger.swagger.reader.spring;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.carlkuesters.swagger.swagger.reader.TypeUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.v3.core.util.ParameterProcessor;
import io.swagger.v3.jaxrs2.ResolvedParameter;
import io.swagger.v3.jaxrs2.ext.AbstractOpenAPIExtension;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.maven.plugin.logging.Log;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Consumes;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

public class SpringSwaggerExtension extends AbstractOpenAPIExtension {

    private static final String DEFAULT_VALUE = "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";
    private static final RequestParam DEFAULT_REQUEST_PARAM = (RequestParam) AnnotationBearer.class.getDeclaredMethods()[0].getParameterAnnotations()[0][0];

    private Log log;

    // Class specifically for holding default value annotations
    private static class AnnotationBearer {
        /**
         * Only used to get annotations..
         * @param requestParam ignore this
         */
        public void get(@RequestParam String requestParam) {
        }
    }

    SpringSwaggerExtension(Log log) {
        this.log = log;
    }

    @Override
    public ResolvedParameter extractParameters(List<Annotation> annotations, Type type, Set<Type> typesToSkip, Components components, Consumes classConsumes, Consumes methodConsumes, boolean includeRequestBody, JsonView jsonViewAnnotation, Iterator<OpenAPIExtension> chain) {
        ResolvedParameter resolvedParameter = new ResolvedParameter();
        if (shouldIgnoreType(type, typesToSkip)) {
            return resolvedParameter;
        }

        if (annotations.isEmpty()) {
            // Method arguments are not required to have any annotations
            annotations = Lists.newArrayList((Annotation) null);
        }

        Map<Class<?>, Annotation> annotationMap = toMap(annotations);

        resolvedParameter.parameters.addAll(extractParametersFromModelAttributeAnnotation(type, annotationMap, jsonViewAnnotation));
        resolvedParameter.parameters.addAll(extractParametersFromAnnotation(type, annotationMap));

        if (!resolvedParameter.parameters.isEmpty()) {
            return resolvedParameter;
        }
        return super.extractParameters(annotations, type, typesToSkip, components, classConsumes, methodConsumes, includeRequestBody, jsonViewAnnotation, chain);
    }

    private Map<Class<?>, Annotation> toMap(List<? extends Annotation> annotations) {
        Map<Class<?>, Annotation> annotationMap = new HashMap<>();
        for (Annotation annotation : annotations) {
            if (annotation == null) {
                continue;
            }
            annotationMap.put(annotation.annotationType(), annotation);
        }

        return annotationMap;
    }

    private boolean hasClassStartingWith(Set<Class<?>> list, String value) {
        for (Class<?> aClass : list) {
            if (aClass.getName().startsWith(value)) {
                return true;
            }
        }

        return false;
    }

    private List<Parameter> extractParametersFromAnnotation(Type type, Map<Class<?>, Annotation> annotations) {
        List<Parameter> parameters = new ArrayList<>();

        if (isRequestParamType(type, annotations)) {
            parameters.add(extractRequestParam(type, (RequestParam)annotations.get(RequestParam.class)));
        }
        if (annotations.containsKey(PathVariable.class)) {
            PathVariable pathVariable = (PathVariable) annotations.get(PathVariable.class);
            PathParameter pathParameter = extractPathVariable(type, pathVariable);
            parameters.add(pathParameter);
        }
        if (annotations.containsKey(RequestHeader.class)) {
            RequestHeader requestHeader = (RequestHeader) annotations.get(RequestHeader.class);
            HeaderParameter headerParameter = extractRequestHeader(type, requestHeader);
            parameters.add(headerParameter);
        }
        if (annotations.containsKey(CookieValue.class)) {
            CookieValue cookieValue = (CookieValue) annotations.get(CookieValue.class);
            CookieParameter cookieParameter = extractCookieValue(type, cookieValue);
            parameters.add(cookieParameter);
        }

        return parameters;
    }

    private QueryParameter extractRequestParam(Type type, RequestParam requestParam) {
        if (requestParam == null) {
            requestParam = DEFAULT_REQUEST_PARAM;
        }
        String paramName = StringUtils.defaultIfEmpty(requestParam.value(), requestParam.name());
        QueryParameter queryParameter = new QueryParameter();
        queryParameter.setName(paramName);
        queryParameter.setRequired(requestParam.required());
        setSchema(queryParameter, type, requestParam.defaultValue());
        return queryParameter;
    }

    private CookieParameter extractCookieValue(Type type, CookieValue cookieValue) {
        String paramName = StringUtils.defaultIfEmpty(cookieValue.value(), cookieValue.name());
        CookieParameter cookieParameter = new CookieParameter();
        cookieParameter.setName(paramName);
        cookieParameter.setRequired(cookieValue.required());
        setSchema(cookieParameter, type, cookieValue.defaultValue());
        return cookieParameter;
    }

    private HeaderParameter extractRequestHeader(Type type, RequestHeader requestHeader) {
        String paramName = StringUtils.defaultIfEmpty(requestHeader.value(), requestHeader.name());
        HeaderParameter headerParameter = new HeaderParameter();
        headerParameter.setName(paramName);
        headerParameter.setRequired(requestHeader.required());
        setSchema(headerParameter, type, requestHeader.defaultValue());
        return headerParameter;
    }

    private PathParameter extractPathVariable(Type type, PathVariable pathVariable) {
        String paramName = StringUtils.defaultIfEmpty(pathVariable.value(), pathVariable.name());
        PathParameter pathParameter = new PathParameter();
        pathParameter.setName(paramName);
        pathParameter.setSchema(TypeUtil.getPrimitiveSchema(type, log));
        return pathParameter;
    }

    private void setSchema(Parameter parameter, Type type, String defaultValue) {
        Schema schema = TypeUtil.getPrimitiveSchema(type, log);
        if (!DEFAULT_VALUE.equals(defaultValue)) {
            schema.setDefault(defaultValue);
            // Supplying a default value implicitly sets required() to false.
            parameter.setRequired(false);
        }
        parameter.setSchema(schema);
    }

    private List<Parameter> extractParametersFromModelAttributeAnnotation(Type type, Map<Class<?>, Annotation> annotations, JsonView jsonViewAnnotation) {
        ModelAttribute modelAttribute = (ModelAttribute)annotations.get(ModelAttribute.class);
        if ((modelAttribute == null || !hasClassStartingWith(annotations.keySet(), "org.springframework.web.bind.annotation"))&& BeanUtils.isSimpleProperty(TypeUtils.getRawType(type, null))) {
            return Collections.emptyList();
        }

        List<Parameter> parameters = new ArrayList<>();
        Class<?> clazz = TypeUtils.getRawType(type, type);
        for (PropertyDescriptor propertyDescriptor : BeanUtils.getPropertyDescriptors(clazz)) {
            // Get all the valid setter methods inside the bean
            Method propertyDescriptorSetter = propertyDescriptor.getWriteMethod();
            if (propertyDescriptorSetter != null) {
                io.swagger.v3.oas.annotations.Parameter propertySetterParameterAnnotation = AnnotationUtils.findAnnotation(propertyDescriptorSetter, io.swagger.v3.oas.annotations.Parameter.class);
                if (propertySetterParameterAnnotation == null) {
                    // If we find a setter that doesn't have @Parameter annotation, then skip it
                    continue;
                }

                // Here we have a bean setter method that is annotated with @Parameter, but we still
                // need to know what type of parameter to create. In order to do this, we look for
                // any annotation attached to the first method parameter of the setter function.
                Annotation[][] parameterAnnotations = propertyDescriptorSetter.getParameterAnnotations();
                if (parameterAnnotations == null || parameterAnnotations.length == 0) {
                    continue;
                }

                Class parameterClass = propertyDescriptor.getPropertyType();
                List<Parameter> propertySetterExtractedParameters = this.extractParametersFromAnnotation(parameterClass, toMap(Arrays.asList(parameterAnnotations[0])));

                for (Parameter parameter : propertySetterExtractedParameters) {
                    if (Strings.isNullOrEmpty(parameter.getName())) {
                        parameter.setName(propertyDescriptor.getDisplayName());
                    }
                    ParameterProcessor.applyAnnotations(parameter, type, Lists.newArrayList(propertySetterParameterAnnotation), new Components(), new String[0], new String[0], jsonViewAnnotation);
                }
                parameters.addAll(propertySetterExtractedParameters);
            }
        }

        return parameters;
    }

    private boolean isRequestParamType(Type type, Map<Class<?>, Annotation> annotations) {
        RequestParam requestParam = (RequestParam) annotations.get(RequestParam.class);
        return requestParam != null || (BeanUtils.isSimpleProperty(TypeUtils.getRawType(type, type)) && !hasClassStartingWith(annotations.keySet(), "org.springframework.web.bind.annotation"));
    }

    @Override
    public boolean shouldIgnoreType(Type type, Set<Type> typesToSkip) {
        Class<?> clazz = TypeUtils.getRawType(type, type);
        if (clazz == null) {
            return false;
        }

        String className = clazz.getName();
        switch (className) {
            case "javax.servlet.ServletRequest":
            case "javax.servlet.ServletResponse":
            case "javax.servlet.http.HttpSession":
            case "javax.servlet.http.PushBuilder":
            case "java.security.Principal":
            case "java.io.OutputStream":
            case "java.io.Writer":
                return true;
        }

        return className.startsWith("org.springframework") && !"org.springframework.web.multipart.MultipartFile".equals(className);
    }
}
