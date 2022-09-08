package com.chrisworks.ing.openapi.extractorutil.models;

import com.chrisworks.ing.openapi.extractorutil.Constants;
import com.chrisworks.ing.openapi.extractorutil.SwaggerConverter;
import java.util.Map;

/**
 * Let's assume this is a simple description of the result we expect after parsing a file
 */
public record SwaggerFile(String data, Map<String, Object> inter, SwaggerFileType swaggerFileType) {
  public SwaggerFile decideVersion(SwaggerFileVersion version) {
    return SwaggerConverter.convert(this.inter, version, this.swaggerFileType);
  }
  public SwaggerFile decideVersion() {
    return decideVersion(SwaggerFileVersion.decideVersion(Constants.SWAGGER_FILE_OUTPUT_VERSION));
  }
  public SwaggerFile asV2() {
    return decideVersion(SwaggerFileVersion.V2_0);
  }

  public SwaggerFile asV3() {
    return decideVersion(SwaggerFileVersion.V3_0);
  }
}
