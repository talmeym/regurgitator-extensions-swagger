package uk.emarte.regurgitator.extensions.swagger;

public class GenerationException extends Exception {
    public GenerationException(String message, Exception e) {
        super(message, e);
    }
}
