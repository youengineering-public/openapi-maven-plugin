package com.github.carlkuesters.swagger.config;

import lombok.Getter;

import java.util.List;

@Getter
public class OutputConfig {

    private String directory;
    private String fileName;
    private List<OutputFormat> formats;

}

