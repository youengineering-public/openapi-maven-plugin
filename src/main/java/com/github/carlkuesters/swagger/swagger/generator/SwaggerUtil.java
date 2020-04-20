package com.github.carlkuesters.swagger.swagger.generator;

import com.github.carlkuesters.swagger.GenerateException;
import io.swagger.models.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.TreeMap;

class SwaggerUtil {

    private static final String[] HTTP_METHODS = { "Get", "Delete", "Post", "Put", "Options", "Patch" };

    static void sort(Swagger swagger) throws GenerateException {
        if (swagger.getTags() != null) {
            swagger.getTags().sort(Comparator.comparing(a -> a.toString().toLowerCase()));
        }
        if (swagger.getPaths() != null) {
            swagger.paths(new TreeMap<>(swagger.getPaths()));
            for (Path path : swagger.getPaths().values()) {
                for (String httpMethod : HTTP_METHODS) {
                    sortOperation(path, httpMethod);
                }
            }
        }
        if (swagger.getDefinitions() != null) {
            swagger.setDefinitions(new TreeMap<>(swagger.getDefinitions()));
        }
    }

    private static void sortOperation(Path path, String httpMethod) throws GenerateException {
        try {
            Method operationGetter = Path.class.getDeclaredMethod("get" + httpMethod);
            Operation operation = (Operation) operationGetter.invoke(path);
            if (operation == null) {
                return;
            }
            operation.setResponses(new TreeMap<>(operation.getResponses()));
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            throw new GenerateException(ex);
        }
    }
}
