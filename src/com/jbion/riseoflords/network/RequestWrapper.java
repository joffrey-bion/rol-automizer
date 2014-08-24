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

class RequestWrapper {
    
    private final URIBuilder uriBuilder;

    private boolean built = false;
    private List<NameValuePair> postData = null;
    
    public RequestWrapper(String baseUrl, String page) {
        try {
            uriBuilder = new URIBuilder(baseUrl).addParameter("p", page);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("incorrect URL");
        }
    }
    
    public RequestWrapper addParameter(String key, String value) {
        if (built) {
            throw new IllegalStateException("request already built, can't add parameters");
        }
        uriBuilder.addParameter(key, value);
        return this;
    }
    
    public RequestWrapper addPostData(String key, String value) {
        if (built) {
            throw new IllegalStateException("request already built, can't add post data");
        }
        if (postData == null) {
            postData = new ArrayList<>();
        }
        postData.add(new BasicNameValuePair(key, value));
        return this;
    }
    
    public HttpGet get() {
        if (postData != null) {
            throw new IllegalStateException("post data has been added, cannot build a GET request");
        }
        try {
            built = true;
            return new HttpGet(uriBuilder.build());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("incorrect URL");
        }
    }
    
    public HttpPost post() {
        if (postData == null) {
            postData = new ArrayList<>();
        }
        try {
            built = true;
            HttpPost postRequest = new HttpPost(uriBuilder.build());
            try {
                UrlEncodedFormEntity postContent = new UrlEncodedFormEntity(postData);
                postRequest.setEntity(postContent);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("Exception not handled yet.", e);
            }
            return postRequest;
        } catch (URISyntaxException e) {
            throw new IllegalStateException("incorrect URL");
        }
    }
}