/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

public class QueryParam {
    private final String key;
    private final String value;
    private final Boolean disabled;

    public QueryParam(String key, String value, Boolean disabled) {
        this.key = key;
        this.value = value;
        this.disabled = disabled;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Boolean getDisabled() {
        return disabled;
    }
}
