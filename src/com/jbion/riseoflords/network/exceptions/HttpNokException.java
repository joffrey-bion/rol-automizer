package com.jbion.riseoflords.network.exceptions;

/**
 * Thrown when a response is received with an HTTP status code different from
 * {@code 200 OK}.
 */
public class HttpNokException extends WebserviceException {

    private final int statusCode;

    /**
     * Creates a {@link HttpNokException}.
     *
     * @param httpStatusCode
     *            the HTTP status code of the response
     */
    public HttpNokException(int httpStatusCode) {
        super("received HTTP status code different from '200 OK'");
        this.statusCode = httpStatusCode;
    }

    /**
     * Creates a {@link WebserviceException} with the specified message.
     *
     * @param httpStatusCode
     *            the HTTP status code of the response
     * @param message
     *            the message of the exception to create
     */
    public HttpNokException(int httpStatusCode, String message) {
        super(message);
        this.statusCode = httpStatusCode;
    }

    /**
     * Returns the HTTP status code of the response that caused this exception.
     *
     * @return the error code of this {@link HttpNokException}.
     */
    public int getHttpStatusCode() {
        return statusCode;
    }
}
