/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

public class Info {
    private final String name;
    private final String description;
    private final String version;
    private final String schema;

    public Info(String name, String description, String version, String schema) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.schema = schema;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public String getSchema() {
        return schema;
    }
}
