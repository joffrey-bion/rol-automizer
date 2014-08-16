package com.jbion.riseoflords.network.exceptions;

/**
 * Thrown when something wrong happens while requesting the web service.
 */
public class WebserviceException extends Exception {

    private int userMessageId = 0;

    /**
     * Creates a {@link WebserviceException}.
     */
    public WebserviceException() {}

    /**
     * Creates a {@link WebserviceException} with the specified message.
     *
     * @param message
     *            the message of this exception
     */
    public WebserviceException(String message) {
        super(message);
    }

    /**
     * Creates a {@link WebserviceException} with the specified cause.
     *
     * @param cause
     *            the cause of this exception, wrapped in this exception
     */
    public WebserviceException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a {@link WebserviceException} with the specified message and cause.
     *
     * @param message
     *            the message of this exception
     * @param cause
     *            the cause of this exception, wrapped in this exception
     */
    public WebserviceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Adds a message to this exception, to be displayed to the user to explain the
     * problem.
     *
     * @param resId
     *            the resource ID of the message to display to the user
     * @return this {@link WebserviceException}, for chaining calls.
     */
    public WebserviceException userMessage(int resId) {
        this.userMessageId = resId;
        return this;
    }

    /**
     * Returns the resource ID of the message to display to the user to explain this
     * error. If none has been set, 0 is returned, which is an invalid Android
     * resource ID.
     *
     * @return the resource ID of the user message, or 0 if none were set.
     */
    public int getUserMessageResId() {
        return userMessageId;
    }
}
