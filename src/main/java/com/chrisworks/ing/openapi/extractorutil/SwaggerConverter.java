package com.chrisworks.ing.openapi.extractorutil;

import com.chrisworks.ing.openapi.extractorutil.Constants.V2;
import com.chrisworks.ing.openapi.extractorutil.Constants.V3;
import com.chrisworks.ing.openapi.extractorutil.models.KeyToPathMapper;
import com.chrisworks.ing.openapi.extractorutil.models.SwaggerFile;
import com.chrisworks.ing.openapi.extractorutil.models.SwaggerFileType;
import com.chrisworks.ing.openapi.extractorutil.models.SwaggerFileVersion;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SwaggerConverter {

  private SwaggerConverter() {}

  public static SwaggerFile convert(Map<String, Object> input, SwaggerFileVersion toVersion,
      SwaggerFileType resultAs) {

    final Function<Map<String, Object>, String> rawToData = raw -> switch (resultAs) {
      case JSON -> SwaggerProcessor.gson.toJson(raw);
      case YAML -> SwaggerProcessor.yaml.dump(raw);
    };

    final boolean isSameVersion = input.keySet()
        .stream()
        .filter(SwaggerFileVersion.typesAsList()::contains)
        .allMatch(toVersion.getVersionType()::equalsIgnoreCase);

    if (isSameVersion)
      return new SwaggerFile(rawToData.apply(input), input, resultAs);

    final Map<String, Object> raw  = switch (toVersion) {
      case V2_0 -> {
        final Map<String, Object> transform = doConversion(input, V2.fromV3);
        transform.replace(Constants.SWAGGER, SwaggerFileVersion.V2_0.getVersionNumber());
        yield SwaggerProcessor.sorter.apply(transform, V2.RES_ORDER);
      }
      case V3_0 -> {
        final Map<String, Object> transform = doConversion(input, V3.fromV2);
        transform.replace(Constants.OPEN_API, SwaggerFileVersion.V3_0.getVersionNumber());
        yield  SwaggerProcessor.sorter.apply(transform, V3.RES_ORDER);
      }
    };

    return new SwaggerFile(rawToData.apply(raw), raw, resultAs);
  }

  private static Map<String, Object> doConversion(Map<String, Object> data, List<KeyToPathMapper> mapper) {

    final Map<String, String[]> keyToPath = mapper
        .stream()
        .collect(Collectors.toMap(KeyToPathMapper::key, KeyToPathMapper::path));
    return keyToPath
        .keySet()
        .stream()
        .collect(Collectors.toMap(key -> key, key -> getValueByInPath(data, keyToPath.get(key))));
  }

  @SuppressWarnings("unchecked")
  private static Object getValueByInPath(Map<String, Object> data, String[] path) {
    Object res = null;
    var temp = data;
    for (String s : path) {
      res = temp.getOrDefault(s, Collections.<String, Object>emptyMap());
      if (res instanceof Map<?,?>)
        temp = (Map<String, Object>) res;
    }
    return res;
  }

}
