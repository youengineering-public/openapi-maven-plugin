package com.youengineering.openapi.document.reader;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Type;

@AllArgsConstructor
@Getter
public class ResolvedArray {

    private Type itemsType;
    private boolean uniqueItems;

}
