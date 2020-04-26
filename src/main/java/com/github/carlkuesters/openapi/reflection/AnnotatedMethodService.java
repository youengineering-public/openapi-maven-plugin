package com.github.carlkuesters.openapi.reflection;

import io.swagger.v3.core.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnnotatedMethodService {

    public static Annotation[][] findAllParamAnnotations(Method method) {
        Annotation[][] paramAnnotation = method.getParameterAnnotations();
        Method overriddenMethod = ReflectionUtils.getOverriddenMethod(method);
        while(overriddenMethod != null) {
            paramAnnotation = merge(overriddenMethod.getParameterAnnotations(), paramAnnotation);
            overriddenMethod = ReflectionUtils.getOverriddenMethod(overriddenMethod);
        }
        return paramAnnotation;
    }

    private static Annotation[][] merge(Annotation[][] overriddenMethodParamAnnotation, Annotation[][] currentParamAnnotations) {
        Annotation[][] mergedAnnotations = new Annotation[overriddenMethodParamAnnotation.length][];
        for (int i = 0; i < overriddenMethodParamAnnotation.length; i++) {
            mergedAnnotations[i] = merge(overriddenMethodParamAnnotation[i], currentParamAnnotations[i]);
        }
        return mergedAnnotations;
    }

    private static Annotation[] merge(Annotation[] annotations1, Annotation[] annotations2) {
        List<Annotation> mergedAnnotations = new ArrayList<>();
        mergedAnnotations.addAll(Arrays.asList(annotations1));
        mergedAnnotations.addAll(Arrays.asList(annotations2));
        return mergedAnnotations.toArray(new Annotation[0]);
    }
}
