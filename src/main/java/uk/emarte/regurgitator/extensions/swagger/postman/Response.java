/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

public class Response {
    private final String body;
    private final StatusCode statusCode;

    public Response(String body, Integer code) {
        this.body = body;
        this.statusCode = StatusCode.forCode(code);
    }

    public String getName() {
        return statusCode.getCode() + " Response";
    }

    public String getBody() {
        return body;
    }

    public String getStatus() {
        return statusCode.name();
    }

    public Integer getCode() {
        return statusCode.getCode();
    }
}
