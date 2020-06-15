package com.youengineering.openapi.document.reader;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Type;
import java.util.HashMap;

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
            return ModelConverters.getInstance().readAllAsResolvedSchema(type);
        }
    }
}
