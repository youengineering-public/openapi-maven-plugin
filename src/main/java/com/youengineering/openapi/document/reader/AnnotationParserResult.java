package com.youengineering.openapi.document.reader;

import io.swagger.v3.oas.models.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
@Getter
public class AnnotationParserResult<T> {

    private T result;
    public Map<String, Schema> referencedSchemas;

}
