package com.jbion.riseoflords.network;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.jbion.riseoflords.network.exceptions.HttpNokException;
import com.jbion.riseoflords.network.exceptions.ResponseException;
import com.jbion.riseoflords.network.exceptions.WebserviceException;
import com.jbion.riseoflords.util.Debug;
import com.jbion.riseoflords.util.Log;

/**
 * Performs low-level HTTP requests.
 */
public class HttpRequestor implements Closeable {

    private static final String LOG_TAG = HttpRequestor.class.getSimpleName();

    private final CloseableHttpClient client;

    /**
     * Creates a new {@link HttpRequestor}.
     */
    public HttpRequestor() {
        client = HttpClients.createDefault();
    }

    /** Pattern of the character set in a Content-Type header. */
    private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

    /**
     * Performs an HTTP GET request to the webservice with the specified URL.
     *
     * @param url
     *            the URL of the desired resource
     * @return the server's response body
     * @throws HttpNokException
     *             if the HTTP response code is not 200 OK
     * @throws WebserviceException
     *             if no useful result could be obtained from the server
     */
    public String doGet(URL url) throws WebserviceException {
        return doGet(url, ResponseConsumer.STRING_CONVERTER);
    }

    /**
     * Performs an HTTP POST request to the webservice with the specified endpoint
     * and content.
     *
     * @param url
     *            the URL of the desired resource
     * @param content
     *            the content to POST as the request body
     * @return the server's response body
     * @throws HttpNokException
     *             if the HTTP response code is not 200 OK
     * @throws WebserviceException
     *             if no useful result could be obtained from the server
     */
    public String doPost(URL url, byte[] content) throws WebserviceException {
        return doPost(url, content, ResponseConsumer.STRING_CONVERTER);
    }

    /**
     * Performs an HTTP GET request to the webservice with the specified URL.
     *
     * @param url
     *            the URL of the desired resource
     * @param handler
     *            the {@link ResponseConsumer} to use to read the stream of the
     *            response
     * @return the server's response body
     * @throws HttpNokException
     *             if the HTTP response code is not 200 OK
     * @throws WebserviceException
     *             if no useful result could be obtained from the server
     */
    public <T> T doGet(URL url, ResponseConsumer<T> handler) throws WebserviceException {
        Log.v(LOG_TAG, "GET " + url);

        final HttpURLConnection connection = createConnection(url);
        try {
            return readResponse(connection, handler);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Performs an HTTP POST request to the webservice with the specified endpoint
     * and content.
     *
     * @param url
     *            the URL of the desired resource
     * @param content
     *            the content to POST as the request body
     * @param handler
     *            the {@link ResponseConsumer} to use to read the stream of the
     *            response
     * @return the server's response body
     * @throws HttpNokException
     *             if the HTTP response code is not 200 OK
     * @throws WebserviceException
     *             if no useful result could be obtained from the server
     */
    public <T> T doPost(URL url, byte[] content, ResponseConsumer<T> handler) throws WebserviceException {
        Log.v(LOG_TAG, "POST " + url + "\n    content: " + content);

        final HttpURLConnection connection = createConnection(url);

        try {
            // to do a POST request
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(content.length);

            OutputStream outStream;
            try {
                outStream = connection.getOutputStream();
            } catch (final IOException e) {
                Log.e(LOG_TAG, "cannot create an output stream to the connection");
                throw new WebserviceException("cannot create an output stream to the connection", e);
            }

            // write request body
            try {
                outStream.write(content);
            } catch (final IOException e) {
                Log.e(LOG_TAG, "cannot write POST content");
                throw new WebserviceException("cannot write POST content", e);
            }

            return readResponse(connection, handler);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Creates an {@link HttpURLConnection} to the specified URL. Wraps the potential
     * exceptions into a {@link WebserviceException}.
     *
     * @param url
     *            the URL to connect to
     * @return the created connection
     * @throws WebserviceException
     *             if the connection cannot be opened for some reason
     */
    private static HttpURLConnection createConnection(URL url) throws WebserviceException {
        Debug.testSlowWS();
        try {
            return (HttpURLConnection) url.openConnection();
        } catch (final IOException e) {
            Log.e(LOG_TAG, "cannot open the connection");
            throw new WebserviceException("cannot open the connection", e);
        }
    }

    /**
     * Returns an uncompressed {@link InputStream} to read the response from the
     * server.
     * <p>
     * The decompression is already dealt with, if any was specified in the HTTP
     * Content-Encoding header. Possible exceptions are wrapped into a
     * {@link WebserviceException}.
     *
     * @param connection
     *            the {@link HttpURLConnection} to use to connect
     * @return an {@link InputStream} to read the server's response body
     * @throws HttpNokException
     *             if the HTTP response code is not 200 OK
     * @throws WebserviceException
     *             if the response cannot be read for some reason
     */
    private static InputStream getResponseStream(HttpURLConnection connection) throws WebserviceException {
        try {
            final int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(LOG_TAG, "received NOK HTTP code from the server: " + responseCode);
                throw new HttpNokException(responseCode);
            }
        } catch (final IOException e) {
            Log.e(LOG_TAG, "cannot read HTTP response code");
            throw new WebserviceException("cannot read HTTP response code", e);
        }

        try {
            return new BufferedInputStream(connection.getInputStream());
        } catch (final IOException e) {
            Log.e(LOG_TAG, "cannot create an input stream from the response");
            throw new ResponseException("cannot create an input stream from the response", e);
        }
    }

    /**
     * Reads the server's response from the given connection.
     * <p>
     * The specified handler is called to read the {@link InputStream} of the
     * response body, and its result is returned. Possible exceptions are wrapped
     * into a {@link WebserviceException}.
     *
     * @param connection
     *            the {@link HttpURLConnection} to use to connect
     * @param handler
     *            the {@link ResponseConsumer} to use to read the stream of the
     *            response
     * @return the result returned by the specified {@link ResponseConsumer} after
     *         parsing
     * @throws HttpNokException
     *             if the HTTP response code is not 200 OK
     * @throws WebserviceException
     *             if the response cannot be read for some reason
     */
    private static <T> T readResponse(HttpURLConnection connection, ResponseConsumer<T> handler)
            throws WebserviceException {
        final InputStream in = getResponseStream(connection);
        try {
            return handler.consume(in, getCharsetFromContentType(connection.getContentType()));
        } catch (final UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "cannot read the response because the encoding is not supported");
            throw new WebserviceException("cannot read the response because the encoding is not supported", e);
        } catch (final IOException e) {
            Log.e(LOG_TAG, "I/O error while reading the response");
            throw new WebserviceException("I/O error while reading the response", e);
        } catch (final Exception e) {
            Log.e(LOG_TAG, "error while reading the response");
            throw new WebserviceException("error while reading the response", e);
        }
    }

    /**
     * Parses out a charset from a Content-Type header.
     *
     * @param contentType
     *            the content type header (e.g. "text/html; charset=EUC-JP")
     * @return a {@code String} representing the name of the charset, or null if not
     *         found. The charset is trimmed and in uppercase.
     */
    private static String getCharsetFromContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        final Matcher m = CHARSET_PATTERN.matcher(contentType);
        if (m.find()) {
            return m.group(1).trim().toUpperCase(Locale.US);
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

}
