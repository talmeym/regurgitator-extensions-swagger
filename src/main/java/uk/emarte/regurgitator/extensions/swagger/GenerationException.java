/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

public class GenerationException extends Exception {
    public GenerationException(String message) {
        super(message);
    }

    public GenerationException(String message, Exception e) {
        super(message, e);
    }
}
