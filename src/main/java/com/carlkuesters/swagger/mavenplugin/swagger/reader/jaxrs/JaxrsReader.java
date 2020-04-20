package com.carlkuesters.swagger.mavenplugin.swagger.reader.jaxrs;

import com.carlkuesters.swagger.mavenplugin.reflection.AnnotatedClassService;
import com.carlkuesters.swagger.mavenplugin.swagger.reader.AbstractReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.jaxrs.ext.SwaggerExtensions;
import io.swagger.jersey.SwaggerJerseyJaxrs;
import io.swagger.models.Operation;
import io.swagger.models.SecurityRequirement;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.Parameter;
import io.swagger.util.PathUtils;
import org.apache.maven.plugin.logging.Log;
import org.reflections.Reflections;

import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

public class JaxrsReader extends AbstractReader {

    public JaxrsReader(AnnotatedClassService classLookupService, Log log) {
        super(classLookupService, log);
    }

    @Override
    protected List<SwaggerExtension> getSwaggerExtensions() {
        return Lists.newArrayList(
                new SwaggerJerseyJaxrs(),
                new JaxrsParameterExtension(),
                new BeanParamInjectExtension()
        );
    }

    @Override
    public Set<Class<? extends Annotation>> getApiAnnotationClasses() {
        return Sets.newHashSet(Path.class);
    }

    @Override
    public void enrich(Swagger swagger, Set<Class<?>> apiClasses) {
        for (Class<?> apiClass : apiClasses) {
            enrich(swagger, apiClass);
        }
    }

    private void enrich(Swagger swagger, Class<?> apiClass) {
        enrich(swagger, apiClass, "", null, false, new String[0], new String[0], new HashMap<>(), new ArrayList<>());
    }

    private void enrich(
            Swagger swagger,
            Class<?> apiClass,
            String parentPath,
            String parentMethod,
            boolean readHidden,
            String[] parentConsumes,
            String[] parentProduces,
            Map<String, Tag> parentTags,
            List<Parameter> parentParameters
    ) {
        Api api = findAnnotation(apiClass, Api.class);
        Path apiPath = findAnnotation(apiClass, Path.class);

        // Only enrich if allowing hidden apis OR api is not marked as hidden
        if (!canReadApi(readHidden, api)) {
            return;
        }

        Map<String, Tag> tags = updateTagsForApi(swagger, parentTags, api);
        List<SecurityRequirement> securities = getSecurityRequirements(api);
        Map<String, Tag> discoveredTags = scanClasspathForTags();

        readCommonParameters(swagger, apiClass);

        // Look for method-level annotated properties
        // Handle subresources by looking at return type
        List<Method> filteredMethods = getFilteredMethods(apiClass);
        for (Method method : filteredMethods) {
            ApiOperation apiOperation = findAnnotation(method, ApiOperation.class);
            if (apiOperation != null && apiOperation.hidden()) {
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
                Map<String, String> regexMap = new HashMap<>();
                operationPath = PathUtils.parsePath(operationPath, regexMap);

                String httpMethod = extractOperationMethod(apiOperation, method, SwaggerExtensions.chain());

                Operation operation = parseMethod(swagger, method, httpMethod);
                updateOperationParameters(parentParameters, regexMap, operation);
                updateOperationProtocols(apiOperation, operation);

                String[] apiConsumes = new String[0];
                String[] apiProduces = new String[0];

                Consumes consumes = findAnnotation(apiClass, Consumes.class);
                if (consumes != null) {
                    apiConsumes = consumes.value();
                }
                Produces produces = findAnnotation(apiClass, Produces.class);
                if (produces != null) {
                    apiProduces = produces.value();
                }

                apiConsumes = updateOperationConsumes(parentConsumes, apiConsumes, operation);
                apiProduces = updateOperationProduces(parentProduces, apiProduces, operation);

                handleSubResource(swagger, apiConsumes, httpMethod, apiProduces, tags, method, apiOperation, operationPath, operation);

                // Can't continue without a valid http method
                httpMethod = (httpMethod == null) ? parentMethod : httpMethod;
                updateTagsForOperation(swagger, operation, apiOperation);
                updateOperation(apiConsumes, apiProduces, tags, securities, operation);
                updatePath(swagger, operationPath, httpMethod, operation);
            }
            updateTagDescriptions(swagger, discoveredTags);
        }
    }

    private List<Method> getFilteredMethods(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        List<Method> filteredMethods = new ArrayList<>();
        for (Method method : methods) {
            if (!method.isBridge()) {
                filteredMethods.add(method);
            }
        }
        return filteredMethods;
    }

    /**
     * Returns true when the swagger object already contains a common parameter
     * with the same name and type as the passed parameter.
     * 
     * @param parameter The parameter to check.
     * @return true if the swagger object already contains a common parameter with the same name and type
     */
    private boolean hasCommonParameter(Swagger swagger, Parameter parameter) {
        Parameter commonParameter = swagger.getParameter(parameter.getName());
        return commonParameter != null && parameter.getIn().equals(commonParameter.getIn());
    }

    private void readCommonParameters(Swagger swagger, Class<?> cls) {
        Path path = findAnnotation(cls, Path.class);
        if (path != null) {
            return;
        }

        List<Method> filteredMethods = getFilteredMethods(cls);
        for (Method method : filteredMethods) {
            path = findAnnotation(method, Path.class);
            if (path != null) {
                return;
            }
            String httpMethod = extractOperationMethod(null, method, SwaggerExtensions.chain());
            if (httpMethod != null) {
                return;
            }
        }

        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            Annotation[] annotations = field.getAnnotations();
            if (annotations.length > 0) {
                List<Parameter> params = getParameters(swagger, cls, Arrays.asList(annotations));
                for (Parameter param : params) {
                    if (hasCommonParameter(swagger, param)) {
                        throw new RuntimeException("[" + cls.getCanonicalName() + "] Redefining common parameter '" + param.getName() + "' already defined elsewhere");
                    }
                    swagger.addParameter(param.getName(), param);
                }
            }
        }
    }

    private void updateTagDescriptions(Swagger swagger, Map<String, Tag> discoveredTags) {
        if (swagger.getTags() != null) {
            for (Tag tag : swagger.getTags()) {
                Tag rightTag = discoveredTags.get(tag.getName());
                if (rightTag != null && rightTag.getDescription() != null) {
                    tag.setDescription(rightTag.getDescription());
                }
            }
        }
    }

    private Map<String, Tag> scanClasspathForTags() {
        Map<String, Tag> tags = new HashMap<>();
        for (Class<?> aClass: new Reflections("").getTypesAnnotatedWith(SwaggerDefinition.class)) {
            SwaggerDefinition swaggerDefinition = findAnnotation(aClass, SwaggerDefinition.class);
            for (io.swagger.annotations.Tag tag : swaggerDefinition.tags()) {
                String tagName = tag.name();
                if (!tagName.isEmpty()) {
                  tags.put(tag.name(), new Tag().name(tag.name()).description(tag.description()));
                }
            }
        }
        return tags;
    }

    private void handleSubResource(Swagger swagger, String[] apiConsumes, String httpMethod, String[] apiProduces, Map<String, Tag> tags, Method method, ApiOperation apiOperation, String operationPath, Operation operation) {
        if (isSubResource(httpMethod, method)) {
            Class<?> responseClass = method.getReturnType();
            if (apiOperation != null && !apiOperation.response().equals(Void.class) && !apiOperation.response().equals(void.class)) {
                responseClass = apiOperation.response();
            }
            log.debug("handling sub-resource method " + method.toString() + " -> " + responseClass);
            enrich(swagger, responseClass, operationPath, httpMethod, true, apiConsumes, apiProduces, tags, operation.getParameters());
        }
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

    private String extractOperationMethod(ApiOperation apiOperation, Method method, Iterator<SwaggerExtension> chain) {
        if (apiOperation != null && !apiOperation.httpMethod().isEmpty()) {
            return apiOperation.httpMethod().toLowerCase();
        } else if (findAnnotation(method, GET.class) != null) {
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
        } else if (findAnnotation(method, io.swagger.jaxrs.PATCH.class) != null) {
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
                return chain.next().extractOperationMethod(apiOperation, method, chain);
            }
        }
        return null;
    }

    @Override
    protected boolean hasResponseContent(Type responseClassType, Method method, String httpMethod) {
        return (!responseClassType.equals(javax.ws.rs.core.Response.class) && !isSubResource(httpMethod, method));
    }

    @Override
    protected List<String> getConsumes(Method method) {
        Consumes consumes = findAnnotation(method, Consumes.class);
        List<String> list = new LinkedList<>();
        if (consumes != null) {
            Collections.addAll(list, consumes.value());
        }
        return list;
    }

    @Override
    protected List<String> getProduces(Method method) {
        Produces produces = findAnnotation(method, Produces.class);
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
