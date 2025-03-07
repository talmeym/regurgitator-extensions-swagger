/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

public class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String STRING = "string", INTEGER = "integer", DOUBLE = "double", FLOAT = "float", NUMBER = "number", OBJECT = "object", BOOLEAN = "boolean", INT_32 = "int32", INT_64 = "int64", ARRAY = "array";
    private static final String FOOBAR = "foobar";
    private static final String ZERO = "0";

    static String saveToJson(Object object, FileOutputStream fileOutputStream) throws IOException {
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(fileOutputStream, object);
        return MAPPER.writeValueAsString(object);
    }

    @SuppressWarnings("rawtypes")
    static Object buildJsonObject(Schema<?> schema, Map<String, Schema> componentSchemas) {
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

                if (ARRAY.equals(type)) {

                    objectContents.put(name, singletonList(buildJsonObject(propertySchema.getItems(), componentSchemas)));
                } else if (INTEGER.equals(type)) {
                    if(INT_64.equals(propertySchema.getFormat())) {
                        objectContents.put(name, Long.parseLong(propertySchema.getExample() != null ? "" + propertySchema.getExample() : ZERO));
                    } else {
                        objectContents.put(name, Integer.parseInt(propertySchema.getExample() != null ? "" + propertySchema.getExample() : ZERO));
                    }
                } else if (NUMBER.equals(type)) {
                    if(propertySchema.getFormat() != null) {
                        switch (propertySchema.getFormat()) {
                            case FLOAT: objectContents.put(name, Float.parseFloat(propertySchema.getExample() != null ? "" + propertySchema.getExample() : ZERO)); break;
                            case DOUBLE: objectContents.put(name, Double.parseDouble(propertySchema.getExample() != null ? "" + propertySchema.getExample() : ZERO)); break;
                            default: objectContents.put(name, Integer.parseInt(propertySchema.getExample() != null ? "" + propertySchema.getExample() : ZERO));
                        }
                    } else {
                        objectContents.put(name, Integer.parseInt(propertySchema.getExample() != null ? "" + propertySchema.getExample() : ZERO));
                    }
                } else if (OBJECT.equals(type)) {
                    objectContents.put(name, buildJsonObject(propertySchema, componentSchemas));
                } else if (BOOLEAN.equals(type)) {
                    objectContents.put(name, Boolean.parseBoolean("" + (propertySchema.getExample() != null ? propertySchema.getExample() : true)));
                } else if (propertySchema.get$ref() != null) {
                    String $ref = propertySchema.get$ref();
                    $ref = $ref.contains("/") ? $ref.substring($ref.lastIndexOf("/") + 1) : $ref;
                    objectContents.put(name, buildJsonObject(componentSchemas.get($ref), componentSchemas));
                } else { // assume string
                    objectContents.put(name, "" + (propertySchema.getExample() != null ? propertySchema.getExample() : FOOBAR));
                }
            }

            return objectContents;
        } else if (ARRAY.equals(schema.getType())) {
            return singletonList(buildJsonObject(schema.getItems(), componentSchemas));
        } else if (STRING.equals(schema.getType())) {
            return schema.getExample();
        }

        throw new IllegalStateException("Cannot construct object for schema");
    }
}
