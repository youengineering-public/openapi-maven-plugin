package com.github.carlkuesters.swagger.swagger.reader.spring;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;

class SpringPathUtil {

	private static final String[] DEFAULT_PATHS = new String[]{""};

	static String[] getControllerPaths(Class<?> controllerClass) {
		RequestMapping requestMapping = AnnotationUtils.findAnnotation(controllerClass, RequestMapping.class);
		return ((requestMapping != null) ? getPaths(requestMapping) : DEFAULT_PATHS);
	}

	static String[] getPaths(RequestMapping requestMapping) {
		String[] paths = new String[0];
		if (requestMapping != null) {
			paths = requestMapping.value();
		}
		if (paths.length == 0) {
			paths = DEFAULT_PATHS;
		}
		return paths;
	}
}
