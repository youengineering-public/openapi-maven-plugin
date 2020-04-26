package com.github.carlkuesters.swagger.swagger.reader.jaxrs;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.carlkuesters.swagger.swagger.reader.TypeUtil;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import io.swagger.v3.jaxrs2.ResolvedParameter;
import io.swagger.v3.jaxrs2.ext.AbstractOpenAPIExtension;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.CookieParameter;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import org.apache.maven.plugin.logging.Log;

import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class JaxrsParameterExtension extends AbstractOpenAPIExtension {

    JaxrsParameterExtension(Log log) {
        this.log = log;
    }
    private Log log;

    @Override
    public ResolvedParameter extractParameters(List<Annotation> annotations, Type type, Set<Type> typesToSkip, Components components, Consumes classConsumes, Consumes methodConsumes, boolean includeRequestBody, JsonView jsonViewAnnotation, Iterator<OpenAPIExtension> chain) {
        if (shouldIgnoreType(type, typesToSkip)) {
            return new ResolvedParameter();
        }
        ClassToInstanceMap<Annotation> annotationInstances = toInstanceMap(annotations);
        ResolvedParameter resolvedParameter = extractParametersFromAnnotation(type, annotationInstances);
        if (resolvedParameter.parameters.size() > 0) {
            return resolvedParameter;
        }
        return super.extractParameters(annotations, type, typesToSkip, components, classConsumes, methodConsumes, includeRequestBody, jsonViewAnnotation, chain);
    }

    private ClassToInstanceMap<Annotation> toInstanceMap(List<? extends Annotation> annotations) {
        ClassToInstanceMap<Annotation> annotationMap = MutableClassToInstanceMap.create();
        for (Annotation annotation : annotations) {
            if (annotation == null) {
                continue;
            }
            annotationMap.put(annotation.annotationType(), annotation);
        }
        return annotationMap;
    }

    private ResolvedParameter extractParametersFromAnnotation(Type type, ClassToInstanceMap<Annotation> annotations) {
        String defaultValue = null;
        if (annotations.containsKey(DefaultValue.class)) {
            DefaultValue defaultValueAnnotation = annotations.getInstance(DefaultValue.class);
            defaultValue = defaultValueAnnotation.value();
        }

        ResolvedParameter resolvedParameter = new ResolvedParameter();
        if (annotations.containsKey(QueryParam.class)) {
            QueryParam param = annotations.getInstance(QueryParam.class);
            QueryParameter queryParameter = extractQueryParam(type, defaultValue, param);
            resolvedParameter.parameters.add(queryParameter);
        } else if (annotations.containsKey(PathParam.class)) {
            PathParam param = annotations.getInstance(PathParam.class);
            PathParameter pathParameter = extractPathParam(type, defaultValue, param);
            resolvedParameter.parameters.add(pathParameter);
        } else if (annotations.containsKey(HeaderParam.class)) {
            HeaderParam param = annotations.getInstance(HeaderParam.class);
            HeaderParameter headerParameter = extractHeaderParam(type, defaultValue, param);
            resolvedParameter.parameters.add(headerParameter);
        } else if (annotations.containsKey(CookieParam.class)) {
            CookieParam param = annotations.getInstance(CookieParam.class);
            CookieParameter cookieParameter = extractCookieParameter(type, defaultValue, param);
            resolvedParameter.parameters.add(cookieParameter);
        }

        return resolvedParameter;
    }

    private QueryParameter extractQueryParam(Type type, String defaultValue, QueryParam param) {
        QueryParameter queryParameter = new QueryParameter();
        queryParameter.setName(param.value());
        queryParameter.setSchema(createPrimitiveSchema(type, defaultValue));
        return queryParameter;
    }

    private PathParameter extractPathParam(Type type, String defaultValue, PathParam param) {
        PathParameter pathParameter = new PathParameter();
        pathParameter.setName(param.value());
        pathParameter.setSchema(createPrimitiveSchema(type, defaultValue));
        return pathParameter;
    }

    private HeaderParameter extractHeaderParam(Type type, String defaultValue, HeaderParam param) {
        HeaderParameter headerParameter = new HeaderParameter();
        headerParameter.setName(param.value());
        headerParameter.setSchema(createPrimitiveSchema(type, defaultValue));
        return headerParameter;
    }

    private CookieParameter extractCookieParameter(Type type, String defaultValue, CookieParam param) {
        CookieParameter cookieParameter = new CookieParameter();
        cookieParameter.setName(param.value());
        cookieParameter.setSchema(createPrimitiveSchema(type, defaultValue));
        return cookieParameter;
    }

    private Schema createPrimitiveSchema(Type type, String defaultValue) {
        Schema primitiveSchema = TypeUtil.getPrimitiveSchema(type, log);
        primitiveSchema.setDefault(defaultValue);
        return primitiveSchema;
    }
}
