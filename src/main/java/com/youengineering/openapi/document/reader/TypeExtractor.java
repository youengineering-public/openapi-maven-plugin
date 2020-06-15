package com.youengineering.openapi.document.reader;

import com.youengineering.openapi.reflection.AnnotatedMethodService;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class TypeExtractor {

    public static List<TypeWithAnnotations> extractTypes(Class<?> clazz) {
        List<TypeWithAnnotations> typesWithAnnotations = new LinkedList<>();
        typesWithAnnotations.addAll(getPropertyTypes(clazz));
        typesWithAnnotations.addAll(getMethodParameterTypes(clazz));
        typesWithAnnotations.addAll(getConstructorParameterTypes(clazz));
        return typesWithAnnotations;
    }

    private static List<TypeWithAnnotations> getPropertyTypes(Class<?> clazz) {
        List<TypeWithAnnotations> typesWithAnnotations = new LinkedList<>();
        for (Field field : getDeclaredAndInheritedMembers(clazz, Class::getDeclaredFields)) {
            Type type = field.getGenericType();
            List<Annotation> annotations = Arrays.asList(field.getAnnotations());
            typesWithAnnotations.add(new TypeWithAnnotations(type, annotations));
        }
        return typesWithAnnotations;
    }

    private static List<TypeWithAnnotations> getMethodParameterTypes(Class<?> clazz) {
        List<TypeWithAnnotations> typesWithAnnotations = new LinkedList<>();
        // For methods we will only examine setters and will only look at the annotations on the parameter, not the method itself
        for (Method method : getDeclaredAndInheritedMembers(clazz, Class::getDeclaredMethods)) {
            Type[] parameterTypes = method.getGenericParameterTypes();
            // Skip methods that don't look like setters
            if (parameterTypes.length != 1 || method.getReturnType() != void.class) {
                continue;
            }
            Type type = parameterTypes[0];
            List<Annotation> annotations = Arrays.asList(AnnotatedMethodService.findAllParamAnnotations(method)[0]);
            typesWithAnnotations.add(new TypeWithAnnotations(type, annotations));
        }
        return typesWithAnnotations;
    }

    private static List<TypeWithAnnotations> getConstructorParameterTypes(Class<?> clazz) {
        List<TypeWithAnnotations> typesWithAnnotations = new LinkedList<>();
        for (Constructor<?> constructor : getDeclaredAndInheritedMembers(clazz, Class::getDeclaredConstructors)) {
            Type[] parameterTypes = constructor.getGenericParameterTypes();
            Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
            for (int i = 0; i < parameterTypes.length; i++) {
                Type type = parameterTypes[i];
                List<Annotation> annotations = Arrays.asList(parameterAnnotations[i]);
                typesWithAnnotations.add(new TypeWithAnnotations(type, annotations));
            }
        }
        return typesWithAnnotations;
    }

    private static <T extends AccessibleObject> List<T> getDeclaredAndInheritedMembers(Class<?> clazz, Function<Class, T[]> getDeclaredMembers) {
        List<T> fields = new ArrayList<>();
        Class<?> inspectedClass = clazz;
        while (inspectedClass != null) {
            fields.addAll(Arrays.asList(getDeclaredMembers.apply(inspectedClass)));
            inspectedClass = inspectedClass.getSuperclass();
        }
        return fields;
    }
}
