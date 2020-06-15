package com.youengineering.openapi.document.reader;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

@AllArgsConstructor
@Getter
public class TypeWithAnnotations {

    private Type type;
    private List<Annotation> annotations;

}
