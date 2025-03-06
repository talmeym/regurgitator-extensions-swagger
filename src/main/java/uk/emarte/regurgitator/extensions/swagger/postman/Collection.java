/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

public class Collection {
    public static final String PLACEHOLDER = "PLACEHOLDER";
    public static final String BASE_URL_KEY = "base_url";
    public static final String BASE_URL_VALUE = "http://localhost:{{port}}";

    private final Info info;
    private final ItemGroup[] item;
    private final Variable[] variable;

    public Collection(Info info, ItemGroup[] item, Variable[] variable, Integer port) {
        this.info = info;
        this.item = item;
        this.variable = new Variable[variable.length + 2];
        System.arraycopy(variable, 0, this.variable, 2, variable.length);
        this.variable[0] = new Variable("port", port);
        this.variable[1] = new Variable(BASE_URL_KEY, BASE_URL_VALUE);
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
