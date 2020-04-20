package com.carlkuesters.swagger.mavenplugin.swagger.generator;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.jackson.ModelResolver;
import io.swagger.models.Model;
import io.swagger.models.properties.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ModelModifier extends ModelResolver {

    private static Logger LOGGER = LoggerFactory.getLogger(ModelModifier.class);

    private Map<JavaType, JavaType> modelSubstitutes = new HashMap<>();

    ModelModifier(ObjectMapper mapper) {
        super(mapper);
    }

    void addModelSubstitute(String fromClassName, String toClassName) {
        JavaType fromType = constructType(fromClassName);
        JavaType toType = constructType(toClassName);
        if ((fromType != null) && (toType != null)) {
            modelSubstitutes.put(fromType, toType);
        }
    }

    private JavaType constructType(String className) {
        try {
            return _mapper.constructType(Class.forName(className));
        } catch (ClassNotFoundException e) {
            LOGGER.warn(String.format("Problem with loading class: %s. Mapping from it will be ignored.", className));
            return null;
        }
    }

    @Override
    public Property resolveProperty(Type type, ModelConverterContext context, Annotation[] annotations, Iterator<ModelConverter> chain) {
    	// For method parameter types we get Type, but JavaType is needed
    	JavaType javaType = toJavaType(type);
        if (modelSubstitutes.containsKey(javaType)) {
            return super.resolveProperty(modelSubstitutes.get(javaType), context, annotations, chain);
        } else if (chain.hasNext()) {
            return chain.next().resolveProperty(type, context, annotations, chain);
        } else {
            return super.resolveProperty(type, context, annotations, chain);
        }

    }

    @Override
    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
    	// For method parameter types we get Type, but JavaType is needed
    	JavaType javaType = toJavaType(type);
        if (modelSubstitutes.containsKey(javaType)) {
            return super.resolve(modelSubstitutes.get(javaType), context, chain);
        } else {
            return super.resolve(type, context, chain);
        }
    }

    private JavaType toJavaType(Type type) {
    	if (type instanceof JavaType) {
    		return (JavaType) type;
    	} else {
    		return _mapper.constructType(type);
    	}
	}
}
