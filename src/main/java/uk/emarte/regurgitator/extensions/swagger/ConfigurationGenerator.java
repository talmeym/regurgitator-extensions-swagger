/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static java.lang.Long.parseLong;
import static java.util.Arrays.asList;
import static java.util.Collections.*;

public class ConfigurationGenerator {
    public static final String PLAIN_TEXT = "text/plain";
    public static final String OK = "200";
    public static final String NUMERIC = "0-9";
    public static final String ALPHA_NUMERIC = "A-Za-z0-9-";
    public static final String REQUEST_METADATA_REQUEST_URI = "request-metadata:request-uri";
    public static final String REQUEST_METADATA_METHOD = "request-metadata:method";
    public static final String REGURGITATOR_COLON = "regurgitator : ";

    private enum Method {
        GET, PUT, POST, PATCH, DELETE, HEAD
    }

    @SuppressWarnings("rawtypes")
    public static void main(String[] args) throws GenerationException {
        File swaggerFile = new File(args[0]);
        File outputDirectory = new File(args[1]);

        if (!(swaggerFile.exists() && outputDirectory.exists() && outputDirectory.isDirectory())) {
            System.err.println("Usage: java uk.emarte.regurgitator.extensions.swagger.ConfigurationGenerator swaggerFile.json /outputDirectory");
            System.exit(1);
        }

        try {
            System.out.println("parsing open api file: " + swaggerFile.getName());
            SwaggerParseResult result = new OpenAPIParser().readLocation(swaggerFile.getAbsolutePath(), null, null);
            OpenAPI openAPI = result.getOpenAPI();
            Paths paths = openAPI.getPaths();
            List<Step> steps = new ArrayList<>();
            List<Rule> rules = new ArrayList<>();

            Map<String, Schema> componentSchemas = openAPI.getComponents() != null ? openAPI.getComponents().getSchemas() : null;

            System.out.println("processing " + paths.size() + " path(s)");
            for (String path : paths.keySet()) {
                PathItem pathItem = paths.get(path);
                processOperation(pathItem.getGet(), path, pathItem, Method.GET, steps, rules, componentSchemas, outputDirectory);
                processOperation(pathItem.getPut(), path, pathItem, Method.PUT, steps, rules, componentSchemas, outputDirectory);
                processOperation(pathItem.getPost(), path, pathItem, Method.POST, steps, rules, componentSchemas, outputDirectory);
                processOperation(pathItem.getPatch(), path, pathItem, Method.PATCH, steps, rules, componentSchemas, outputDirectory);
                processOperation(pathItem.getDelete(), path, pathItem, Method.DELETE, steps, rules, componentSchemas, outputDirectory);
                processOperation(pathItem.getHead(), path, pathItem, Method.HEAD, steps, rules, componentSchemas, outputDirectory);
            }

            System.out.println("creating default no-path step");
            String defaultStepId = "step-" + (steps.size() + 1);
            steps.add(new CreateHttpResponse(defaultStepId, REGURGITATOR_COLON + "unmapped operation", null, 500L, PLAIN_TEXT));

            System.out.println("creating decision");
            Decision decision = new Decision("decision-1", steps, rules, defaultStepId);

            System.out.println("saving regurgitator configuration");

            FileOutputStream fileOutputStream = new FileOutputStream(new File(outputDirectory, "regurgitator-configuration.json"), false);
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fileOutputStream, new RegurgitatorConfiguration(singletonList(decision)));
        } catch (Exception e) {
            throw new GenerationException("Error generating configuration", e);
        }
    }

    @SuppressWarnings({"rawtypes", "ResultOfMethodCallIgnored"})
    private static void processOperation(Operation operation, String path, PathItem pathItem, Method method, List<Step> steps, List<Rule> rules, Map<String, Schema> componentSchemas, File outputDirectory) throws IOException {
        if (operation != null) {
            System.out.println("processing " + method + " " + path);
            System.out.println("- creating path and method conditions");
            Condition pathCondition = buildPathCondition(path, pathItem.getParameters() != null ? pathItem.getParameters() : operation.getParameters());
            Condition methodCondition = new Condition(REQUEST_METADATA_METHOD, method.name(), null);
            ApiResponses responses = operation.getResponses();
            String stepId = "step-" + (steps.size() + 1);

            if (responses != null) {
                File pathDirectory = new File(outputDirectory, method + path.replace("/", "-").replace("{", "_").replace("}", "_"));
                pathDirectory.mkdirs();

                for (String code : responses.keySet()) {
                    System.out.println("### " + code + " response");
                    ApiResponse apiResponse = responses.get(code);
                    Content content = apiResponse.getContent();

                    if (content != null && content.size() > 0) {
                        String firstMediaTypeName = content.keySet().iterator().next();
                        System.out.println("### media type " + firstMediaTypeName);
                        MediaType firstMediaType = content.get(firstMediaTypeName);

                        if (firstMediaType.getSchema() != null && (firstMediaType.getSchema().getProperties() != null || firstMediaType.getSchema().get$ref() != null || firstMediaType.getSchema().getAdditionalProperties() != null || "array".equals(firstMediaType.getSchema().getType()))) {
                            File responseFile = new File(pathDirectory, pathDirectory.getName() + "-" + code + ".json");
                            System.out.println("Generating response file: " + responseFile.getName());
                            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new FileOutputStream(responseFile, false), buildObject(firstMediaType.getSchema(), componentSchemas));
                        }
                    } else {
                        System.out.println("### code " + code + " has no content !?");
                    }
                }

                Optional<String> optStatusCodeToUse = responses.keySet().stream().filter(sc -> sc.length() == 3 && sc.startsWith("2")).findFirst();
                String statusCodeToUse = optStatusCodeToUse.orElseGet(() -> responses.keySet().iterator().next());
                String responseFileToUse = "classpath:/" + pathDirectory.getName() + "/" + pathDirectory.getName() + "-" + statusCodeToUse + ".json";
                Content contentToUse = responses.get(statusCodeToUse).getContent();
                String contentTypeToUse = contentToUse != null && contentToUse.size() > 0 ? contentToUse.keySet().iterator().next() : PLAIN_TEXT;

                CreateHttpResponse createHttpResponse;

                if(new File(pathDirectory, pathDirectory.getName() + "-" + statusCodeToUse + ".json").exists()) {
                    System.out.println("- creating http response step using response file");
                    createHttpResponse = new CreateHttpResponse(null, null, responseFileToUse, parseLong(statusCodeToUse), contentTypeToUse);
                } else {
                    System.out.println("- creating http response step without response file");
                    createHttpResponse = new CreateHttpResponse(null, "no content", null, parseLong(statusCodeToUse), contentTypeToUse);
                }

                List<Step> stepsForConfiguration = buildCreateParameterStepsForPath(path, pathItem.getParameters() != null ? pathItem.getParameters() : operation.getParameters());
                stepsForConfiguration.add(createHttpResponse);

                RegurgitatorConfiguration regurgitatorConfiguration = new RegurgitatorConfiguration(stepsForConfiguration);
                File configFile = new File(pathDirectory, "regurgitator-configuration.json");
                System.out.println("- generating config file: " + pathDirectory.getName() + "/" + configFile.getName());
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new FileOutputStream(configFile, false), regurgitatorConfiguration);

                System.out.println("- creating sequence ref step");
                steps.add(new SequenceRef(stepId, "classpath:/" + pathDirectory.getName() + "/regurgitator-configuration.json"));
            } else {
                System.out.println("- creating http response step");
                steps.add(new CreateHttpResponse(stepId, REGURGITATOR_COLON + stepId + " : " + method + " " + path + " : " + OK + " " + PLAIN_TEXT, null, parseLong(OK), PLAIN_TEXT));
            }

            System.out.println("- creating rule");
            rules.add(new Rule(stepId, asList(methodCondition, pathCondition)));
        }
    }

    private static Condition buildPathCondition(String path, List<Parameter> parameters) {
        if (path.contains("{") && path.contains("}")) {
            System.out.println("-- parsing inline parameters from path");
            List<String> separators = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Boolean> requireds = new ArrayList<>();

            while (path.contains("{")) {
                separators.add(path.substring(0, path.indexOf("{")));
                processPathParameter(path.substring(path.indexOf("{") + 1, path.indexOf("}")), parameters, types, requireds);
                path = path.substring(path.indexOf("}") + 1);
            }

            separators.add(path);

            System.out.println("-- creating regex for path");
            StringBuilder builder = new StringBuilder("^");

            while (!separators.isEmpty()) {
                builder.append(separators.remove(0).replace("/", "\\/"));

                if (!types.isEmpty()) {
                    builder.append("([").append("integer".equals(types.remove(0)) ? NUMERIC : ALPHA_NUMERIC).append("]").append(requireds.remove(0) ? "+" : "*").append(")");
                }
            }

            String regex = builder.append("$").toString();
            return new Condition(REQUEST_METADATA_REQUEST_URI, null, regex);
        }

        return new Condition(REQUEST_METADATA_REQUEST_URI, path, null);
    }

    private static List<Step> buildCreateParameterStepsForPath(String path, List<Parameter> parameters) {
        if (path.contains("{") && path.contains("}")) {
            System.out.println("-- parsing inline parameters from path");
            List<String> ids = new ArrayList<>();
            List<String> separators = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Boolean> requireds = new ArrayList<>();

            while (path.contains("{")) {
                separators.add(path.substring(0, path.indexOf("{")));
                String id = path.substring(path.indexOf("{") + 1, path.indexOf("}"));
                processPathParameter(id, parameters, types, requireds);
                ids.add(id);
                path = path.substring(path.indexOf("}") + 1);
            }

            separators.add(path);

            System.out.println("-- creating extract format for path");
            StringBuilder builder = new StringBuilder();
            int idIndex = 0;

            while (!separators.isEmpty()) {
                builder.append(separators.remove(0));

                if (!types.isEmpty()) {
                    types.remove(0);
                    builder.append("{").append(idIndex++).append("}");
                }
            }

            String extractFormat = builder.toString();
            System.out.println("-- creating create-parameter steps for path params");
            List<Step> createParameters = new ArrayList<>();

            for(int i = 0; i < ids.size(); i++) {
                createParameters.add(new CreateParameter(ids.get(i), REQUEST_METADATA_REQUEST_URI, new ExtractProcessor(extractFormat, i)));
            }

            return createParameters;
        }

        return new ArrayList<>();
    }

    private static void processPathParameter(String id, List<Parameter> parameters, List<String> types, List<Boolean> requireds) {
        Optional<Parameter> firstParam = parameters.stream().filter(p -> id.equals(p.getName()) && "path".equals(p.getIn())).findFirst();
        types.add(firstParam.isPresent() ? firstParam.get().getSchema().getType() : "string");
        requireds.add(firstParam.isPresent() ? firstParam.get().getRequired() : true);
    }

    @SuppressWarnings("rawtypes")
    private static Object buildObject(Schema<?> schema, Map<String, Schema> componentSchemas) {

        if (schema.get$ref() != null) {
            String $ref = schema.get$ref();
            $ref = $ref.contains("/") ? $ref.substring($ref.lastIndexOf("/") + 1) : $ref;
            schema = componentSchemas.get($ref);
        }

        if (schema.getProperties() != null || schema.getAdditionalProperties() != null) {
            Map<String, Object> objectContents = new LinkedHashMap<>();
            Map<String, Schema> properties = schema.getProperties() != null ? schema.getProperties() : ((ObjectSchema) schema.getAdditionalProperties()).getProperties();

            for (String name : properties.keySet()) {
                Schema<?> propertySchema = properties.get(name);
                String type = propertySchema.getType();

                if ("array".equals(type)) {
                    objectContents.put(name, singletonList(buildObject(propertySchema.getItems(), componentSchemas)));
                } else if ("integer".equals(type)) {
                    objectContents.put(name, Integer.parseInt("" + (propertySchema.getExample() != null ? propertySchema.getExample() : 0)));
                } else if ("object".equals(type)) {
                    objectContents.put(name, buildObject(propertySchema, componentSchemas));
                } else if ("boolean".equals(type)) {
                    objectContents.put(name, Boolean.parseBoolean("" + (propertySchema.getExample() != null ? propertySchema.getExample() : true)));
                } else if ("float".equals(type)) {
                    objectContents.put(name, Float.parseFloat("" + (propertySchema.getExample() != null ? propertySchema.getExample() : 0f)));
                } else if ("double".equals(type)) {
                    objectContents.put(name, Double.parseDouble("" + (propertySchema.getExample() != null ? propertySchema.getExample() : 0d)));
                } else if (propertySchema.get$ref() != null) {
                    String $ref = propertySchema.get$ref();
                    $ref = $ref.contains("/") ? $ref.substring($ref.lastIndexOf("/") + 1) : $ref;
                    objectContents.put(name, buildObject(componentSchemas.get($ref), componentSchemas));
                } else { // assume string
                    objectContents.put(name, "" + (propertySchema.getExample() != null ? propertySchema.getExample() : "foobar"));
                }
            }

            return objectContents;
        } else if ("array".equals(schema.getType())) {
            return singletonList(buildObject(schema.getItems(), componentSchemas));
        } else if ("string".equals(schema.getType())) {
            return schema.getExample();
        }

        throw new IllegalStateException("Cannot construct object for schema");
    }
}
