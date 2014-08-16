package com.jbion.riseoflords.network.exceptions;

/**
 * Thrown when the webservice response is malformed or contains an error code.
 */
public class ResponseException extends WebserviceException {

    private final Integer errorCode;

    /**
     * Creates a {@link ResponseException} with the specified webservice error code.
     *
     * @param errorCode
     *            the error code of the response
     */
    public ResponseException(int errorCode) {
        super("the webservice sent the error code " + errorCode);
        this.errorCode = errorCode;
    }

    /**
     * Creates a {@link ResponseException} with the specified webservice error code
     * and message.
     *
     * @param errorCode
     *            the error code of the response
     * @param message
     *            the message of the exception to create
     */
    public ResponseException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates a {@link ResponseException} representing a problem with the webservice
     * response. For example, it can wrap an exception thrown while parsing a
     * malformed response.
     *
     * @param cause
     *            the {@link Throwable} that caused this exception, or {@code null}
     *            if no specific cause is known
     */
    public ResponseException(Throwable cause) {
        super("there was a problem with the response", cause);
        this.errorCode = null;
    }

    /**
     * Creates a {@link ResponseException} representing a problem with the webservice
     * response, with the specified message. For example, it can wrap an exception
     * thrown while parsing a malformed response.
     *
     * @param message
     *            the message of the exception to create
     * @param cause
     *            the {@link Throwable} that caused this exception, or {@code null}
     *            if no specific cause is known
     */
    public ResponseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    /**
     * Returns the error code of this {@link ResponseException}. This method returns
     * {@code null} if this exception is not due to an error code in the response.
     *
     * @return the error code of this {@link ResponseException}, or {@code null} if
     *         none is available.
     */
    public Integer getErrorCode() {
        return errorCode;
    }
}
