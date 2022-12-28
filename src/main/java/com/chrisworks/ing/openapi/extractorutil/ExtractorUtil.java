package com.chrisworks.ing.openapi.extractorutil;

import com.chrisworks.ing.openapi.extractorutil.SwaggerProcessor.ProcessorConfig;

public class ExtractorUtil {

  public static void main(String[] args) {
    SwaggerProcessor.loadFile()
        .map(SwaggerProcessor::processFile)
        .ifPresent(SwaggerProcessor::writeFile);
  }

}
