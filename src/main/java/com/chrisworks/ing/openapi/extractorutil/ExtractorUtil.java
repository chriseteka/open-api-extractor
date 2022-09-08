package com.chrisworks.ing.openapi.extractorutil;

import com.chrisworks.ing.openapi.extractorutil.models.SwaggerFile;

public class ExtractorUtil {

  public static void main(String[] args) {

    SwaggerProcessor
        .loadFile()
        .map(SwaggerProcessor::processFile)
        .map(SwaggerFile::decideVersion)
        .ifPresent(SwaggerProcessor::writeFile);
  }

}
