package com.chrisworks.ing.openapi.extractorutil.models;

import java.util.Arrays;

/**
 * The Swagger File can come in either of these extensions, and we need this information for the:
 * determination of a traversal strategy, determination of the output format
 */
public enum SwaggerFileType {

  JSON(".json"),
  YAML(".yaml", ".yml");

  private final String[] extension;

  SwaggerFileType(String... extension) {
    this.extension = extension;
  }

  public static SwaggerFileType fromFileFullName(String fileName) {
    return Arrays.stream(values())
        .filter(sft -> Arrays.stream(sft.extension)
            .anyMatch(ext -> fileName.toLowerCase().endsWith(ext)))
        .findAny()
        .orElseThrow(() -> new AppException(
            "Unsupported file type passed, ensure to supply either JSON or YAML file"));
  }

  public String getExtension() {
    return extension[0];
  }
}
