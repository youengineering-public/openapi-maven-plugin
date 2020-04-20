package com.github.carlkuesters.swagger.mavenplugin.swagger.reader.spring;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

class SpringResourceExtractor {

    // Relate all methods to one base request mapping if multiple controllers exist for that mapping
    // Get all methods from each controller & find their request mapping
    // Create map - resource string (after first slash) as key, new SpringResource as value
    static Map<String, SpringResource> generateResourceMap(Set<Class<?>> controllerClasses) {
        Map<String, SpringResource> resourceMap = new HashMap<>();
        for (Class<?> controllerCLass : controllerClasses) {
            analyzeControllerClass(controllerCLass, resourceMap);
        }
        return resourceMap;
    }

    private static void analyzeControllerClass(Class<?> controllerClass, Map<String, SpringResource> resourceMap) {
        // Iterate over all value attributes of the class-level RequestMapping annotation
        String[] classRequestMappings = SpringUtils.getApiClassRequestMapping(controllerClass);
        for (String classRequestMapping : classRequestMappings) {
            for (Method method : controllerClass.getMethods()) {
                // Skip methods introduced by compiler
                if (method.isSynthetic()) {
                    continue;
                }

                // Look for method-level @RequestMapping annotation
                RequestMapping methodRequestMapping = findMergedAnnotation(method, RequestMapping.class);
                if (methodRequestMapping != null) {
                    String[] methodRequestMappingValues = methodRequestMapping.value();

                    // For each method-level @RequestMapping annotation, iterate over HTTP Verbs
                    RequestMethod[] requestMethods = methodRequestMapping.method();
                    for (RequestMethod requestMethod : requestMethods) {
                        // Check for cases where method-level @RequestMapping#value is not set, and use the controllers @RequestMapping
                        if (methodRequestMappingValues.length == 0) {
                            // The map key is a concat of the following:
                            //   1. The controller package
                            //   2. The controller class name
                            //   3. The controller-level @RequestMapping#value
                            String resourceKey = controllerClass.getCanonicalName() + classRequestMapping + requestMethod;
                            if (!resourceMap.containsKey(resourceKey)) {
                                resourceMap.put( resourceKey, new SpringResource(controllerClass, classRequestMapping, resourceKey));
                            }
                            resourceMap.get(resourceKey).addMethod(method);
                        } else {
                            // Here we know that method-level @RequestMapping#value is populated, so
                            // iterate over all the @RequestMapping#value attributes, and add them to the resource map.
                            for (String methodRequestMappingValue : methodRequestMappingValues) {
                                String resourceKey = controllerClass.getCanonicalName() + classRequestMapping + methodRequestMappingValue + requestMethod;
                                if (!(classRequestMapping + methodRequestMappingValue).isEmpty()) {
                                    if (!resourceMap.containsKey(resourceKey)) {
                                        resourceMap.put(resourceKey, new SpringResource(controllerClass, methodRequestMappingValue, resourceKey));
                                    }
                                    resourceMap.get(resourceKey).addMethod(method);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
