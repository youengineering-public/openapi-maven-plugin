package com.youengineering.openapi.samples.spring;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/echo")
public class TestController {

    @RequestMapping(value = "/pathVariableExpectParameterName/{parameterName}", method = RequestMethod.GET)
    @Operation
    public String pathVariableExpectParameterName(@PathVariable String parameterName) {
        return parameterName;
    }

    @RequestMapping(value = "/pathVariableExpectVariableName/{pathVariableName}", method = RequestMethod.GET)
    @Operation
    public String pathVariableExpectVariableName(@PathVariable(name = "pathVariableName") String parameterName) {
        return parameterName;
    }

    @RequestMapping(value = "/pathVariableExpectVariableValue/{pathVariableValue}", method = RequestMethod.GET)
    @Operation
    public String pathVariableExpectVariableValue(@PathVariable(value = "pathVariableValue") String parameterName) {
        return parameterName;
    }

    @RequestMapping(value = "/requestParamExpectParameterName", method = RequestMethod.GET)
    @Operation
    public String requestParamExpectParameterName(@RequestParam String parameterName) {
        return parameterName;
    }

    @RequestMapping(value = "/requestParamExpectParamName", method = RequestMethod.GET)
    @Operation
    public String requestParamExpectParamName(@RequestParam(name = "requestParamName") String parameterName) {
        return parameterName;
    }

    @RequestMapping(value = "/requestParamExpectParamValue", method = RequestMethod.GET)
    @Operation
    public String requestParamExpectParamValue(@RequestParam(value = "requestParamValue") String parameterName) {
        return parameterName;
    }

    @RequestMapping(value = "/requestHeaderExpectParameterName", method = RequestMethod.GET)
    @Operation
    public String requestHeaderExpectParameterName(@RequestHeader String parameterName) {
        return parameterName;
    }

    @RequestMapping(value = "/requestHeaderExpectHeaderName", method = RequestMethod.GET)
    @Operation
    public String requestHeaderExpectHeaderName(@RequestHeader(name = "requestHeaderName") String parameterName) {
        return parameterName;
    }

    @RequestMapping(value = "/requestHeaderExpectHeaderValue", method = RequestMethod.GET)
    @Operation
    public String requestHeaderExpectHeaderValue(@RequestHeader(value = "requestHeaderValue") String parameterName) {
        return parameterName;
    }

    @RequestMapping(value = "/cookieValueExpectParameterName", method = RequestMethod.GET)
    @Operation
    public String cookieValueExpectParameterName(@CookieValue String parameterName) {
        return parameterName;
    }

    @RequestMapping(value = "/cookieValueExpectCookieName", method = RequestMethod.GET)
    @Operation
    public String cookieValueExpectCookieName(@CookieValue(name = "cookieValueName") String parameterName) {
        return parameterName;
    }

    @RequestMapping(value = "/cookieValueExpectCookieValue", method = RequestMethod.GET)
    @Operation
    public String cookieValueExpectCookieValue(@CookieValue(value = "cookieValueValue") String parameterName) {
        return parameterName;
    }
}
