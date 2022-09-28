package com.chrisworks.ing.openapi.extractorutil;

public class ExtractorUtil {

  public static void main(String[] args) {
    SwaggerProcessor.loadFile()
        .map(SwaggerProcessor::processFile)
        .map(SwaggerProcessor::convertToV3)
        .ifPresent(SwaggerProcessor::writeFile);
  }

}
