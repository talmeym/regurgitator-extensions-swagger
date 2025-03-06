/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

import uk.emarte.regurgitator.extensions.swagger.Method;

import java.util.List;

import static uk.emarte.regurgitator.extensions.swagger.postman.Collection.BASE_URL_KEY;

public class Request {
    private final String name;
    private final String description;
    private final Object url;
    private final Method method;
    private final Header[] header;
    private final Body body;

    public Request(String name, String description, String url, Method method, Header[] header, Body body, List<QueryParam> queryParams) {
        this.name = name;
        this.description = description;
        url = "{{" + BASE_URL_KEY + "}}" + url.replaceAll("\\{", "{{").replaceAll("}", "}}");
        this.url = queryParams.size() > 0 ? new Url(url, queryParams) : url;
        this.method = method;
        this.header = header;
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Object getUrl() {
        return url;
    }

    public Method getMethod() {
        return method;
    }

    public Header[] getHeader() {
        return header;
    }

    public Body getBody() {
        return body;
    }
}

