package com.github.carlkuesters.openapi.document.reader.jaxrs;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.carlkuesters.openapi.document.reader.AbstractReaderOpenAPIExtension;
import com.github.carlkuesters.openapi.document.reader.TypeExtractor;
import com.github.carlkuesters.openapi.document.reader.TypeWithAnnotations;
import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.core.header.FormDataContentDisposition;
import io.swagger.v3.jaxrs2.ResolvedParameter;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
import io.swagger.v3.oas.models.Components;
import org.apache.commons.lang3.reflect.TypeUtils;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This extension extracts the parameters inside a {@code @BeanParam} by
 * expanding the target bean type's fields/methods/constructor parameters and
 * recursively feeding them back through the {@link JaxrsReader}.
 */
public class BeanParamInjectExtension extends AbstractReaderOpenAPIExtension {

    @Override
    public ResolvedParameter extractParameters(List<Annotation> annotations, Type type, Set<Type> typesToSkip, Components components, Consumes classConsumes, Consumes methodConsumes, boolean includeRequestBody, JsonView jsonViewAnnotation, Iterator<OpenAPIExtension> chain) {
        Class<?> clazz = TypeUtils.getRawType(type, type);
        if (shouldIgnoreClass(clazz)) {
            return new ResolvedParameter();
        }
        for (Annotation annotation : annotations) {
            if ((annotation instanceof BeanParam) || (annotation instanceof InjectParam)) {
                return extractTypes(clazz, typesToSkip, components, jsonViewAnnotation);
            }
        }
        return super.extractParameters(annotations, type, typesToSkip, components, classConsumes, methodConsumes, includeRequestBody, jsonViewAnnotation, chain);
    }

    public ResolvedParameter extractTypes(Class<?> clazz, Set<Type> typesToSkip, Components components, JsonView jsonViewAnnotation) {
        ResolvedParameter resolvedParameter = new ResolvedParameter();

        List<TypeWithAnnotations> typesWithAnnotations = TypeExtractor.extractTypes(clazz);
        for (TypeWithAnnotations typeWithAnnotations : typesWithAnnotations) {
            Type type = typeWithAnnotations.getType();
            List<Annotation> annotations = typeWithAnnotations.getAnnotations();

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

            ResolvedParameter additionalResolvedParameter = reader.getParameters(type, annotations, recurseTypesToSkip, components, new String[0], new String[0], jsonViewAnnotation);
            resolvedParameter.parameters.addAll(additionalResolvedParameter.parameters);
            resolvedParameter.formParameters.addAll(additionalResolvedParameter.formParameters);
        }

        return resolvedParameter;
    }

    @Override
    public boolean shouldIgnoreClass(Class<?> clazz) {
        return (clazz == FormDataContentDisposition.class);
    }
}
