package com.github.carlkuesters.swagger.mavenplugin.swagger.reader.jaxrs;

import com.github.carlkuesters.swagger.mavenplugin.swagger.reader.TypeExtractor;
import com.github.carlkuesters.swagger.mavenplugin.swagger.reader.TypeWithAnnotations;
import com.google.common.collect.Lists;
import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.core.header.FormDataContentDisposition;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.models.parameters.Parameter;
import org.apache.commons.lang3.reflect.TypeUtils;

import javax.ws.rs.BeanParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

/**
 * This extension extracts the parameters inside a {@code @BeanParam} by
 * expanding the target bean type's fields/methods/constructor parameters and
 * recursively feeding them back through the {@link JaxrsReader}.
 */
public class BeanParamInjectExtension extends AbstractReaderSwaggerExtension {

    @Override
    public List<Parameter> extractParameters(List<Annotation> annotations, Type type, Set<Type> typesToSkip, Iterator<SwaggerExtension> chain) {
        Class<?> clazz = TypeUtils.getRawType(type, type);
        if (shouldIgnoreClass(clazz)) {
            return new LinkedList<>();
        }
        for (Annotation annotation : annotations) {
            if ((annotation instanceof BeanParam) || (annotation instanceof InjectParam)) {
                return extractTypes(clazz, typesToSkip, Lists.newArrayList());
            }
        }
        return super.extractParameters(annotations, type, typesToSkip, chain);
    }

    private List<Parameter> extractTypes(Class<?> clazz, Set<Type> typesToSkip, List<Annotation> additionalAnnotations) {
        List<Parameter> parameters = new ArrayList<>();

        List<TypeWithAnnotations> typesWithAnnotations = TypeExtractor.extractTypes(clazz);
        for (TypeWithAnnotations typeWithAnnotations : typesWithAnnotations) {
            Type type = typeWithAnnotations.getType();

            List<Annotation> annotations = new LinkedList<>(additionalAnnotations);
            annotations.addAll(typeWithAnnotations.getAnnotations());

            /*
             * Skip the type of the bean itself when recursing into its members
             * in order to avoid a cycle (stack overflow), as crazy as that user
             * code would have to be.
             *
             * There are no tests to prove this works because the test bean
             * classes are shared with SwaggerReaderTest and Swagger's own logic
             * doesn't prevent this problem.
             */
            Set<Type> recurseTypesToSkip = new HashSet<>(typesToSkip);
            recurseTypesToSkip.add(clazz);

            parameters.addAll(reader.getParameters(swagger, type, annotations, recurseTypesToSkip));
        }

        return parameters;
    }

    @Override
    public boolean shouldIgnoreClass(Class<?> clazz) {
        return (clazz == FormDataContentDisposition.class);
    }
}
