package com.chrisworks.ing.openapi.extractorutil.models;

/**
 * Let's assume this is a simple description of the result we expect after parsing a file
 */
public record SwaggerFile(String data, SwaggerFileType swaggerFileType) {}
