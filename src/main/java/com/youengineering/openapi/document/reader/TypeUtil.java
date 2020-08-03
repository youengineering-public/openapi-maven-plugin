package com.youengineering.openapi.document.reader;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class TypeUtil {

    public static Schema getPrimitiveSchema(Type type, Log log) {
        Schema primitiveTypeSchema = PrimitiveType.createProperty(type);
        if (primitiveTypeSchema != null) {
            return primitiveTypeSchema;
        } else {
            log.warn(String.format("Non-primitive type %s used as primitive parameter, assuming String deserialization", type));
            return new StringSchema();
        }
    }

    public static ResolvedSchema getResolvedSchema(Type type) {
        Schema primitiveTypeSchema = PrimitiveType.createProperty(type);
        if (primitiveTypeSchema != null) {
            ResolvedSchema resolvedSchema = new ResolvedSchema();
            resolvedSchema.schema = primitiveTypeSchema;
            resolvedSchema.referencedSchemas = new HashMap<>();
            return resolvedSchema;
        } else {
            ResolvedSchema resolvedSchema = ModelConverters.getInstance().readAllAsResolvedSchema(type);
            // For arrays, the referenced schemas are resolved, but the "parent" array schema isn't populated, so we recursively fill it
            ResolvedArray resolvedArray = resolveArray(type);
            if (resolvedArray != null) {
                ArraySchema arraySchema = new ArraySchema();
                arraySchema.setItems(getResolvedSchema(resolvedArray.getItemsType()).schema);
                arraySchema.setUniqueItems(resolvedArray.isUniqueItems());
                resolvedSchema.schema = arraySchema;
            }
            return resolvedSchema;
        }
    }

    private static ResolvedArray resolveArray(Type type) {
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return new ResolvedArray(clazz.getComponentType(), false);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class) {
                Class<?> rawClazz = (Class<?>) rawType;
                if (List.class.isAssignableFrom(rawClazz)) {
                    return new ResolvedArray(parameterizedType.getActualTypeArguments()[0], false);
                } else if (Set.class.isAssignableFrom(rawClazz)) {
                    return new ResolvedArray(parameterizedType.getActualTypeArguments()[0], true);
                }
            }
        }
        return null;
    }
}
