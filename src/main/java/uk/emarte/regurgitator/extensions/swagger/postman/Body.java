/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger.postman;

public class Body {
    private final Mode mode;
    private final String raw;
    private final boolean disabled;

    public Body(Mode mode, String raw, boolean disabled) {
        this.mode = mode;
        this.raw = raw;
        this.disabled = disabled;
    }

    public Mode getMode() {
        return mode;
    }

    public String getRaw() {
        return raw;
    }

    public boolean isDisabled() {
        return disabled;
    }
}
