package com.github.carlkuesters.swagger.swagger.reader;

import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;

class ResponseContainerUtil {

    static Property withContainer(Property property, String responseContainer) {
        if ("list".equalsIgnoreCase(responseContainer)) {
            return new ArrayProperty(property);
        } else if ("set".equalsIgnoreCase(responseContainer)) {
            return new ArrayProperty(property).uniqueItems();
        } else if ("map".equalsIgnoreCase(responseContainer)) {
            return new MapProperty(property);
        }
        return property;
    }
}
