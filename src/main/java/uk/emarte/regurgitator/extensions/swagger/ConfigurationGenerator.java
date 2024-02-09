package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import uk.emarte.regurgitator.core.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

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

            System.out.println("processing " + paths.size() + " path(s)");
            for (String path : paths.keySet()) {
                PathItem pathItem = paths.get(path);
                processOperation(pathItem.getGet(), path, pathItem, Method.GET, steps, rules);
                processOperation(pathItem.getPut(), path, pathItem, Method.PUT, steps, rules);
                processOperation(pathItem.getPost(), path, pathItem, Method.POST, steps, rules);
                processOperation(pathItem.getPatch(), path, pathItem, Method.PATCH, steps, rules);
                processOperation(pathItem.getDelete(), path, pathItem, Method.DELETE, steps, rules);
                processOperation(pathItem.getHead(), path, pathItem, Method.HEAD, steps, rules);
            }

            System.out.println("creating default no-path step");
            String defaultStepId = "no-path";
            steps.add(new CreateHttpResponse(defaultStepId, REGURGITATOR_COLON + "unmapped operation : " + 500L + " " + PLAIN_TEXT, 500L, PLAIN_TEXT));

            System.out.println("creating decision");
            Decision decision = new Decision("decision-1", steps, rules, defaultStepId);

            System.out.println("saving regurgitator configuration");

            FileOutputStream fileOutputStream = new FileOutputStream(new File(outputDirectory, "regurgitator-configuration.json"), false);
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fileOutputStream, new RegurgitatorConfiguration(singletonList(decision)));
        } catch (Exception e) {
            throw new GenerationException("Error generating configuration", e);
        }
    }

    private static void processOperation(Operation operation, String path, PathItem pathItem, Method method, List<Step> steps, List<Rule> rules) {
        if (operation != null) {
            System.out.println("processing path: " + path);
            System.out.println("- creating path and method conditions");
            Condition pathCondition = buildPathCondition(path, pathItem.getParameters() != null ? pathItem.getParameters() : operation.getParameters());
            Condition methodCondition = new Condition(REQUEST_METADATA_METHOD, method.name(), null);

            ApiResponses responses = operation.getResponses();

            String statusCode = OK;
            String contentType = PLAIN_TEXT;

            if (responses != null) {
                statusCode = responses.keySet().iterator().next();
                ApiResponse apiResponse = responses.get(statusCode);
                Content content = apiResponse.getContent();
                contentType = content != null && content.size() > 0 ? content.keySet().iterator().next() : PLAIN_TEXT;
            }

            System.out.println("- creating http response step");
            String stepId = "path-" + (steps.size() + 1);
            steps.add(new CreateHttpResponse(stepId, REGURGITATOR_COLON + stepId + " : " + method + " " + path + " : " + statusCode + " " + contentType, Long.parseLong(statusCode), PLAIN_TEXT));

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

    private static void processPathParameter(String id, List<Parameter> parameters, List<String> types, List<Boolean> requireds) {
        Optional<Parameter> firstParam = parameters.stream().filter(p -> id.equals(p.getName()) && "path".equals(p.getIn())).findFirst();
        types.add(firstParam.isPresent() ? firstParam.get().getSchema().getType() : "string");
        requireds.add(firstParam.isPresent() ? firstParam.get().getRequired() : true);
    }
}
