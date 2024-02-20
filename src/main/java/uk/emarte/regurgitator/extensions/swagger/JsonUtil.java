/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.IOException;

public class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static void saveToJson(Object object, FileOutputStream fileOutputStream) throws IOException {
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(fileOutputStream, object);
    }
}
