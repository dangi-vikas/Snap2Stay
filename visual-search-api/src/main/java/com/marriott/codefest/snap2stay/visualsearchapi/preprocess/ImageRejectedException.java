package com.marriott.codefest.snap2stay.visualsearchapi.preprocess;

/** Thrown when an uploaded image is rejected (too big, NSFW, broken, etc.). */
public class ImageRejectedException extends RuntimeException {
    private final String code;

    public ImageRejectedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
