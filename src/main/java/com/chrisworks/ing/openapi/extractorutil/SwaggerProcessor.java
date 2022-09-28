package com.chrisworks.ing.openapi.extractorutil;

import static com.chrisworks.ing.openapi.extractorutil.Constants.CONST_KEYS;
import static com.chrisworks.ing.openapi.extractorutil.Constants.ENDPOINTS_OF_INTEREST;
import static com.chrisworks.ing.openapi.extractorutil.Constants.RES_ORDER;
import static com.chrisworks.ing.openapi.extractorutil.Constants.SWAGGER_FILE_NAME;

import com.chrisworks.ing.openapi.extractorutil.models.AppException;
import com.chrisworks.ing.openapi.extractorutil.models.SwaggerFile;
import com.chrisworks.ing.openapi.extractorutil.models.SwaggerFileType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;

public final class SwaggerProcessor {

  private SwaggerProcessor() {
  }

  private static final Gson gson = new GsonBuilder().registerTypeAdapter(OffsetDateTime.class,
      (JsonDeserializer<OffsetDateTime>) (json, typeOfT, context) -> OffsetDateTime.parse(json.getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-ddTHH:mm:ss.SSSZ"))).create();
  private static final Yaml yaml = new Yaml();
  private static final Type type = new TypeToken<HashMap<String, Object>>() {
  }.getType();

  /**
   * Reads file from the path specified in {@linkplain Constants#SWAGGER_FILE_NAME}
   *
   * @return an optional of {@link SwaggerFile}
   */
  public static Optional<InputStream> loadFile() {
    return Optional
        .ofNullable(SwaggerProcessor.class.getClassLoader()
            .getResourceAsStream(SWAGGER_FILE_NAME));
  }

  /**
   * Processes the swagger file, basically does a filter and takes out the endpoint of interest
   * {@linkplain Constants#ENDPOINTS_OF_INTEREST}, together with every definition it needs to work
   * properly.
   *
   * @param swaggerFileInputStream This is the data structure which represents the file to be
   *                               processed
   * @return {@link SwaggerFile}.
   */
  public static SwaggerFile processFile(InputStream swaggerFileInputStream) {

    final SwaggerFileType swaggerFileType = SwaggerFileType.fromFileFullName(SWAGGER_FILE_NAME);

    return new SwaggerFile(
        switch (swaggerFileType) {
          case JSON -> processAsJSON(swaggerFileInputStream);
          case YAML -> processAsYAML(swaggerFileInputStream);
        },
        swaggerFileType
    );
  }

  private static String processAsJSON(InputStream inputStream) {

    try {

      final String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      final Map<String, Object> input = gson.fromJson(jsonString, type);

      return gson.toJson(traverseData(input), type);

    } catch (IOException e) {
      e.printStackTrace();
      throw new AppException("Unable to parse json file");
    }
  }

  private static String processAsYAML(InputStream inputStream) {

    final Map<String, Object> input = yaml.load(inputStream);

    return yaml.dump(traverseData(input));
  }

  /**
   * Both YAML files and JSON files end up producing the same DS, which we can feed here and work
   * on
   *
   * @param input This is a map we want to traverse
   * @return Desired map containing only APIs and their important definitions
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> traverseData(Map<String, Object> input) {

    final String PATHS = "paths";
    final String DEFINITIONS = "definitions";
    final Map<String, Object> output = CONST_KEYS
        .stream()
        .collect(Collectors.toMap(key -> key, input::get));
    final Map<String, Object> paths = (Map<String, Object>) input.get(PATHS);
    final Map<String, Object> definitions = (Map<String, Object>) input.get(DEFINITIONS);
    final Map<String, Object> defsOfInterest = new HashMap<>();

    final Map<String, Object> pathsOfInterest = ENDPOINTS_OF_INTEREST
        .stream()
        .collect(Collectors.toMap(key -> key, key -> {
          final Object endpointOfInterest = (paths).getOrDefault(key, "");
          if (endpointOfInterest instanceof Map) {
            final Set<String> initialRefs = drillForRefs((Map<String, Object>) endpointOfInterest,
                new HashSet<>());
            defsOfInterest.putAll(retrieveDefinitions(definitions, initialRefs, new HashMap<>()));
          }
          return endpointOfInterest;
        }));

    output.put(PATHS, pathsOfInterest);
    output.put(DEFINITIONS, defsOfInterest);

    //Sort the map to look like the old one
    final TreeMap<String, Object> sortedTree = new TreeMap<>(
        Comparator.comparingInt(RES_ORDER::indexOf));
    sortedTree.putAll(output);

    return sortedTree;
  }

  /**
   * Recursive function, given a map it drills up to the last point in search of the key $ref
   *
   * @param dataOfInterest This is the map to be drilled
   * @param acc            This is the accumulator, a set of string
   * @return A set of string that has been trimmed and truncated
   */
  @SuppressWarnings("unchecked")
  private static Set<String> drillForRefs(Map<String, Object> dataOfInterest, Set<String> acc) {

    final String REF = "$ref";
    final String REF_VALUE_TEMPLATE = "#/definitions/";

    dataOfInterest.forEach((key, value) -> {

      if (key.equalsIgnoreCase(REF) && value instanceof String) {
        acc.add(String.valueOf(value).replace(REF_VALUE_TEMPLATE, ""));
      } else if (value instanceof Map) {
        drillForRefs((Map<String, Object>) value, acc);
      } else if (value instanceof ArrayList) {
        ((ArrayList<Object>) value).forEach(obj -> {
          if (obj instanceof Map) {
            drillForRefs((Map<String, Object>) obj, acc);
          }
        });
      }

    });

    return acc;
  }

  /**
   * Loops through a set of string and ensures its definitions are all retrieved
   *
   * @param definitions These are objects in form of map which we are interested in retrieving data
   *                    from
   * @param refs        These are the set of keys which we will use to query the aforementioned map
   * @param accumulator This is the store for the data so retrieved
   * @return a Map of the keys against the data structure they point to in the definitions object
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> retrieveDefinitions(Map<String, Object> definitions,
      Set<String> refs, Map<String, Object> accumulator) {

    final Set<String> alreadyLooped = new HashSet<>();
    do {
      final String key = refs.iterator().next();
      final Object value = definitions.get(key);
      if (value != null) {
        accumulator.put(key, value);
        alreadyLooped.add(key);
        drillForRefs((Map<String, Object>) value, refs);
      }
      refs.removeAll(alreadyLooped);
    } while (!refs.isEmpty());

    return accumulator;
  }


  static int m = 1;
  /**
   * This writes our end result back to the path {@linkplain Constants#RESULT_OUTPUT_PATH}
   *
   * @param result {@link SwaggerFile}
   */
  public static void writeFile(final SwaggerFile result) {
    final String liner = "------------------------------------------";
    System.out.println("File Name: " + SWAGGER_FILE_NAME);
    System.out.println(liner);
    System.out.println("File Type: " + result.swaggerFileType());
    System.out.println(liner);
    System.out.println("File Ext: " + result.swaggerFileType().getExtension());
    System.out.println(liner);
    System.out.println(result.data());
  }

  public static SwaggerFile convertToV3(final SwaggerFile file) {

    final SwaggerParseResult result = new OpenAPIParser().readContents(file.data(),
        null, null);

    //If there are errors, just print them to the console
    if (result.getMessages() != null && !result.getMessages().isEmpty())
      result.getMessages().forEach(System.err::println);

    final OpenAPI openAPI = result.getOpenAPI();
    final Map<String, Object> openapi = new HashMap<>();

    cleanUpNullKeyValues(openAPI.getOpenapi())
        .ifPresent(version -> openapi.put("openapi", version));
    cleanUpNullKeyValues(openAPI.getInfo())
        .ifPresent(info -> openapi.put("info", info));
    cleanUpNullKeyValues(openAPI.getExtensions())
        .ifPresent(extensions -> openapi.put("extensions", extensions));
    cleanUpNullKeyValues(openAPI.getExternalDocs())
        .ifPresent(externalDoc -> openapi.put("externalDocumentations", externalDoc));
    cleanUpNullKeyValues(openAPI.getTags())
        .ifPresent(tags -> openapi.put("tags", tags));
    cleanUpNullKeyValues(openAPI.getPaths())
        .ifPresent(paths -> openapi.put("paths", paths));
//    cleanUpNullKeyValues(openAPI.getComponents())
//        .ifPresent(components -> openapi.put("components", components));
    cleanUpNullKeyValues(openAPI.getSecurity())
        .ifPresent(security -> openapi.put("security", security));

    return new SwaggerFile(
        switch (file.swaggerFileType()) {
          case JSON -> gson.toJson(openapi, type);
          case YAML -> yaml.dump(openapi);
        },
        file.swaggerFileType()
    );
  }

  /**
   * Function to help clean up the DS by removing keys whose values are nulls
   * @param data This is the data to be checked, could be of any type.
   *            If it is a map, it will be traversed over again
   * @return Object, which could be of any type
   */
  @SuppressWarnings("unchecked")
  private static Optional<Object> cleanUpNullKeyValues(final Object data) {

    final String dataAsString = String.valueOf(data);
    if (dataAsString.equalsIgnoreCase("null") || dataAsString.equalsIgnoreCase("{}"))
      return Optional.empty();
    if (dataAsString.contains("class")) {
      final Map<String, Object> intermediate = new HashMap<>();
      ((Map<String, Object>) gson.fromJson(gson.toJson(data), type))
          .forEach((key, value) ->
              cleanUpNullKeyValues(value).ifPresent(res -> intermediate.put(key, res)));

      return intermediate.isEmpty() ? Optional.empty() : Optional.of(intermediate);
    }
    else return Optional.of(data);
  }

}
