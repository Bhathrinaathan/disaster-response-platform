package com.disasterrecovery.exception;

public class DataAggregationException extends RuntimeException {

    private String source;

    public DataAggregationException(String message) {
        super(message);
    }

    public DataAggregationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAggregationException(String message, String source, Throwable cause) {
        super(message, cause);
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
---------------------------------------------------------------------------
