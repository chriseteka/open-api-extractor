package com.chrisworks.ing.openapi.extractorutil.models;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Just so we support people with different interests, we want to ensure that we can transform
 * between versions
 */
public enum SwaggerFileVersion {

  V2_0("swagger", "2.0"),
  V3_0("openapi", "3.0.1"),
  ;

  private final String versionType;
  private final String versionNumber;

  public String getVersionType() {
    return versionType;
  }

  public String getVersionNumber() {
    return versionNumber;
  }

  public static List<String> typesAsList() {
    return Stream.of(values()).map(SwaggerFileVersion::getVersionType).toList();
  }

  /**
   * Method that allows the selection of version
   * @param version Input string which decides which version will be returned
   * @return {@link SwaggerFileVersion} based on the argument passed or defaults to {@link #V3_0}
   */
  public static SwaggerFileVersion decideVersion(String version) {

    //argument can be of the form: "3" || "2.0" || "swagger" || "openapi" || "3.0.1" || V2_0
    final Predicate<SwaggerFileVersion> decider = sfv ->
      sfv.versionNumber.equalsIgnoreCase(version) ||
          sfv.versionType.equalsIgnoreCase(version) ||
          sfv.name().equalsIgnoreCase(version) ||
          sfv.name().contains(version.replace(".0", "")) ||
          sfv.versionNumber.contains(version.replace(".0", ""));

    return Stream.of(values())
        .filter(decider)
        .findFirst()
        .orElse(SwaggerFileVersion.V3_0);
  }

  SwaggerFileVersion(String versionType, String versionNumber) {
    this.versionType    = versionType;
    this.versionNumber  = versionNumber;
  }
}
