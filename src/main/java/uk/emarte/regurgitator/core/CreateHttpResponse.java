package uk.emarte.regurgitator.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateHttpResponse implements Step {
    @JsonProperty private final String kind = "create-http-response";
    @JsonProperty private final String id;
    @JsonProperty private final String value;
    @JsonProperty("status-code") private final long statusCode;
    @JsonProperty("content-type") private final String contentType;

    public CreateHttpResponse(String id, String value, long statusCode, String contentType) {
        this.id = id;
        this.value = value;
        this.statusCode = statusCode;
        this.contentType = contentType;
    }
}
