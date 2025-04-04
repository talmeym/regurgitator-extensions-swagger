/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

public enum StatusCode {
    CONTINUE(100),
    SWITCHING_PROTOCOLS(101),
    PROCESSING(102),
    OK(200),
    CREATED(201),
    ACCEPTED(202),
    NON_AUTHORITATIVE_INFORMATION(203),
    NO_CONTENT(204),
    RESET_CONTENT(205),
    PARTIAL_CONTENT(206),
    MULTI_STATUS(207),
    MULTIPLE_CHOICES(300),
    MOVED_PERMANENTLY(301),
    MOVED_TEMPORARILY(302),
    SEE_OTHER(303),
    NOT_MODIFIED(304),
    USE_PROXY(305),
    TEMPORARY_REDIRECT(307),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    PAYMENT_REQUIRED(402),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    NOT_ACCEPTABLE(406),
    PROXY_AUTHENTICATION_REQUIRED(407),
    REQUEST_TIMEOUT(408),
    CONFLICT(409),
    GONE(410),
    LENGTH_REQUIRED(411),
    PRECONDITION_FAILED(412),
    REQUEST_TOO_LONG(413),
    REQUEST_URI_TOO_LONG(414),
    UNSUPPORTED_MEDIA_TYPE(415),
    REQUESTED_RANGE_NOT_SATISFIABLE(416),
    EXPECTATION_FAILED(417),
    INSUFFICIENT_SPACE_ON_RESOURCE(419),
    METHOD_FAILURE(420),
    UNPROCESSABLE_ENTITY(422),
    LOCKED(423),
    FAILED_DEPENDENCY(424),
    TEMPORARILY_UNAVAILABLE(480),
    CALL_TRANSACTION_DOES_NOT_EXIST(481),
    LOOP_DETECTED(482),
    TOO_MANY_HOPS(483),
    ADDRESS_INCOMPLETE(484),
    AMBIGUOUS(485),
    BUSY_HERE(486),
    REQUEST_TERMINATED(487),
    NOT_ACCEPTIBLE_HERE(488),
    BAD_EVENT(489),
    INTERNAL_SERVER_ERROR(500),
    NOT_IMPLEMENTED(501),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504),
    HTTP_VERSION_NOT_SUPPORTED(505),
    INSUFFICIENT_STORAGE(507);
    
    private final Integer code;

    StatusCode(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    static StatusCode forCode(Integer code) {
        StatusCode[] values = values();

        for (StatusCode value : values) {
            if (value.code.equals(code)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Invalid status code: " + code);
    }
}
