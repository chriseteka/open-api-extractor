package com.chrisworks.ing.openapi.extractorutil;

import com.chrisworks.ing.openapi.extractorutil.models.KeyToPathMapper;
import java.util.List;

public final class Constants {

  private Constants() {}

  private static final String INFO = "info";
  private static final String HOST = "host";
  private static final String PATHS = "paths";
  private static final String SCHEMES = "schemes";
  public static final String SWAGGER = "swagger";
  public static final String OPEN_API = "openapi";
  private static final String CONSUMES = "consumes";
  private static final String PRODUCES = "produces";
  private static final String BASE_PATH = "basePath";
  private static final String COMPONENTS = "components";
  private static final String DEFINITIONS = "definitions";

  public static final class V2 {
    private V2() {}

    public static final List<String> CONST_KEYS = List.of(SWAGGER, INFO, CONSUMES, PRODUCES, SCHEMES, HOST, BASE_PATH);
    public static final List<String> RES_ORDER = List.of(SWAGGER, INFO, CONSUMES, PRODUCES, SCHEMES, HOST, BASE_PATH, PATHS, DEFINITIONS);
    public static final List<KeyToPathMapper> fromV3  = List.of(
        new KeyToPathMapper(SWAGGER, OPEN_API),
        new KeyToPathMapper(INFO, INFO),
        new KeyToPathMapper(PATHS, PATHS),
        new KeyToPathMapper(DEFINITIONS, COMPONENTS)
    );
  }

  public static final class V3 {
    private V3() {}

    public static final List<String> CONST_KEYS = List.of(OPEN_API, INFO);
    public static final List<String> RES_ORDER = List.of(OPEN_API, INFO, PATHS, COMPONENTS);
    public static final List<KeyToPathMapper> fromV2  = List.of(
        new KeyToPathMapper(OPEN_API, SWAGGER),
        new KeyToPathMapper(INFO, INFO),
        new KeyToPathMapper(PATHS, PATHS),
        new KeyToPathMapper(COMPONENTS, DEFINITIONS)
    );
  }
  public static final String RESULT_OUTPUT_PATH = "../openapi";

  //For now you are allowed to change the list and the file name and the version we want to output to
  public static final String SWAGGER_FILE_OUTPUT_VERSION = "2";
  public static final String SWAGGER_FILE_NAME = "v3.yaml";
  public static final List<String> ENDPOINTS_OF_INTEREST = List.of("/v4/involved-parties/search/agreementIdentifiers");

}
