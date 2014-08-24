package com.jbion.riseoflords.network;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class RequestSender {

    private static final String FAKE_USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.143 Safari/537.36";

    private final ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
        @Override
        public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        }
    };

    private final CloseableHttpClient http;

    public RequestSender() {
        BasicCookieStore cookieStore = new BasicCookieStore();
        http = HttpClients.custom().setDefaultCookieStore(cookieStore).setUserAgent(FAKE_USER_AGENT).build();
    }

    public String execute(HttpUriRequest request) {
        try {
            return http.execute(request, responseHandler);
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }

    public boolean execute(HttpUriRequest request, Predicate<String> responseSuccessful) {
        return execute(request, responseSuccessful, r -> true, r -> false);
    }

    public <T> T execute(HttpUriRequest request, Predicate<String> responseSuccessful,
            Function<String, T> successHandler) {
        return execute(request, responseSuccessful, successHandler, r -> null);
    }

    public <T> T execute(HttpUriRequest request, Predicate<String> responseSuccessful,
            Function<String, T> successHandler, Function<String, T> failureHandler) {
        try {
            String response = http.execute(request, responseHandler);
            if (!responseSuccessful.test(response)) {
                System.err.println(response);
                return failureHandler.apply(response);
            } else {
                return successHandler.apply(response);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Exception not handled yet.", e);
        }
    }
}
