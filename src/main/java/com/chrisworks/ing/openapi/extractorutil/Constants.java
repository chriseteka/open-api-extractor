package com.chrisworks.ing.openapi.extractorutil;

import java.util.List;

public final class Constants {

  private Constants() {}

  public static final List<String> RES_ORDER = List.of("swagger", "info", "consumes", "produces",
      "schemes", "host", "basePath", "paths", "definitions", "parameters", "responses");
  public static final List<String> CONST_KEYS = List.of("swagger", "info", "consumes", "produces",
      "schemes", "host", "basePath");
  public static final String RESULT_OUTPUT_PATH = "gen";

  //For now you are allowed to change the list and the file name (Read all these from class path)
  public static final String SWAGGER_FILE_NAME = "ProductAgreements-API-14.1.0.yaml";
  public static final List<String> ENDPOINTS_OF_INTEREST = List.of("/v6/agreements/products/lookup");

}
