package com.jbion.riseoflords.network;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

/**
 * A wrapper class for basic HTTP request creation. Several parameters can be added, then the
 * request can be built using {@link #get()} or {@link #post()}.
 */
class Request {

    private URIBuilder uriBuilder;

    private boolean built = false;
    private List<NameValuePair> postData = null;

    private Request() {}

    /**
     * Creates a new {@link Request} initialized on the specified URL, with a preset page parameter.
     *
     * @param baseUrl
     *            the base URL to use
     * @param page
     *            the "p" parameter value, containing the page to request
     * @return the created request
     */
    public static Request from(String baseUrl, String page) {
        final Request req = new Request();
        try {
            req.uriBuilder = new URIBuilder(baseUrl).addParameter("p", page);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("incorrect URL");
        }
        return req;
    }

    /**
     * Adds the specified parameter to the URL's query string.
     *
     * @param key
     *            the parameter's key
     * @param value
     *            the parameter's value
     * @return this {@link Request}
     */
    public Request addParameter(String key, String value) {
        if (built) {
            throw new IllegalStateException("request already built, can't add parameters");
        }
        uriBuilder.addParameter(key, value);
        return this;
    }

    /**
     * Adds the specified parameter to the request body. This forbids later use of {@link #get()} on
     * this {@link Request}, as this method should only be used for {@link #post()} requests.
     *
     * @param key
     *            the key of the parameter to add
     * @param value
     *            the value of the parameter to add
     * @return this {@link Request}
     */
    public Request addPostData(String key, String value) {
        if (built) {
            throw new IllegalStateException("request already built, can't add post data");
        }
        if (postData == null) {
            postData = new ArrayList<>();
        }
        postData.add(new BasicNameValuePair(key, value));
        return this;
    }

    /**
     * Creates a GET request with all the previously set parameters.
     *
     * @return an {@link HttpGet} object representing the request
     */
    public HttpGet get() {
        if (postData != null) {
            throw new IllegalStateException("post data has been added, cannot build a GET request");
        }
        try {
            built = true;
            return new HttpGet(uriBuilder.build());
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("incorrect URL");
        }
    }

    /**
     * Creates a POST request with all the previously set parameters and POST content.
     *
     * @return an {@link HttpPost} object representing the request
     */
    public HttpPost post() {
        if (postData == null) {
            postData = new ArrayList<>();
        }
        try {
            built = true;
            final HttpPost postRequest = new HttpPost(uriBuilder.build());
            try {
                final UrlEncodedFormEntity postContent = new UrlEncodedFormEntity(postData);
                postRequest.setEntity(postContent);
            } catch (final UnsupportedEncodingException e) {
                throw new IllegalStateException("Exception not handled yet.", e);
            }
            return postRequest;
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("incorrect URL");
        }
    }
}