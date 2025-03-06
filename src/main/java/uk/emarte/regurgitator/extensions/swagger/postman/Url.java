/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

import java.util.List;

public class Url {
    private final String raw;
    private final String[] host;
    private final String[] path;
    private final List<QueryParam> query;

    public Url(String raw, List<QueryParam> queryParams) {
        this.raw = raw + "?" + formatQueryParams(queryParams);
        this.host = new String[]{raw.substring(0, raw.indexOf("/"))};
        this.path = raw.substring(raw.indexOf("/") + 1).split("/");
        this.query = queryParams;

    }

    private String formatQueryParams(List<QueryParam> queryParams) {
        StringBuilder stringBuilder = new StringBuilder(queryParams.get(0).getKey()).append("=").append(queryParams.get(0).getValue());

        for(int i = 1; i < queryParams.size(); i++) {
            stringBuilder.append("&").append(queryParams.get(i).getKey()).append("=").append(queryParams.get(i).getValue());
        }

        return stringBuilder.toString();
    }

    public String getRaw() {
        return raw;
    }

    public String[] getHost() {
        return host;
    }

    public String[] getPath() {
        return path;
    }

    public List<QueryParam> getQuery() {
        return query;
    }
}
