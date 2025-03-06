/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

public class Variable implements Comparable<Variable> {
    String key;
    String value;
    String type;
    Boolean disabled = false;
    Boolean system = false;

    public Variable(String key, String value) {
        this.key = key;
        this.value = value;
        this.type = "string";
    }

    public Variable(String key, Integer value) {
        this.key = key;
        this.value = String.valueOf(value);
        this.type = "number";
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public Boolean getSystem() {
        return system;
    }

    @Override
    public int compareTo(Variable o) {
        return o.key.hashCode() - key.hashCode() + o.value.hashCode() - value.hashCode();
    }
}
