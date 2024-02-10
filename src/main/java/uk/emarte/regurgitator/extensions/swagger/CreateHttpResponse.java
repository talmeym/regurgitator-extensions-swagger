/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
class CreateHttpResponse implements Step {
    @JsonProperty private final String kind = "create-http-response";
    @JsonProperty private final String id;
    @JsonProperty private final String value;
    @JsonProperty private final String file;
    @JsonProperty("status-code") private final long statusCode;
    @JsonProperty("content-type") private final String contentType;

    CreateHttpResponse(String id, String value, String file, long statusCode, String contentType) {
        this.id = id;
        this.value = value;
        this.file = file;
        this.statusCode = statusCode;
        this.contentType = contentType;
    }
}
