/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static java.lang.Long.parseLong;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * Generates regurgitator configuration from open api (v3) 'swagger' files
 */
public class ConfigurationGenerator {
    private static final String USAGE_TEXT = "Usage: java uk.emarte.regurgitator.extensions.swagger.ConfigurationGenerator swaggerFile.[json|yaml] /outputDirectory";
    private static final String OK = "200";
    private static final String PLAIN_TEXT = "text/plain";
    private static final String NUMERIC = "0-9";
    private static final String ALPHA_NUMERIC = "A-Za-z0-9-";
    private static final String REQUEST_METADATA_REQUEST_URI = "request-metadata:request-uri";
    private static final String REQUEST_METADATA_METHOD = "request-metadata:method";
    private static final String REQUEST_HEADERS_MOCK_RESPONSE_CODE = "request-headers:mock-response-code";
    private static final String SLASH_SUBSTITUTE = "%";
    private static final String CURLY_BRACE_SUBSTITUTE = "^";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_XML = "application/xml";
    public static final String JSON = "json";
    public static final String XML = "xml";

    private enum Method {
        GET, PUT, POST, PATCH, DELETE, HEAD
    }

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
     * @see ConfigurationGenerator#generateConfiguration(File, File, String)
     */
    @SuppressWarnings("rawtypes")
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

            Map<String, Schema> componentSchemas = openAPI.getComponents() != null ? openAPI.getComponents().getSchemas() : null;

            System.out.println("processing " + paths.size() + " route(s)");
            for (String path : paths.keySet()) {
                PathItem pathItem = paths.get(path);
                processOperation(pathItem.getGet(), path, pathItem, Method.GET, steps, rules, componentSchemas, outputDirectory, outputType);
                processOperation(pathItem.getPut(), path, pathItem, Method.PUT, steps, rules, componentSchemas, outputDirectory, outputType);
                processOperation(pathItem.getPost(), path, pathItem, Method.POST, steps, rules, componentSchemas, outputDirectory, outputType);
                processOperation(pathItem.getPatch(), path, pathItem, Method.PATCH, steps, rules, componentSchemas, outputDirectory, outputType);
                processOperation(pathItem.getDelete(), path, pathItem, Method.DELETE, steps, rules, componentSchemas, outputDirectory, outputType);
                processOperation(pathItem.getHead(), path, pathItem, Method.HEAD, steps, rules, componentSchemas, outputDirectory, outputType);
            }

            System.out.println("creating routing default step");
            String defaultStepId = "step-" + (steps.size() + 1);
            steps.add(new CreateHttpResponse(defaultStepId, "regurgitator : " + "unmapped operation", null, 500L, PLAIN_TEXT));

            System.out.println("creating routing decision");
            Decision decision = new Decision("decision-1", steps, rules, defaultStepId);

            System.out.println("### saving routing configuration");
            outputType.save(new RegurgitatorConfiguration(singletonList(decision)), new FileOutputStream(new File(outputDirectory, "regurgitator-configuration." + outputType), false));
        } catch (Exception e) {
            throw new GenerationException("Error generating configuration", e);
        }
    }

    @SuppressWarnings({"rawtypes", "ResultOfMethodCallIgnored"})
    private static void processOperation(Operation operation, String path, PathItem pathItem, Method method, List<Step> steps, List<Rule> rules, Map<String, Schema> componentSchemas, File outputDirectory, OutputType outputType) throws IOException, ParserConfigurationException, TransformerException {
        if (operation != null) {
            System.out.println("processing route " + method + " " + path);

            File pathDirectory = new File(outputDirectory, method + path.replace("/", SLASH_SUBSTITUTE).replace("{", CURLY_BRACE_SUBSTITUTE).replace("}", CURLY_BRACE_SUBSTITUTE));
            pathDirectory.mkdirs();
            RequestBody requestBody = operation.getRequestBody();

            if (requestBody != null) {
                System.out.println("### request");
                processRequest(requestBody.getContent(), componentSchemas, pathDirectory);
            }

            ApiResponses responses = operation.getResponses();
            boolean responseCreated = false;

            List<Step> responseDecisionSteps = new ArrayList<>();
            List<Rule> responseDecisionRules = new ArrayList<>();

            if (responses != null) {
                for (String code : responses.keySet()) {
                    boolean result = processResponse(code, responses.get(code).getContent(), responseDecisionSteps, responseDecisionRules, componentSchemas, pathDirectory);
                    responseCreated = responseCreated || result;
                }
            }

            System.out.println("creating path param extract steps");
            List<Step> stepsForConfiguration = new ArrayList<>(buildCreateParameterStepsForPath(path, pathItem.getParameters() != null ? pathItem.getParameters() : operation.getParameters()));

            if (responseCreated) {
                System.out.println("creating route response decision");
                Optional<String> optStatusCodeToUse = responses.keySet().stream().filter(sc -> sc.length() == 3 && sc.startsWith("2")).findFirst();
                String statusCodeToUse = optStatusCodeToUse.orElseGet(() -> responses.keySet().iterator().next());
                stepsForConfiguration.add(new Decision(null, responseDecisionSteps, responseDecisionRules, pathDirectory.getName() + "-" + statusCodeToUse));
            } else {
                System.out.println("creating route response step, no responses generated");
                stepsForConfiguration.add(new CreateHttpResponse(null, "regurgitator : " + method + " " + path + " : " + OK + " " + PLAIN_TEXT, null, parseLong(OK), PLAIN_TEXT));
            }

            RegurgitatorConfiguration regurgitatorConfiguration = new RegurgitatorConfiguration(stepsForConfiguration);
            File configFile = new File(pathDirectory, "regurgitator-configuration." + outputType);
            System.out.println("### generating config file: " + pathDirectory.getName() + "/" + configFile.getName());
            outputType.save(regurgitatorConfiguration, new FileOutputStream(configFile, false));

            System.out.println("creating sequence ref step");
            String stepId = "step-" + (steps.size() + 1);
            steps.add(new SequenceRef(stepId, "classpath:/" + pathDirectory.getName() + "/regurgitator-configuration." + outputType));

            System.out.println("creating path condition");
            Condition pathCondition = buildPathCondition(path, pathItem.getParameters() != null ? pathItem.getParameters() : operation.getParameters());
            System.out.println("creating method condition");
            Condition methodCondition = new Condition(REQUEST_METADATA_METHOD, method.name(), null);
            System.out.println("creating routing rule");
            rules.add(new Rule(stepId, asList(methodCondition, pathCondition)));
        }
    }

    private static void processRequest(Content requestContent, Map<String, Schema> componentSchemas, File pathDirectory) throws IOException, ParserConfigurationException, TransformerException {
        System.out.println("### request");

        if (requestContent != null && requestContent.size() > 0) {
            String requestMediaTypeName = requestContent.keySet().iterator().next();
            System.out.println("### media type " + requestMediaTypeName);

            if (APPLICATION_JSON.equals(requestMediaTypeName) || APPLICATION_XML.equals(requestMediaTypeName)) {
                MediaType requestMediaType = requestContent.get(requestMediaTypeName);

                if (requestMediaType.getSchema() != null && (requestMediaType.getSchema().getProperties() != null || requestMediaType.getSchema().get$ref() != null || requestMediaType.getSchema().getAdditionalProperties() != null || "array".equals(requestMediaType.getSchema().getType()))) {
                    File requestFile = new File(pathDirectory, pathDirectory.getName() + "-REQ." + (APPLICATION_JSON.equals(requestMediaTypeName) ? JSON : XML));
                    System.out.println("### generating request file: " + requestFile.getName());

                    if(APPLICATION_JSON.equals(requestMediaTypeName)) {
                        JsonUtil.saveToJson(buildJsonObject(requestMediaType.getSchema(), componentSchemas), new FileOutputStream(requestFile, false));
                    } else {
                        Document document = XmlUtil.newDocument();
                        XmlUtil.saveElement(buildXmlObject(null, requestMediaType.getSchema(), document, componentSchemas), document, new FileOutputStream(requestFile, false));
                    }
                }
            } else {
                System.out.println("### unsupported request media type: " + requestMediaTypeName);
            }
        } else {
            System.out.println("### request has no content !?");
        }
    }

    private static boolean processResponse(String code, Content responseContent, List<Step> responseDecisionSteps, List<Rule> responseDecisionRules, Map<String, Schema> componentSchemas, File pathDirectory) throws IOException, ParserConfigurationException, TransformerException {
        System.out.println("### " + code + " response");

        if (responseContent != null && responseContent.size() > 0) {
            String responseMediaTypeName = responseContent.keySet().iterator().next();
            System.out.println("### media type " + responseMediaTypeName);

            if (APPLICATION_JSON.equals(responseMediaTypeName) || APPLICATION_XML.equals(responseMediaTypeName)) {
                MediaType responseMediaType = responseContent.get(responseMediaTypeName);

                if (responseMediaType.getSchema() != null && (responseMediaType.getSchema().getProperties() != null || responseMediaType.getSchema().get$ref() != null || responseMediaType.getSchema().getAdditionalProperties() != null || "array".equals(responseMediaType.getSchema().getType()))) {
                    File responseFile = new File(pathDirectory, pathDirectory.getName() + "-" + code + "." + (APPLICATION_JSON.equals(responseMediaTypeName) ? JSON : XML));
                    System.out.println("### generating response file: " + responseFile.getName());

                    if(APPLICATION_JSON.equals(responseMediaTypeName)) {
                        JsonUtil.saveToJson(buildJsonObject(responseMediaType.getSchema(), componentSchemas), new FileOutputStream(responseFile, false));
                    } else {
                        Document document = XmlUtil.newDocument();
                        XmlUtil.saveElement(buildXmlObject(null, responseMediaType.getSchema(), document, componentSchemas), document, new FileOutputStream(responseFile, false));
                    }

                    String fileReference = "classpath:/" + pathDirectory.getName() + "/" + responseFile.getName();
                    System.out.println("creating http response for file");
                    responseDecisionSteps.add(new CreateHttpResponse(pathDirectory.getName() + "-" + code, null, fileReference, parseLong(code), responseMediaTypeName));
                    System.out.println("creating decision rule for file");
                    responseDecisionRules.add(new Rule(pathDirectory.getName() + "-" + code, singletonList(new Condition(REQUEST_HEADERS_MOCK_RESPONSE_CODE, code, null))));
                    return true;
                }
            }
        } else {
            System.out.println("### no media type ");
            System.out.println("creating http response without file");
            responseDecisionSteps.add(new CreateHttpResponse(pathDirectory.getName() + "-" + code, "no content", null, parseLong(code), PLAIN_TEXT));
            responseDecisionRules.add(new Rule(pathDirectory.getName() + "-" + code, singletonList(new Condition(REQUEST_HEADERS_MOCK_RESPONSE_CODE, code, null))));
        }

        return false;
    }

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
    private static Object buildJsonObject(Schema<?> schema, Map<String, Schema> componentSchemas) {
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

                    objectContents.put(name, singletonList(buildJsonObject(propertySchema.getItems(), componentSchemas)));
                } else if ("integer".equals(type)) {
                    if ("int32".equals(propertySchema.getFormat()) || propertySchema.getFormat() == null) {
                        objectContents.put(name, Integer.parseInt(propertySchema.getExample() != null ? "" + propertySchema.getExample() : "0"));
                    }
                    if ("int64".equals(propertySchema.getFormat())) {
                        objectContents.put(name, Long.parseLong(propertySchema.getExample() != null ? "" + propertySchema.getExample() : "0"));
                    }
                } else if ("number".equals(type)) {
                    if (propertySchema.getFormat() == null) {
                        objectContents.put(name, Integer.parseInt(propertySchema.getExample() != null ? "" + propertySchema.getExample() : "0"));
                    }
                    if ("float".equals(propertySchema.getFormat())) {
                        objectContents.put(name, Float.parseFloat(propertySchema.getExample() != null ? "" + propertySchema.getExample() : "0"));
                    }
                    if ("double".equals(propertySchema.getFormat())) {
                        objectContents.put(name, Double.parseDouble(propertySchema.getExample() != null ? "" + propertySchema.getExample() : "0"));
                    }
                } else if ("object".equals(type)) {
                    objectContents.put(name, buildJsonObject(propertySchema, componentSchemas));
                } else if ("boolean".equals(type)) {
                    objectContents.put(name, Boolean.parseBoolean("" + (propertySchema.getExample() != null ? propertySchema.getExample() : true)));
                } else if (propertySchema.get$ref() != null) {
                    String $ref = propertySchema.get$ref();
                    $ref = $ref.contains("/") ? $ref.substring($ref.lastIndexOf("/") + 1) : $ref;
                    objectContents.put(name, buildJsonObject(componentSchemas.get($ref), componentSchemas));
                } else { // assume string
                    objectContents.put(name, "" + (propertySchema.getExample() != null ? propertySchema.getExample() : "foobar"));
                }
            }

            return objectContents;
        } else if ("array".equals(schema.getType())) {
            return singletonList(buildJsonObject(schema.getItems(), componentSchemas));
        } else if ("string".equals(schema.getType())) {
            return schema.getExample();
        }

        throw new IllegalStateException("Cannot construct object for schema");
    }

    @SuppressWarnings("rawtypes")
    private static Element buildXmlObject(String schemaName, Schema<?> schema, Document document, Map<String, Schema> componentSchemas) {
        if (schema.get$ref() != null) {
            String $ref = schema.get$ref();
            $ref = $ref.contains("/") ? $ref.substring($ref.lastIndexOf("/") + 1) : $ref;
            schema = componentSchemas.get($ref);
        }

        XML xml = schema.getXml();
        String elementName = xml != null ? xml.getName() : schema.getName() != null ? schema.getName() : schemaName;
        String prefix = xml != null ? xml.getPrefix() : null;
        String namespace = xml != null ? xml.getNamespace() : null;

        if (schema.getProperties() != null || schema.getAdditionalProperties() != null) {
            if(elementName == null) {
                throw new IllegalStateException("no element name defined");
            }

            Element element = document.createElement(prefix != null && prefix.length() > 0 ? prefix + ":" + elementName : elementName);

            if(prefix != null && namespace != null) {
                element.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:" + prefix, namespace);
            }

            Map<String, Schema> properties = schema.getProperties() != null ? schema.getProperties() : ((ObjectSchema) schema.getAdditionalProperties()).getProperties();

            for (String name : properties.keySet()) {
                Schema<?> propertySchema = properties.get(name);
                String type = propertySchema.getType();
                XML propertyXml = propertySchema.getXml();
                String xmlName = propertyXml != null && propertyXml.getName() != null ? propertyXml.getName() : name;

                if ("array".equals(type)) {
                    boolean wrapped = propertyXml != null && propertyXml.getWrapped() != null && propertyXml.getWrapped();
                    Element elementToUse = element;

                    if(wrapped) {
                        Element child = document.createElement(xmlName);
                        element.appendChild(child);
                        elementToUse = child;
                    }

                    io.swagger.v3.oas.models.media.XML itemsXml = propertySchema.getItems().getXml();
                    String itemName = itemsXml != null && itemsXml.getName() != null ? itemsXml.getName() : name;
                    elementToUse.appendChild(buildXmlObject(itemName, propertySchema.getItems(), document, componentSchemas));
                } else if ("integer".equals(type)) {
                    String value = "";

                    if ("int32".equals(propertySchema.getFormat()) || propertySchema.getFormat() == null) {
                        value = "" + Integer.parseInt(propertySchema.getExample() != null ? "" + propertySchema.getExample() : "0");
                    }
                    if ("int64".equals(propertySchema.getFormat())) {
                        value = "" + Long.parseLong(propertySchema.getExample() != null ? "" + propertySchema.getExample() : "0");
                    }

                    xmlChildElementOrAttribute(xmlName, value, propertyXml, document, element);
                } else if ("number".equals(type)) {
                    String value = "";

                    if (propertySchema.getFormat() == null) {
                        value = "" + Integer.parseInt(propertySchema.getExample() != null ? "" + propertySchema.getExample() : "0");
                    }
                    if ("float".equals(propertySchema.getFormat())) {
                        value = "" + Float.parseFloat(propertySchema.getExample() != null ? "" + propertySchema.getExample() : "0");
                    }
                    if ("double".equals(propertySchema.getFormat())) {
                        value = "" + Double.parseDouble(propertySchema.getExample() != null ? "" + propertySchema.getExample() : "0");
                    }

                    xmlChildElementOrAttribute(xmlName, value, propertyXml, document, element);
                } else if ("object".equals(type)) {
                    element.appendChild(buildXmlObject(xmlName, propertySchema, document, componentSchemas));
                } else if ("boolean".equals(type)) {
                    String value = "" + Boolean.parseBoolean("" + (propertySchema.getExample() != null ? propertySchema.getExample() : true));
                    xmlChildElementOrAttribute(xmlName, value, propertyXml, document, element);
                } else if (propertySchema.get$ref() != null) {
                    element.appendChild(buildXmlObject(xmlName, propertySchema, document, componentSchemas));
                } else { // assume string
                    String value = "" + (propertySchema.getExample() != null ? propertySchema.getExample() : "foobar");
                    xmlChildElementOrAttribute(xmlName, value, propertyXml, document, element);
                }
            }

            return element;
        } else if ("string".equals(schema.getType()) || "integer".equals(schema.getType())) {
            Element element = document.createElement(prefix != null && prefix.length() > 0 ? prefix + ":" + elementName : elementName);
            element.appendChild(document.createTextNode("" + (schema.getExample() != null ? schema.getExample() : ("integer".equals(schema.getType()) ? "0" : "foobar"))));
            return element;
        }

        throw new IllegalStateException("Cannot construct object for schema");
    }

    private static void xmlChildElementOrAttribute(String name, String value, XML propertyXml, Document document, Element element) {
        if (propertyXml != null && propertyXml.getAttribute()) {
            element.setAttribute(name, value);
        } else {
            Element child = document.createElement(name);
            element.appendChild(child);
            child.appendChild(document.createTextNode(value));
        }
    }
}
