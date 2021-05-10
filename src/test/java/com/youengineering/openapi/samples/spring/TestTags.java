package com.youengineering.openapi.samples.spring;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/tags")
@Tag(name = "myClassTag", description = "myClassTagDescription")
public class TestTags {

    @GetMapping("/noExplicitTagOnMethod")
    public void testNoExplicitTagOnMethod() {

    }

    @GetMapping("/sameTagOnMethodAsOnClass")
    @Tag(name = "myClassTag", description = "myClassTagDescription")
    public void testSameTagOnMethodAsOnClass() {

    }

    @GetMapping("/sameTagOnMethodAsOnClassPlusExplicitOne")
    @Tag(name = "myClassTag", description = "myClassTagDescription")
    @Tag(name = "myMethodTag", description = "myMethodTagDescription")
    public void testSameTagOnMethodAsOnClassPlusExplicitOne() {

    }
}
