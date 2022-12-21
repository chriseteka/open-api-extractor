package com.chrisworks.ing.openapi.extractorutil;

import static com.chrisworks.ing.openapi.extractorutil.Constants.CONST_KEYS;
import static com.chrisworks.ing.openapi.extractorutil.Constants.ENDPOINTS_OF_INTEREST;
import static com.chrisworks.ing.openapi.extractorutil.Constants.RESULT_OUTPUT_PATH;
import static com.chrisworks.ing.openapi.extractorutil.Constants.RES_ORDER;
import static com.chrisworks.ing.openapi.extractorutil.Constants.SWAGGER_FILE_NAME;

import com.chrisworks.ing.openapi.extractorutil.models.AppException;
import com.chrisworks.ing.openapi.extractorutil.models.SwaggerFile;
import com.chrisworks.ing.openapi.extractorutil.models.SwaggerFileType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;

public final class SwaggerProcessor {
  
  static final Logger logger = Logger.getGlobal();

  private SwaggerProcessor() {
  }

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
      final Gson gson = new Gson();
      final Type type = new TypeToken<HashMap<String, Object>>() {
      }.getType();
      final Map<String, Object> input = gson.fromJson(jsonString, type);

      return gson.toJson(traverseData(input), type);

    } catch (IOException e) {
      e.printStackTrace();
      throw new AppException("Unable to parse json file");
    }
  }

  private static String processAsYAML(InputStream inputStream) {

    final Yaml yaml = new Yaml();
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
    final String RESPONSES = "responses";
    final String PARAMETERS = "parameters";
    final String DEFINITIONS = "definitions";
    final Map<String, Object> output = CONST_KEYS
        .stream()
        .collect(Collectors.toMap(key -> key, input::get));
    final Map<String, Object> responses = (Map<String, Object>) input.get(RESPONSES);

    final PathsAndRefs pathsAndRefs = extractPathsAndRefFrom((Map<String, Object>) input.get(PATHS), responses);
    final Map<String, Object> responsesOfInterest = pathsAndRefs.fromRefsFilterFor(responses);
    final Map<String, Object> defsOfInterest = pathsAndRefs.fromRefsFilterFor((Map<String, Object>) input.get(DEFINITIONS));
    final Map<String, Object> paramsOfInterest = pathsAndRefs.fromRefsFilterFor((Map<String, Object>) input.get(PARAMETERS));

    output.put(PATHS, pathsAndRefs.paths);
    output.put(DEFINITIONS, defsOfInterest);
    output.put(PARAMETERS, paramsOfInterest);
    output.put(RESPONSES, responsesOfInterest);

    //Sort the map to look like the old one
    final TreeMap<String, Object> sortedTree = new TreeMap<>(
        Comparator.comparingInt(RES_ORDER::indexOf));
    sortedTree.putAll(output);

    return sortedTree;
  }

  @SuppressWarnings("unchecked")
  private static PathsAndRefs extractPathsAndRefFrom(Map<String, Object> paths, Map<String, Object> responses) {
    return ENDPOINTS_OF_INTEREST
        .stream()
        .map(key -> {
          final Object endpointOfInterest = (paths).getOrDefault(key, "");
          final PathsAndRefs initPathAndRef = new PathsAndRefs(Map.of(key, endpointOfInterest));
          if (endpointOfInterest instanceof Map) {
            final Set<String> initialRefs = drillForRefs((Map<String, Object>) endpointOfInterest,
                new HashSet<>());
            initPathAndRef.withRefs(initialRefs);
          }
          //Because response sometimes contains definitions, we need to make sure its refs are passed on
          if (!responses.isEmpty()) {
            final Set<String> responsesRefs = drillForRefs(responses, new HashSet<>());
            initPathAndRef.addRefs(responsesRefs);
          }
          return initPathAndRef;
        })
        .reduce(PathsAndRefs.empty(), PathsAndRefs::merge);
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
    final String REF_VALUE_TEMPLATE_DEF = "#/definitions/";
    final String REF_VALUE_TEMPLATE_PAR = "#/parameters/";
    final String REF_VALUE_TEMPLATE_RES = "#/responses/";

    dataOfInterest.forEach((key, value) -> {

      if (key.equalsIgnoreCase(REF) && value instanceof String) {
        acc.add(String.valueOf(value).replace(REF_VALUE_TEMPLATE_DEF, "")
            .replace(REF_VALUE_TEMPLATE_PAR, "")
            .replace(REF_VALUE_TEMPLATE_RES, ""));
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

    do {
      final String key = refs.iterator().next();
      final Object value = definitions.get(key);
      if (value != null) {
        accumulator.put(key, value);
        drillForRefs((Map<String, Object>) value, refs);
      }
      refs.remove(key);
    } while (!refs.isEmpty());

    return accumulator;
  }

  /**
   * This writes our end result back to the path {@linkplain Constants#RESULT_OUTPUT_PATH}
   *
   * @param result {@link SwaggerFile}
   */
  public static void writeFile(final SwaggerFile result) {

    //WE may want to read this from the properties file as well
    final boolean debug = false;
    final String data = result.data();
    final SwaggerFileType swaggerFileType = result.swaggerFileType();
    if (debug) {
      final String liner = "------------------------------------------";
      logger.info("File Name: " + SWAGGER_FILE_NAME);
      logger.info(liner);
      logger.info("File Type: %s".formatted(swaggerFileType));
      logger.info(liner);
      logger.info("File Ext: %s".formatted(swaggerFileType.getExtension()));
      logger.info(liner);
      logger.info(data);
    }

    final String fileName = "%s/%s-output%s".formatted(RESULT_OUTPUT_PATH, SWAGGER_FILE_NAME, swaggerFileType.getExtension());
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
      writer.write(data);
      logger.info("Extraction completed");
      logger.info("Result written to the file: %s".formatted(fileName));
    } catch (IOException e) {
      throw new AppException("Failed to write results with reason: " + e.getMessage());
    }

  }

  private record PathsAndRefs(Map<String, Object> paths, Set<String> refs) {

    private PathsAndRefs(Map<String, Object> paths) {
      this(paths, new HashSet<>());
    }

    private void withRefs(Set<String> initRefs) {
      refs.addAll(initRefs);
    }

    private PathsAndRefs merge(PathsAndRefs another) {
      refs.addAll(another.refs);
      paths.putAll(another.paths);
      return this;
    }

    private static PathsAndRefs empty() {
      return new PathsAndRefs(new HashMap<>(), new HashSet<>());
    }

    private Set<String> aCopyOfRefs() {
      return new HashSet<>(refs);
    }

    private Map<String, Object> fromRefsFilterFor(Map<String, Object> filterMap) {
      if (filterMap.isEmpty())
        return filterMap;
      return retrieveDefinitions(filterMap, aCopyOfRefs(), new HashMap<>());
    }

    public void addRefs(Set<String> responsesRefs) {
      refs.addAll(responsesRefs);
    }
  }

}
