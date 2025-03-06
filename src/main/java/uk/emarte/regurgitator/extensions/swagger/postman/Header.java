/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

public class Header {
    private final String key;
    private final String value;
    private final boolean disabled;
    private final String description;

    public Header(String key, String value, boolean disabled, String description) {
        this.key = key;
        this.value = value;
        this.disabled = disabled;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public String getDescription() {
        return description;
    }
}
