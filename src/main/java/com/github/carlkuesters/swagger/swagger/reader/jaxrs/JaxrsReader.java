package com.github.carlkuesters.swagger.swagger.reader.jaxrs;

import com.github.carlkuesters.swagger.config.ContentConfig;
import com.github.carlkuesters.swagger.reflection.AnnotatedClassService;
import com.github.carlkuesters.swagger.swagger.reader.AbstractReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtensions;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.maven.plugin.logging.Log;

import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

public class JaxrsReader extends AbstractReader {

    public JaxrsReader(AnnotatedClassService annotatedClassService, Log log, ContentConfig contentConfig) {
        super(annotatedClassService, log, contentConfig);
    }

    @Override
    protected List<OpenAPIExtension> getSwaggerExtensions() {
        return Lists.newArrayList(
                new JaxrsParameterExtension(log),
                new BeanParamInjectExtension()
        );
    }

    @Override
    public Set<Class<? extends Annotation>> getApiAnnotationClasses() {
        return Sets.newHashSet(Path.class);
    }

    @Override
    public void enrich(OpenAPI swagger, Set<Class<?>> apiClasses) {
        for (Class<?> apiClass : apiClasses) {
            enrich(swagger, apiClass);
        }
    }

    private void enrich(OpenAPI swagger, Class<?> apiClass) {
        enrich(swagger, apiClass, "", null, false, new LinkedList<>());
    }

    private void enrich(
            OpenAPI swagger,
            Class<?> apiClass,
            String parentPath,
            String parentHttpMethod,
            boolean readHidden,
            List<Parameter> parentParameters
    ) {
        Path apiPath = findAnnotation(apiClass, Path.class);

        // Look for method-level annotated properties
        // Handle subresources by looking at return type
        List<Method> filteredMethods = getFilteredMethods(apiClass);
        for (Method method : filteredMethods) {
            io.swagger.v3.oas.annotations.Operation operationAnnotation = findAnnotation(method, io.swagger.v3.oas.annotations.Operation.class);
            if ((operationAnnotation != null) && operationAnnotation.hidden()) {
                continue;
            }
            Path methodPath = findAnnotation(method, Path.class);

            String parentPathValue = String.valueOf(parentPath);
            // Is method default handler within a subresource
            if((apiPath == null) && (methodPath == null) && (parentPath != null) && readHidden) {
                methodPath = new Path() {

                    @Override
                    public String value(){
                        return parentPath;
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Path.class;
                    }
                };
                parentPathValue = null;
            }
            String operationPath = getPath(apiPath, methodPath, parentPathValue);
            if (operationPath != null) {
                String httpMethod = extractHttpMethod(method, OpenAPIExtensions.chain());
                if (httpMethod == null) {
                    httpMethod = parentHttpMethod;
                }
                if (httpMethod != null) {
                    Operation operation = parseAndAddMethod(swagger, apiClass, method, httpMethod, operationPath, parentParameters);
                    if (isSubResource(httpMethod, method)) {
                        Class<?> responseClass = method.getReturnType();
                        log.debug("handling sub-resource method " + method.toString() + " -> " + responseClass);
                        enrich(swagger, responseClass, operationPath, httpMethod, true, operation.getParameters());
                    }
                }
            }
        }
    }

    private List<Method> getFilteredMethods(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        List<Method> filteredMethods = new ArrayList<>();
        for (Method method : methods) {
            if (!method.isBridge()) {
                filteredMethods.add(method);
            }
        }
        return filteredMethods;
    }

    private boolean isSubResource(String httpMethod, Method method) {
        return (httpMethod == null) && (method.getReturnType() != null) && (findAnnotation(method, Path.class) != null);
    }

    private String getPath(Path classLevelPath, Path methodLevelPath, String parentPath) {
        if (classLevelPath == null && methodLevelPath == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        if (parentPath != null && !parentPath.isEmpty() && !parentPath.equals("/")) {
            if (!parentPath.startsWith("/")) {
                parentPath = "/" + parentPath;
            }
            if (parentPath.endsWith("/")) {
                parentPath = parentPath.substring(0, parentPath.length() - 1);
            }

            stringBuilder.append(parentPath);
        }
        if (classLevelPath != null) {
            stringBuilder.append(classLevelPath.value());
        }
        if (methodLevelPath != null && !methodLevelPath.value().equals("/")) {
            String methodPath = methodLevelPath.value();
            if (!methodPath.startsWith("/") && !stringBuilder.toString().endsWith("/")) {
                stringBuilder.append("/");
            }
            if (methodPath.endsWith("/")) {
                methodPath = methodPath.substring(0, methodPath.length() - 1);
            }
            stringBuilder.append(methodPath);
        }
        String output = stringBuilder.toString();
        if (!output.startsWith("/")) {
            output = "/" + output;
        }
        if (output.endsWith("/") && output.length() > 1) {
            return output.substring(0, output.length() - 1);
        } else {
            return output;
        }
    }

    private String extractHttpMethod(Method method, Iterator<OpenAPIExtension> chain) {
        if (findAnnotation(method, GET.class) != null) {
            return "get";
        } else if (findAnnotation(method, PUT.class) != null) {
            return "put";
        } else if (findAnnotation(method, POST.class) != null) {
            return "post";
        } else if (findAnnotation(method, DELETE.class) != null) {
            return "delete";
        } else if (findAnnotation(method, OPTIONS.class) != null) {
            return "options";
        } else if (findAnnotation(method, HEAD.class) != null) {
            return "head";
        } else if (findAnnotation(method, PATCH.class) != null) {
            return "patch";
        } else {
            // Check for custom HTTP Method annotations
            for (Annotation declaredAnnotation : method.getDeclaredAnnotations()) {
                Annotation[] innerAnnotations = declaredAnnotation.annotationType().getAnnotations();
                for (Annotation innerAnnotation : innerAnnotations) {
                    if (innerAnnotation instanceof HttpMethod) {
                        HttpMethod httpMethod = (HttpMethod) innerAnnotation;
                        return httpMethod.value().toLowerCase();
                    }
                }
            }
            if (chain.hasNext()) {
                return chain.next().extractOperationMethod(method, chain);
            }
        }
        return null;
    }

    @Override
    protected boolean hasResponseContent(Type responseClassType, Method method, String httpMethod) {
        return (!responseClassType.equals(javax.ws.rs.core.Response.class) && !isSubResource(httpMethod, method));
    }

    @Override
    protected Collection<String> getConsumes(AnnotatedElement annotatedElement) {
        Consumes consumes = findAnnotation(annotatedElement, Consumes.class);
        List<String> list = new LinkedList<>();
        if (consumes != null) {
            Collections.addAll(list, consumes.value());
        }
        return list;
    }

    @Override
    protected Collection<String> getProduces(AnnotatedElement annotatedElement) {
        Produces produces = findAnnotation(annotatedElement, Produces.class);
        List<String> list = new LinkedList<>();
        if (produces != null) {
            Collections.addAll(list, produces.value());
        }
        return list;
    }

    @Override
    protected Map<Integer, String> getResponseDescriptions(Method method) {
        return Maps.newHashMap();
    }
}
