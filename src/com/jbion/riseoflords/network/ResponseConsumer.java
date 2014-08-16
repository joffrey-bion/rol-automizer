package com.jbion.riseoflords.network;

import java.io.InputStream;

import com.jbion.riseoflords.util.Streams;

/**
 * Reads the body of a server response, and gives a result.
 *
 * @param <Result>
 *            the type of the returned result
 */
public interface ResponseConsumer<Result> {

    /**
     * A simple {@link ResponseConsumer} that converts the response into a
     * {@code String}.
     */
    public static final ResponseConsumer<String> STRING_CONVERTER = new ResponseConsumer<String>() {
        @Override
        public String consume(InputStream response, String charsetName) throws Exception {
            return Streams.toString(response, charsetName == null ? "UTF-8" : charsetName);
        }
    };

    /**
     * Reads completely the response, and returns a result.
     *
     * @param response
     *            the response body as an {@link InputStream} to read from
     * @param charsetName
     *            the character set to use to read the stream, or {@code null} if it
     *            is unknown
     * @return a result to be returned by the method dealing with the HTTP request
     * @throws Exception
     *             if an error occurred while reading the response
     */
    public Result consume(InputStream response, String charsetName) throws Exception;

}