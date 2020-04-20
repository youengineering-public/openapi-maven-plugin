package com.github.carlkuesters.swagger.swagger.reader.spring;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;

class SpringUtils {

    static String[] getApiClassRequestMapping(Class<?> controllerClass) {
		String[] requestMappings = new String[0];

		RequestMapping requestMapping = AnnotationUtils.findAnnotation(controllerClass, RequestMapping.class);
		if (requestMapping != null) {
			requestMappings = requestMapping.value();
		}

		if (requestMappings.length == 0) {
			requestMappings = new String[] { "" };
		}

		return requestMappings;
    }
}
