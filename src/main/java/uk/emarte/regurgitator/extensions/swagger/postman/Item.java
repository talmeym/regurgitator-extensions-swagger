/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

public class Item implements Items {
    private final String name;
    private final Request request;
    private final Response[] response;

    public Item(String name, Request request, Response[] response) {
        this.name = name;
        this.request = request;
        this.response = response;
    }

    public String getName() {
        return name;
    }

    public Request getRequest() {
        return request;
    }

    public Response[] getResponse() {
        return response;
    }
}
