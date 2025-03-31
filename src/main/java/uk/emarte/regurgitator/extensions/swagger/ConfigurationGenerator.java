/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import uk.emarte.regurgitator.extensions.swagger.postman.Collection;
import uk.emarte.regurgitator.extensions.swagger.postman.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static uk.emarte.regurgitator.extensions.swagger.postman.Collection.PLACEHOLDER;

/**
 * Generates regurgitator configuration from open api (v3) 'swagger' files
 */
public class ConfigurationGenerator {
    private static final String USAGE_TEXT = "Usage: java uk.emarte.regurgitator.extensions.swagger.ConfigurationGenerator swaggerFile.[json|yaml] /outputDirectory xml|json";
    private static final String NUMERIC = "0-9", ALPHA_NUMERIC = "A-Za-z0-9-";
    private static final String REQUEST_METADATA_REQUEST_URI = "request-metadata:request-uri";
    private static final String REQUEST_METADATA_QUERY_STRING = "request-metadata:query-string";
    private static final String REQUEST_METADATA_METHOD = "request-metadata:method";
    private static final String SLASH_SUBSTITUTE = "-", CURLY_BRACE_SUBSTITUTE = "^";
    private static final String JSON = "json", XML = "xml", OK = "200", DEFAULT = "default", NO_CONTENT = "no content";
    private static final String STRING = "string", INTEGER = "integer", ARRAY = "array";
    private static final String RESPONSE_CODE_HEADER = "mock-response-code";
    private static final String REQUEST_HEADERS_MOCK_RESPONSE_CODE = "request-headers:mock-response-code", RESPONSE_METADATA_CONTENT_TYPE = "response-metadata:content-type", RESPONSE_METADATA_STATUS_CODE = "response-metadata:status-code";
    private static final String NUM_XX_REGEX = "^([2-5])(XX|xx)$", ZERO_ZERO = "00";
    private static final String PLAIN_TEXT = "text/plain";
    private static final List<String> APPLICATION_XMLS = Arrays.asList("application/xml", "text/xml");
    private static final List<String> APPLICATION_JSONS = Arrays.asList("application/json", "application/vnd.amadeus+json");

    private enum OutputType {
        json {
            @Override
            void save(Object object, FileOutputStream fileOutputStream) throws IOException {
                JsonUtil.saveToJson(object, fileOutputStream);
            }
        },
        xml {
            @Override
            void save(Object object, FileOutputStream fileOutputStream) throws ParserConfigurationException, TransformerException {
                XmlUtil.saveToXml((RegurgitatorConfiguration) object, fileOutputStream);
            }
        };

        static boolean contains(String type) {
            return Arrays.stream(values()).anyMatch(ot -> ot.name().equals(type));
        }

        abstract void save(Object object, FileOutputStream fileOutputStream) throws IOException, ParserConfigurationException, TransformerException;
    }

    /**
     * @param args input arguments - [0] open api 'swagger' file path, [1] output directory path, [2] output type
     * @throws GenerationException if a problem is encountered whilst generating the configuration
     * @see #generateConfiguration(File, File, String)
     */
    public static void main(String[] args) throws GenerationException {
        if (args.length != 3) {
            System.err.println("Invalid argument count: " + args.length);
            System.err.println(USAGE_TEXT);
            System.exit(1);
        }

        generateConfiguration(new File(args[0]), new File(args[1]), args[2]);
    }

    /**
     * generates a set of regurgitator configuration from any open api 'swagger' file.
     * NOTE: both parameters need to exist.
     *
     * @param swaggerFile     an open api 'swagger' file from which to generate configuration
     * @param outputDirectory a directory into which to save the configuration files
     * @param outputTypeStr   the desired document type for the configuration files [json|xml]
     * @throws GenerationException if a problem is encountered whilst generating the configuration
     */
    public static void generateConfiguration(File swaggerFile, File outputDirectory, String outputTypeStr) throws GenerationException {
        if (!(swaggerFile.exists() && outputDirectory.isDirectory() && outputDirectory.exists() && OutputType.contains(outputTypeStr))) {
            if (!swaggerFile.exists()) {
                System.err.println("Swagger file does not exist");
            }

            if (!outputDirectory.isDirectory()) {
                System.err.println("Output directory is not a directory");
            }

            if (!outputDirectory.exists()) {
                System.err.println("Output directory does not exist");
            }

            if (!OutputType.contains(outputTypeStr)) {
                System.err.println("Invalid output type: " + outputTypeStr);
            }

            System.err.println(USAGE_TEXT);
            System.exit(1);
        }

        OutputType outputType = OutputType.valueOf(outputTypeStr);

        try {
            System.out.println("parsing open api file: " + swaggerFile.getName());
            SwaggerParseResult result = new OpenAPIParser().readLocation(swaggerFile.getAbsolutePath(), null, null);
            OpenAPI openAPI = result.getOpenAPI();
            Paths paths = openAPI.getPaths();
            List<Step> steps = new ArrayList<>();
            List<Rule> rules = new ArrayList<>();

            List<Item> postmanItems = new ArrayList<>();
            Set<Variable> postmanVariables = new TreeSet<>();

            System.out.println("processing " + paths.size() + " route(s)");
            for (String path : paths.keySet()) {
                PathItem pathItem = paths.get(path);
                path = escapeUrlParts(path);
                processOperation(pathItem.getGet(), path, pathItem, Method.GET, steps, rules, openAPI.getComponents(), outputDirectory, outputType, postmanItems, postmanVariables);
                processOperation(pathItem.getPut(), path, pathItem, Method.PUT, steps, rules, openAPI.getComponents(), outputDirectory, outputType, postmanItems, postmanVariables);
                processOperation(pathItem.getPost(), path, pathItem, Method.POST, steps, rules, openAPI.getComponents(), outputDirectory, outputType, postmanItems, postmanVariables);
                processOperation(pathItem.getPatch(), path, pathItem, Method.PATCH, steps, rules, openAPI.getComponents(), outputDirectory, outputType, postmanItems, postmanVariables);
                processOperation(pathItem.getDelete(), path, pathItem, Method.DELETE, steps, rules, openAPI.getComponents(), outputDirectory, outputType, postmanItems, postmanVariables);
                processOperation(pathItem.getHead(), path, pathItem, Method.HEAD, steps, rules, openAPI.getComponents(), outputDirectory, outputType, postmanItems, postmanVariables);
            }

            System.out.println("creating routing default step");
            String defaultStepId = "default-route";
            steps.add(new CreateHttpResponse(defaultStepId, "regurgitator : unmapped operation", null, 500L, PLAIN_TEXT));

            System.out.println("creating routing decision");
            Decision decision = new Decision("routing-decision", steps, rules, defaultStepId);

            System.out.println("### saving routing configuration");
            outputType.save(new RegurgitatorConfiguration(singletonList(decision)), new FileOutputStream(new File(outputDirectory, "regurgitator-configuration." + outputType), false));

            ItemGroup postmanItemGroup = new ItemGroup("All Requests", "All requests for swagger file " + swaggerFile.getName(), postmanItems.stream().filter(Objects::nonNull).toArray(Item[]::new));
            Collection postmanCollection = new Collection(new Info("Swagger Collection - " + swaggerFile.getName(), "Collection for swagger file " + swaggerFile.getName(), "1", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"), new ItemGroup[]{postmanItemGroup}, postmanVariables.stream().filter(Objects::nonNull).toArray(Variable[]::new));
            JsonUtil.saveToJson(postmanCollection, new FileOutputStream(new File(outputDirectory, "postman.json"), false));
        } catch (GenerationException ge) {
            throw ge;
        } catch (Exception e) {
            throw new GenerationException("Error generating configuration", e);
        }
    }

    /**
     * *
     * @param path
     * @return
     */
    private static String escapeUrlParts(String path) {
        return Arrays.stream(path.split("/")).map(s -> {
            try {
                return URLEncoder.encode(s, "UTF-8").replaceAll("%7B", "{").replaceAll("%7D", "}");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("Error url encoding url: " + path);
            }
        }).collect(Collectors.joining("/"));
    }

    /**
     * *
     * @param operation
     * @param path
     * @param pathItem
     * @param method
     * @param steps
     * @param rules
     * @param components
     * @param outputDirectory
     * @param outputType
     * @param postmanItems
     * @param postmanVariables
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     * @throws GenerationException
     */
    private static void processOperation(Operation operation, String path, PathItem pathItem, Method method, List<Step> steps, List<Rule> rules, Components components, File outputDirectory, OutputType outputType, List<Item> postmanItems, Set<Variable> postmanVariables) throws IOException, ParserConfigurationException, TransformerException, GenerationException {
        if (operation != null) {
            System.out.println("processing route " + method + " " + path);

            File pathDirectory = new File(outputDirectory, method + path.replace("/", SLASH_SUBSTITUTE).replace("{", CURLY_BRACE_SUBSTITUTE).replace("}", CURLY_BRACE_SUBSTITUTE));

            if (!pathDirectory.mkdirs()) {
                throw new GenerationException("A configuration directory already exists: " + pathDirectory.getName());
            }

            RequestBody requestBody = operation.getRequestBody();
            String requestContent = null;

            if (requestBody != null) {
                System.out.println("### request");
                requestContent = processRequest(requestBody, components, pathDirectory);
            }

            ApiResponses responses = operation.getResponses();

            List<Step> responseDecisionSteps = new ArrayList<>();
            List<Rule> responseDecisionRules = new ArrayList<>();
            Map<String, String> responseContents = new LinkedHashMap<>();

            if (responses != null) {
                for (String code : responses.keySet()) {
                    responseContents.put(code, processResponse(code, responses.get(code), responseDecisionSteps, responseDecisionRules, components, pathDirectory));
                }
            } else {
                throw new IllegalStateException("eh ???");
            }

            System.out.println("creating path param extract steps");
            List<Parameter> allParams = new ArrayList<>(pathItem.getParameters() != null ? pathItem.getParameters() : emptyList());
            allParams.addAll(operation.getParameters() != null ? operation.getParameters() : emptyList());
            List<Step> parameterStepsForPath = buildCreateParameterStepsForPath(path, allParams);
            List<Step> stepsForConfiguration = new ArrayList<>(parameterStepsForPath);
            stepsForConfiguration.addAll(buildCreateParameterStepsForQuery(allParams));

            if (responseContents.size() > 0) {
                if (responseContents.size() > 1) {
                    System.out.println("creating route response decision");
                    Optional<String> optDef = responses.keySet().stream().filter(sc -> sc.equals(DEFAULT)).findFirst();
                    Optional<String> opt2XX = responses.keySet().stream().filter(sc -> sc.length() == 3 && sc.startsWith("2")).findFirst();
                    Optional<String> optNumeric = responses.keySet().stream().filter(StringUtils::isNumeric).findFirst();
                    String defaultStatusCode = opt2XX.orElse(optNumeric.orElse(optDef.orElse(responses.keySet().iterator().next())));

                    if(optDef.isPresent()) {
                        responseDecisionRules.add(new Rule(pathDirectory.getName() + "-" + DEFAULT, singletonList(new Condition(REQUEST_HEADERS_MOCK_RESPONSE_CODE, null, null, "true"))));
                    }

                    stepsForConfiguration.add(new Decision(null, responseDecisionSteps, responseDecisionRules, pathDirectory.getName() + "-" + defaultStatusCode));
                } else {
                    stepsForConfiguration.add(responseDecisionSteps.get(0));
                }
            } else {
                System.out.println("creating route response step, no responses generated");
                stepsForConfiguration.add(new CreateHttpResponse(null, "regurgitator : " + method + " " + path, null, parseLong(OK), PLAIN_TEXT));
            }

            RegurgitatorConfiguration regurgitatorConfiguration = new RegurgitatorConfiguration(stepsForConfiguration);
            File configFile = new File(pathDirectory, "regurgitator-configuration." + outputType);
            System.out.println("### generating config file: " + pathDirectory.getName() + "/" + configFile.getName());
            outputType.save(regurgitatorConfiguration, new FileOutputStream(configFile, false));

            System.out.println("creating sequence ref step");
            String stepId = "route-" + (steps.size() + 1);
            steps.add(new SequenceRef(stepId, "classpath:/" + pathDirectory.getName() + "/regurgitator-configuration." + outputType));

            System.out.println("creating path condition");
            Condition pathCondition = buildPathCondition(path, pathItem.getParameters() != null ? pathItem.getParameters() : operation.getParameters());
            System.out.println("creating method condition");
            Condition methodCondition = new Condition(REQUEST_METADATA_METHOD, method.name(), null, null);
            System.out.println("creating routing rule");
            rules.add(new Rule(stepId, asList(methodCondition, pathCondition)));

            List<QueryParam> queryParams = allParams.stream().filter(p -> "query".equals(p.getIn())).map(p -> new QueryParam(p.getName(), "PLACEHOLDER", p.getRequired() == null || !p.getRequired())).collect(Collectors.toList());
            Request postmanRequest = new Request(operation.getSummary(), operation.getDescription(), path, method, responseContents.keySet().size() > 1 ? responseContents.keySet().stream().map(k -> new Header(RESPONSE_CODE_HEADER, k.replace(DEFAULT, PLACEHOLDER), true, "Return response code " + k)).toArray(Header[]::new) : null, new Body(Mode.raw, requestContent, false), queryParams);
            postmanItems.add(new Item(method + " " + path, postmanRequest, responseContents.entrySet().stream().filter(e -> (StringUtils.isNumeric(e.getKey()) || Pattern.compile(NUM_XX_REGEX).matcher(e.getKey()).matches()) && e.getValue() != null).map(e -> new Response(e.getValue(), Integer.parseInt(e.getKey().replaceAll("X", "0")))).toArray(Response[]::new)));
            postmanVariables.addAll(parameterStepsForPath.stream().map(s -> new Variable(((CreateParameter) s).getName(), PLACEHOLDER)).collect(Collectors.toList()));
        }
    }

    /**
     * *
     * @param requestBody
     * @param components
     * @param pathDirectory
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    private static String processRequest(RequestBody requestBody, Components components, File pathDirectory) throws IOException, ParserConfigurationException, TransformerException {
        System.out.println("### request");

        Content requestContent = requestBody.getContent();

        if (requestBody.get$ref() != null) {
            String ref = requestBody.get$ref();
            requestContent = components.getRequestBodies().get(ref.contains("/") ? ref.substring(ref.lastIndexOf("/") + 1) : ref).getContent();
        }

        if (requestContent != null && requestContent.size() > 0) {
            String requestMediaTypeName = requestContent.keySet().iterator().next();
            System.out.println("### media type " + requestMediaTypeName);

            if (APPLICATION_JSONS.contains(requestMediaTypeName) || APPLICATION_XMLS.contains(requestMediaTypeName)) {
                MediaType requestMediaType = requestContent.get(requestMediaTypeName);

                if (requestMediaType.getSchema() != null && (requestMediaType.getSchema().getProperties() != null || requestMediaType.getSchema().get$ref() != null || requestMediaType.getSchema().getAdditionalProperties() != null || ARRAY.equals(requestMediaType.getSchema().getType()))) {
                    File requestFile = new File(pathDirectory, pathDirectory.getName() + "-request." + (APPLICATION_JSONS.contains(requestMediaTypeName) ? JSON : XML));
                    System.out.println("### generating request file: " + requestFile.getName());

                    if (APPLICATION_JSONS.contains(requestMediaTypeName)) {
                        return JsonUtil.saveToJson(JsonUtil.buildJsonObject(requestMediaType.getSchema(), components, 1), new FileOutputStream(requestFile, false));
                    } else {
                        Document document = XmlUtil.newDocument();
                        return XmlUtil.saveToXml(XmlUtil.buildXmlObject(null, requestMediaType.getSchema(), document, components, 1), document, new FileOutputStream(requestFile, false));
                    }
                }
            } else {
                System.out.println("### unsupported request media type: " + requestMediaTypeName);
            }
        } else {
            System.out.println("### request has no content !?");
        }

        return null;
    }

    /**
     * *
     * @param code
     * @param apiResponse
     * @param responseDecisionSteps
     * @param responseDecisionRules
     * @param components
     * @param pathDirectory
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    private static String processResponse(String code, ApiResponse apiResponse, List<Step> responseDecisionSteps, List<Rule> responseDecisionRules, Components components, File pathDirectory) throws IOException, ParserConfigurationException, TransformerException {
        System.out.println("### " + code + " response");

        Matcher numericCodeRegex = Pattern.compile(NUM_XX_REGEX).matcher(code);
        Content responseContent = apiResponse.getContent();

        if (apiResponse.get$ref() != null) {
            String ref = apiResponse.get$ref();
            responseContent = components.getResponses().get(ref.contains("/") ? ref.substring(ref.lastIndexOf("/") + 1) : ref).getContent();
        }

        if (responseContent != null && responseContent.size() > 0) {
            String responseMediaTypeName = responseContent.keySet().iterator().next();
            System.out.println("### media type " + responseMediaTypeName);

            if (APPLICATION_JSONS.contains(responseMediaTypeName) || APPLICATION_XMLS.contains(responseMediaTypeName)) {
                MediaType responseMediaType = responseContent.get(responseMediaTypeName);
                File responseFile = new File(pathDirectory, pathDirectory.getName() + "-" + code + "." + (APPLICATION_JSONS.contains(responseMediaTypeName) ? JSON : XML));
                String fileReference = "classpath:/" + pathDirectory.getName() + "/" + responseFile.getName();

                if (responseMediaType.getSchema() != null && (responseMediaType.getSchema().getProperties() != null || responseMediaType.getSchema().get$ref() != null || responseMediaType.getSchema().getAdditionalProperties() != null || responseMediaType.getSchema().getType() != null)) {
                    System.out.println("### generating response file: " + responseFile.getName());
                    String content;

                    if (APPLICATION_JSONS.contains(responseMediaTypeName)) {
                        content = JsonUtil.saveToJson(JsonUtil.buildJsonObject(responseMediaType.getSchema(), components, 1), new FileOutputStream(responseFile, false));
                    } else {
                        Document document = XmlUtil.newDocument();
                        content = XmlUtil.saveToXml(XmlUtil.buildXmlObject(null, responseMediaType.getSchema(), document, components, 1), document, new FileOutputStream(responseFile, false));
                    }

                    generateLogicSteps(code, responseDecisionSteps, responseDecisionRules, pathDirectory, numericCodeRegex, responseMediaTypeName, null, fileReference);

                    return content;
                } else if (responseMediaType.getExample() != null) {
                    System.out.println("### generating response file: " + responseFile.getName());
                    String content = String.valueOf(responseMediaType.getExample());
                    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                    new FileOutputStream(responseFile, false).write(bytes, 0, bytes.length);

                    generateLogicSteps(code, responseDecisionSteps, responseDecisionRules, pathDirectory, numericCodeRegex, responseMediaTypeName, null, fileReference);

                    return content;
                }
            } else {
                System.out.println("### unsupported media type ");
                System.out.println("creating http response without file");

                generateLogicSteps(code, responseDecisionSteps, responseDecisionRules, pathDirectory, numericCodeRegex, responseMediaTypeName, NO_CONTENT, null);
            }
        } else {
            System.out.println("### no media type ");
            System.out.println("creating http response without file");

            generateLogicSteps(code, responseDecisionSteps, responseDecisionRules, pathDirectory, numericCodeRegex, PLAIN_TEXT, NO_CONTENT, null);
        }

        return null;
    }

    /**
     * @param code
     * @param responseDecisionSteps
     * @param responseDecisionRules
     * @param pathDirectory
     * @param numericCodeRegex
     * @param responseMediaTypeName
     * @param value
     * @param file
     */
    private static void generateLogicSteps(String code, List<Step> responseDecisionSteps, List<Rule> responseDecisionRules, File pathDirectory, Matcher numericCodeRegex, String responseMediaTypeName, String value, String file) {
        if (StringUtils.isNumeric(code)) {
            System.out.println("creating http response");
            responseDecisionSteps.add(new CreateHttpResponse(pathDirectory.getName() + "-" + code, value, file, parseLong(code), responseMediaTypeName));
            System.out.println("creating decision rule");
            responseDecisionRules.add(new Rule(pathDirectory.getName() + "-" + code, singletonList(new Condition(REQUEST_HEADERS_MOCK_RESPONSE_CODE, code, null, null))));
        } else if(numericCodeRegex.matches()) {
            String firstDigit = numericCodeRegex.group(1);
            responseDecisionSteps.add(new Sequence(pathDirectory.getName() + "-" + code, Arrays.asList(
                    new CreateParameter(RESPONSE_METADATA_CONTENT_TYPE, null, responseMediaTypeName, null, false),
                    new CreateParameter(RESPONSE_METADATA_STATUS_CODE, REQUEST_HEADERS_MOCK_RESPONSE_CODE, firstDigit + ZERO_ZERO, null, false),
                    new CreateResponse(pathDirectory.getName() + "-" + code + "-response", value, file)
            )));
            responseDecisionRules.add(new Rule(pathDirectory.getName() + "-" + code, singletonList(new Condition(REQUEST_HEADERS_MOCK_RESPONSE_CODE, null, "^[" + firstDigit + "][0-9][0-9]$", null))));
        } else {
            responseDecisionSteps.add(new Sequence(pathDirectory.getName() + "-" + code, Arrays.asList(
                    new CreateParameter(RESPONSE_METADATA_CONTENT_TYPE, null, responseMediaTypeName, null, false),
                    new CreateParameter(RESPONSE_METADATA_STATUS_CODE, REQUEST_HEADERS_MOCK_RESPONSE_CODE, OK, null, false),
                    new CreateResponse(pathDirectory.getName() + "-" + code + "-response", value, file)
            )));
        }
    }

    /**
     * *
     * @param path
     * @param parameters
     * @return
     */
    private static Condition buildPathCondition(String path, List<Parameter> parameters) {
        if (path.contains("{") && path.contains("}")) {
            System.out.println("- parsing inline parameters from path");
            List<String> separators = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Boolean> requireds = new ArrayList<>();

            while (path.contains("{")) {
                separators.add(path.substring(0, path.indexOf("{")));
                processPathParameter(path.substring(path.indexOf("{") + 1, path.indexOf("}")), parameters, types, requireds);
                path = path.substring(path.indexOf("}") + 1);
            }

            separators.add(path);

            System.out.println("- creating regex for path");
            StringBuilder builder = new StringBuilder("^");

            while (!separators.isEmpty()) {
                builder.append(separators.remove(0).replace("/", "\\/"));

                if (!types.isEmpty()) {
                    builder.append("([").append(INTEGER.equals(types.remove(0)) ? NUMERIC : ALPHA_NUMERIC).append("]").append(requireds.remove(0) ? "+" : "*").append(")");
                }
            }

            String regex = builder.append("$").toString();
            return new Condition(REQUEST_METADATA_REQUEST_URI, null, regex, null);
        }

        return new Condition(REQUEST_METADATA_REQUEST_URI, path, null, null);
    }

    /**
     * *
     * @param path
     * @param parameters
     * @return
     */
    private static List<Step> buildCreateParameterStepsForPath(String path, List<Parameter> parameters) {
        if (path.contains("{") && path.contains("}")) {
            System.out.println("- parsing inline parameters from path");
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

            System.out.println("- creating extract format for path");
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
            System.out.println("- creating create-parameter steps for path params");
            List<Step> createParameters = new ArrayList<>();

            for (int i = 0; i < ids.size(); i++) {
                createParameters.add(new CreateParameter(ids.get(i), REQUEST_METADATA_REQUEST_URI, null, new ExtractProcessor(extractFormat, i), !requireds.get(i)));
            }

            return createParameters;
        }

        return new ArrayList<>();
    }

    /**
     * *
     * @param parameters
     * @return
     */
    private static List<Step> buildCreateParameterStepsForQuery(List<Parameter> parameters) {
        return parameters.stream().filter(p -> "query".equals(p.getIn())).map(p -> new CreateParameter(p.getName(), REQUEST_METADATA_QUERY_STRING, null, new QueryParamProcessor(p.getName()), p.getRequired() == null || !p.getRequired())).collect(Collectors.toList());
    }

    /**
     * *
     * @param id
     * @param parameters
     * @param types
     * @param requireds
     */
    private static void processPathParameter(String id, List<Parameter> parameters, List<String> types, List<Boolean> requireds) {
        if (parameters != null) {
            Optional<Parameter> firstParam = parameters.stream().filter(p -> id.equals(p.getName()) && "path".equals(p.getIn())).findFirst();
            types.add(firstParam.isPresent() ? firstParam.get().getSchema().getType() : STRING);
            requireds.add(firstParam.isPresent() ? firstParam.get().getRequired() : true);
        } else {
            types.add(STRING);
            requireds.add(true);
        }
    }
}
