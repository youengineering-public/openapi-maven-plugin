package com.github.carlkuesters.swagger.mavenplugin.swagger.reader.spring;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SpringResource {

    private Class<?> controllerClass;
    private List<Method> methods;
    private String controllerMapping; //FIXME should be an array
    private String resourceName;
    private String resourceKey;

    /**
     *
     * @param clazz Controller class
     * @param resourceName Resource Name
     * @param resourceKey Key containing the controller package, class controller class name, and controller-level @RequestMapping#value
     */
    public SpringResource(Class<?> clazz, String resourceName, String resourceKey) {
        this.controllerClass = clazz;
        this.resourceName = resourceName;
        this.resourceKey = resourceKey;
        methods = new ArrayList<>();

        String[] controllerRequestMappingValues = SpringUtils.getApiClassRequestMapping(controllerClass);

        this.controllerMapping = StringUtils.removeEnd(controllerRequestMappingValues[0], "/");
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public void setControllerClass(Class<?> controllerClass) {
        this.controllerClass = controllerClass;
    }

    public List<Method> getMethods() {
        return methods;
    }

    public void setMethods(List<Method> methods) {
        this.methods = methods;
    }

    public void addMethod(Method m) {
        this.methods.add(m);
    }

    public String getControllerMapping() {
        return controllerMapping;
    }

    public void setControllerMapping(String controllerMapping) {
        this.controllerMapping = controllerMapping;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResource(String resource) {
        this.resourceName = resource;
    }

    public String getResourcePath() {
        return "/" + resourceName;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

}
