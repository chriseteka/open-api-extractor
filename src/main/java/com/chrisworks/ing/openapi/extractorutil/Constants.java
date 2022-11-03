package com.chrisworks.ing.openapi.extractorutil;

import java.util.List;

public final class Constants {

  private Constants() {}

  public static final List<String> RES_ORDER = List.of("swagger", "info", "consumes", "produces",
      "schemes", "host", "basePath", "paths", "definitions");
  public static final List<String> CONST_KEYS = List.of("swagger", "info", "consumes", "produces",
      "schemes", "host", "basePath");
  public static final String RESULT_OUTPUT_PATH = "gen";

  //For now you are allowed to change the list and the file name (Read all these from class path)
  public static final String SWAGGER_FILE_NAME = "Involved-Party-API-24.1.3.yaml";
  public static final List<String> ENDPOINTS_OF_INTEREST = List.of("/v5/involved-parties/{uuid}/close");

}
