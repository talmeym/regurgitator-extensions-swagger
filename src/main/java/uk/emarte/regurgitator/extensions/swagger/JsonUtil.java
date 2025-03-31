/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Collections.EMPTY_MAP;
import static java.util.Collections.singletonList;

public class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String STRING = "string", INTEGER = "integer", DOUBLE = "double", FLOAT = "float", NUMBER = "number", OBJECT = "object", BOOLEAN = "boolean", INT_32 = "int32", INT_64 = "int64", ARRAY = "array";

    private static final String EX_STR = "abcdefgh", EX_NUM = "1";

    static String saveToJson(Object object, FileOutputStream fileOutputStream) throws IOException {
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(fileOutputStream, object);
        return MAPPER.writeValueAsString(object);
    }

    @SuppressWarnings("rawtypes")
    static Object buildJsonObject(Schema<?> schema, Components components, int level) {
        if(level >= 20) {
            return EMPTY_MAP;
        }

        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            schema = components.getSchemas().get(ref.contains("/") ? ref.substring(ref.lastIndexOf("/") + 1) : ref);
        }

        if(schema.getExample() != null) {
            return schema.getExample();
        }

        if (schema.getProperties() != null || schema.getAdditionalProperties() != null) {
            Map<String, Object> objectContents = new LinkedHashMap<>();
            Map<String, Schema> properties = schema.getProperties() != null ? schema.getProperties() : ((ObjectSchema) schema.getAdditionalProperties()).getProperties();

            for (String name : properties.keySet()) {
                Schema<?> propertySchema = properties.get(name);
                String type = propertySchema.getType();

                if (ARRAY.equals(type)) {
                    objectContents.put(name, singletonList(buildJsonObject(propertySchema.getItems(), components, level + 1)));
                } else if (INTEGER.equals(type)) {
                    if(propertySchema.getFormat() != null) {
                        switch (propertySchema.getFormat()) {
                            case INT_64: objectContents.put(name, getNumberObject(propertySchema, Long::parseLong, BigDecimal::longValue)); break;
                            case INT_32:
                            default: objectContents.put(name, getNumberObject(propertySchema, Integer::parseInt, BigDecimal::intValue));
                        }
                    } else {
                        objectContents.put(name, getNumberObject(propertySchema, Integer::parseInt, BigDecimal::intValue));
                    }
                } else if (NUMBER.equals(type)) {
                    if(propertySchema.getFormat() != null) {
                        switch (propertySchema.getFormat()) {
                            case FLOAT: objectContents.put(name, getNumberObject(propertySchema, Float::parseFloat, BigDecimal::floatValue)); break;
                            case DOUBLE: objectContents.put(name, getNumberObject(propertySchema, Double::parseDouble, BigDecimal::doubleValue)); break;
                            default: objectContents.put(name, getNumberObject(propertySchema, Integer::parseInt, BigDecimal::intValue));
                        }
                    } else {
                        objectContents.put(name, getNumberObject(propertySchema, Integer::parseInt, BigDecimal::intValue));
                    }
                } else if (OBJECT.equals(type)) {
                    objectContents.put(name, buildJsonObject(propertySchema, components, level + 1));
                } else if (BOOLEAN.equals(type)) {
                    objectContents.put(name, Boolean.parseBoolean("" + (propertySchema.getExample() != null ? propertySchema.getExample() : false)));
                } else if (propertySchema.get$ref() != null) {
                    String ref = propertySchema.get$ref();
                    objectContents.put(name, buildJsonObject(components.getSchemas().get(ref.contains("/") ? ref.substring(ref.lastIndexOf("/") + 1) : ref), components, level + 1));
                } else { // assume string
                    objectContents.put(name, getStringValue(propertySchema));
                }
            }

            return objectContents;
        } else if (ARRAY.equals(schema.getType())) {
            return singletonList(buildJsonObject(schema.getItems(), components, level + 1));
        }

        return getStringValue(schema);
    }

    private static <TYPE> Object getNumberObject(Schema<?> propertySchema, Function<String, TYPE> objectFunction, Function<BigDecimal, TYPE> decimalFunction) {
        if(propertySchema.getExample() != null) {
            return objectFunction.apply("" + propertySchema.getExample());
        }

        if(propertySchema.getEnum() != null && propertySchema.getEnum().size() > 0) {
            return "" + propertySchema.getEnum().get(0);
        }

        if(propertySchema.getMinimum() != null) {
            return decimalFunction.apply(propertySchema.getMinimum());
        }

        return objectFunction.apply(EX_NUM);
    }

    private static String getStringValue(Schema<?> propertySchema) {
        if(propertySchema.getExample() != null) {
            return "" + propertySchema.getExample();
        }

        if(propertySchema.getEnum() != null && propertySchema.getEnum().size() > 0) {
            return "" + propertySchema.getEnum().get(0);
        }

        if(propertySchema.getFormat() != null) {
            switch(propertySchema.getFormat()) {
                case "date-time": return new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").format(new Date());
                case "uuid": return UUID.randomUUID().toString();
            }
        }

        return EX_STR;
    }
}
