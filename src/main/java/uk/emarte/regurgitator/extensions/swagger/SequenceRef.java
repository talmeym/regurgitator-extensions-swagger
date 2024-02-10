/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;

class SequenceRef implements Step {
    @JsonProperty private final String kind = "sequence-ref";
    @JsonProperty private String id;
    @JsonProperty private String file;

    SequenceRef(String id, String file) {
        this.id = id;
        this.file = file;
    }
}
