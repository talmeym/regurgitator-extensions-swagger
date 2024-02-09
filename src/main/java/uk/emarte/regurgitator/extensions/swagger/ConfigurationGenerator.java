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
    public static final String REGURGITATOR_PREFIX = "regurgitator - ";

    private enum Method {
        GET,
        PUT,
        POST,
        PATCH,
        DELETE,
        HEAD
    }

    public static void main(String[] args) throws GenerationException {
        System.out.println("parsing open api file: " + args[0]);
        SwaggerParseResult result = new OpenAPIParser().readLocation(args[0], null, null);
        OpenAPI openAPI = result.getOpenAPI();
        Paths paths = openAPI.getPaths();
        List<Step> steps = new ArrayList<>();
        List<Rule> rules = new ArrayList<>();

        System.out.println("processing " + paths.size() + " path(s)");
        for(String path: paths.keySet()) {
            PathItem pathItem = paths.get(path);

            operation(pathItem.getGet(), path, pathItem, Method.GET, steps, rules);
            operation(pathItem.getPut(), path, pathItem, Method.PUT, steps, rules);
            operation(pathItem.getPost(), path, pathItem, Method.POST, steps, rules);
            operation(pathItem.getPatch(), path, pathItem, Method.PATCH, steps, rules);
            operation(pathItem.getDelete(), path, pathItem, Method.DELETE, steps, rules);
            operation(pathItem.getHead(), path, pathItem, Method.HEAD, steps, rules);
        }

        System.out.println("creating default no-path step");
        String defaultStepId = "no-path";
        steps.add(new CreateHttpResponse(defaultStepId, REGURGITATOR_PREFIX + "unmapped operation - " + 500L + " " + PLAIN_TEXT, 500L, PLAIN_TEXT));

        System.out.println("creating decision");
        Decision decision = new Decision("decision-1", steps, rules, defaultStepId);
        System.out.println("creating regurgitator configuration");
        RegurgitatorConfiguration regurgitatorConfiguration = new RegurgitatorConfiguration(singletonList(decision));
        System.out.println("saving file");

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(args[1], false);
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fileOutputStream, regurgitatorConfiguration);
        } catch (IOException e) {
            throw new GenerationException("Error generating Regurgitation configuration", e);
        }
    }
    
    private static void operation(Operation operation, String path, PathItem pathItem, Method method, List<Step> steps, List<Rule> rules) {
        if(operation != null) {
            System.out.println("processing path: " + path);
            System.out.println("- creating path and method conditions");
            Condition pathCondition = handleInlineParameters(path, pathItem.getParameters() != null ? pathItem.getParameters() : operation.getParameters());
            Condition methodCondition = new Condition(REQUEST_METADATA_METHOD, method.name(), null);

            ApiResponses responses = operation.getResponses();

            String statusCode = OK;
            String contentType = PLAIN_TEXT;

            if(responses != null) {
                statusCode = responses.keySet().iterator().next();
                ApiResponse apiResponse = responses.get(statusCode);
                Content content = apiResponse.getContent();
                contentType = content != null && content.size() > 0 ? content.keySet().iterator().next() : PLAIN_TEXT;
            }

            System.out.println("- creating http response step");
            String stepId = "path-" + (steps.size() + 1);
            steps.add(new CreateHttpResponse(stepId, REGURGITATOR_PREFIX + stepId + " - " + method + " " + path + " - " + statusCode + " " + contentType, Long.parseLong(statusCode), contentType));

            System.out.println("- creating rule");
            rules.add(new Rule(stepId, asList(methodCondition, pathCondition)));
        }
    }

    private static Condition handleInlineParameters(String path, List<Parameter> parameters) {
        if(path.contains("{") && path.contains("}")) {
            String prefix = path.substring(0, path.indexOf("{"));
            String suffix = path.substring(path.indexOf("}") + 1);
            String id = path.substring(path.indexOf("{") + 1, path.indexOf("}"));

            Optional<Parameter> firstParam = parameters.stream().filter(p -> id.equals(p.getName()) && "path".equals(p.getIn())).findFirst();
            String type = firstParam.isPresent() ? firstParam.get().getSchema().getType() : "string";
            boolean required = firstParam.isPresent() ? firstParam.get().getRequired() : true;

            System.out.println("-- creating regex for path");
            String regex = "^" + prefix.replace("/", "\\/") + "([" + ("integer".equals(type) ? NUMERIC : ALPHA_NUMERIC) + "]" + (required ? "+" : "*") + ")" + suffix + "$";
            return new Condition(REQUEST_METADATA_REQUEST_URI, null, regex);
        }

        return new Condition(REQUEST_METADATA_REQUEST_URI, path, null);
    }
}
