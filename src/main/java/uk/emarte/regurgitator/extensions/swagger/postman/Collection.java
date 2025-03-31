/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

public class Collection {
    public static final String PLACEHOLDER = "PLACEHOLDER";
    public static final String BASE_URL_KEY = "base_url";
    public static final String BASE_URL_VALUE = "http://{{hostname}}:{{port}}";

    private final Info info;
    private final ItemGroup[] item;
    private final Variable[] variable;

    public Collection(Info info, ItemGroup[] item, Variable[] variable) {
        this.info = info;
        this.item = item;
        this.variable = new Variable[variable.length + 3];
        System.arraycopy(variable, 0, this.variable, 3, variable.length);
        this.variable[0] = new Variable("hostname", "HOSTNAME");
        this.variable[1] = new Variable("port", "PORT");
        this.variable[2] = new Variable(BASE_URL_KEY, BASE_URL_VALUE);
    }

    public Info getInfo() {
        return info;
    }

    public ItemGroup[] getItem() {
        return item;
    }

    public Variable[] getVariable() {
        return variable;
    }
}
