package com.chrisworks.ing.openapi.extractorutil;

public class ExtractorUtil {

  public static void main(String[] args) {
    SwaggerProcessor.loadFile()
        .map(SwaggerProcessor::processFile)
        .ifPresent(SwaggerProcessor::writeFile);
  }

}
