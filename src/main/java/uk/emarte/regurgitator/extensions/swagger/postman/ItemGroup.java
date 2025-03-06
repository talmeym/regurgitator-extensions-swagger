/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

public class ItemGroup implements Items {
    private final String name;
    private final String description;
    private final Items[] item;

    public ItemGroup(String name, String description, Items[] item) {
        this.name = name;
        this.description = description;
        this.item = item;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Items[] getItem() {
        return item;
    }
}
