package com.youengineering.openapi.samples.spring;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class TestResponseDeprecatedJava {
    @Deprecated
    private String text;
}
