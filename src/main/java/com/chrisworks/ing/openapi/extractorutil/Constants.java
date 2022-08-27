package com.chrisworks.ing.openapi.extractorutil;

import java.util.List;

public final class Constants {

  private Constants() {}

  public static final List<String> RES_ORDER = List.of("swagger", "info", "consumes", "produces",
      "schemes", "host", "basePath", "paths", "definitions");
  public static final List<String> CONST_KEYS = List.of("swagger", "info", "consumes", "produces",
      "schemes", "host", "basePath");
  public static final String RESULT_OUTPUT_PATH = "../openapi";

  //For now you are allowed to change the list and the file name
  public static final String SWAGGER_FILE_NAME = "sample.yaml";
  public static final List<String> ENDPOINTS_OF_INTEREST = List.of("/v4/involved-parties/search/agreementIdentifiers");

}
