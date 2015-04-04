package org.hildan.bots.riseoflords.network;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class RequestSender {

    private static final String FAKE_USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.143 Safari/537.36";

    /**
     * A basic response handler that returns the response content as a String.
     */
    private final ResponseHandler<String> responseHandler = response -> {
        final int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            final HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };

    private final CloseableHttpClient http;

    /**
     * Creates a new {@link RequestSender}.
     */
    public RequestSender() {
        final BasicCookieStore cookieStore = new BasicCookieStore();
        http = HttpClients.custom().setDefaultCookieStore(cookieStore).setUserAgent(FAKE_USER_AGENT).build();
    }

    /**
     * Executes the specified request and returns the response as a String.
     *
     * @param request
     *            the request to execute
     * @return the server's response
     */
    public String execute(HttpUriRequest request) {
        try {
            return http.execute(request, responseHandler);
        } catch (final IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

    /**
     * Executes the specified request and acts depending on the success of that request.
     *
     * @param request
     *            the request to execute
     * @param responseSuccessful
     *            a predicate on a response, to determine whether a request was successful or not
     * @return true if the request was successful, false otherwise
     */
    public boolean execute(HttpUriRequest request, Predicate<String> responseSuccessful) {
        return execute(request, responseSuccessful, r -> true, r -> false);
    }

    /**
     * Executes the specified request and acts depending on the success of that request.
     *
     * @param request
     *            the request to execute
     * @param responseSuccessful
     *            a predicate on a response, to determine whether a request was successful or not
     * @param successHandler
     *            a handler to execute if the request was successful. It will be passed the response
     *            as a String.
     * @return the handler's return value, or null if the request failed
     */
    public <T> T execute(HttpUriRequest request, Predicate<String> responseSuccessful,
            Function<String, T> successHandler) {
        return execute(request, responseSuccessful, successHandler, r -> null);
    }

    /**
     * Executes the specified request and acts depending on the success of that request.
     *
     * @param request
     *            the request to execute
     * @param responseSuccessful
     *            a predicate on a response, to determine whether a request was successful or not
     * @param successHandler
     *            a handler to execute if the request was successful. It will be passed the response
     *            as a String.
     * @param failureHandler
     *            a handler to execute if the request was not successful. It will be passed the
     *            response as a String.
     * @return the executed handler's return value
     */
    public <T> T execute(HttpUriRequest request, Predicate<String> responseSuccessful,
            Function<String, T> successHandler, Function<String, T> failureHandler) {
        try {
            final String response = http.execute(request, responseHandler);
            if (!responseSuccessful.test(response)) {
                System.err.println(response);
                return failureHandler.apply(response);
            } else {
                return successHandler.apply(response);
            }
        } catch (final IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }
}
