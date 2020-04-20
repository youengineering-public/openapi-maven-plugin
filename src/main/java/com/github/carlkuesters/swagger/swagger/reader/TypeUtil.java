package com.github.carlkuesters.swagger.swagger.reader;

import io.swagger.converter.ModelConverters;
import io.swagger.models.properties.Property;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Type;

public class TypeUtil {

    private static final String[] PRIMITIVE_TYPES = new String[]{ "integer", "string", "number", "boolean", "array", "file" };

    public static boolean isPrimitive(Type type) {
        Property property = ModelConverters.getInstance().readAsProperty(type);
        if (property != null) {
            return ArrayUtils.contains(PRIMITIVE_TYPES, property.getType());
        }
        return false;
    }
}
