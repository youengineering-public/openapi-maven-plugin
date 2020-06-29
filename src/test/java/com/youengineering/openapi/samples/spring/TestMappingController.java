package com.youengineering.openapi.samples.spring;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/mapping")
public class TestMappingController {

    @GetMapping("/get")
    public String testGet(@RequestParam String myAutoNamedParameter) {
        return myAutoNamedParameter;
    }

    @PostMapping("/post")
    public TestResponse testPost(@RequestBody TestRequestBody testRequestBody) {
        return new TestResponse(testRequestBody.getNumber() + "_" + testRequestBody.getText());
    }
}
