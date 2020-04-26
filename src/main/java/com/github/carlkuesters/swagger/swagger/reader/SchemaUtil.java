package com.github.carlkuesters.swagger.swagger.reader;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.Schema;

class SchemaUtil {

    static Schema withContainer(Schema schema, String responseContainer) {
        if ("list".equalsIgnoreCase(responseContainer)) {
            // TODO: Check
            ArraySchema arraySchema = new ArraySchema();
            arraySchema.setItems(schema);
            return arraySchema;
        } else if ("set".equalsIgnoreCase(responseContainer)) {
            // TODO: Check
            ArraySchema arraySchema = new ArraySchema();
            arraySchema.setItems(schema);
            return arraySchema;
        } else if ("map".equalsIgnoreCase(responseContainer)) {
            // TODO: Check
            MapSchema mapSchema = new MapSchema();
            return mapSchema;
        }
        return schema;
    }
}
