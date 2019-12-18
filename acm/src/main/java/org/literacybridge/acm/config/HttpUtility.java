package org.literacybridge.acm.config;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import static com.ibm.media.codec.audio.g723.G723Tables.s;

/**
 * This class encapsulates methods for requesting a server via HTTP GET/POST and
 * provides methods for parsing response from the server.
 *
 * @author modified from www.codejava.net
 *
 */

public class HttpUtility {

    /**
     * Represents an HTTP connection
     */
    private HttpURLConnection httpConn;

    private HttpURLConnection sendRequest(String method,
        String requestURL,
        JSONObject params,
        Map<String, String> headers) throws IOException
    {
        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoInput(true); // true indicates the server returns response
        headers.forEach((key, value) -> httpConn.setRequestProperty(key, value));
        httpConn.setRequestMethod(method);

        if (params != null && params.size() > 0) {

            httpConn.setDoOutput(true); // true indicates POST request

            // sends POST data
            OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
            writer.write(params.toString());
            writer.flush();
        }

        return httpConn;
    }
    /**
     * Makes an HTTP request using POST method to the specified URL.
     *
     * @param requestURL
     *            the URL of the remote server
     * @param params
     *            A map containing POST data in form of key-value pairs
     * @return An HttpURLConnection object
     * @throws IOException
     *             thrown if any I/O error occurred
     */
    public HttpURLConnection sendPostRequest(String requestURL,
        JSONObject params,
        Map<String, String> headers) throws IOException
    {
        Map<String, String> allHeaders = new LinkedHashMap<>();
        if (headers != null) allHeaders.putAll(headers);
        allHeaders.put("Content-Type", "application/json");
        allHeaders.put("Accept", "application/json");

        return sendRequest("POST", requestURL, params, allHeaders);
    }
    public HttpURLConnection sendPostRequest(String requestURL,
        JSONObject params) throws IOException
    {
        return sendPostRequest(requestURL, params, null);
    }
    public HttpURLConnection sendGetRequest(String requestURL, Map<String, String> headers)
        throws IOException
    {
        return sendRequest("GET", requestURL, null, headers);
    }

    private InputStream getInputStream() throws IOException {
        InputStream inputStream = null;
        if (httpConn != null) {
            inputStream = httpConn.getInputStream();
        } else {
            throw new IOException("Connection is not established.");
        }
        return inputStream;
    }

    /**
     * Returns only one line from the server's response. This method should be
     * used if the server returns only a single line of String.
     *
     * @return a String of the server's response
     * @throws IOException
     *             thrown if any I/O error occurred
     */
    public String readSingleLineResponse() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                getInputStream()));

        String response = reader.readLine();
        reader.close();

        return response;
    }
    /**
     * Returns an array of lines from the server's response. This method should
     * be used if the server returns multiple lines of String.
     *
     * @return an array of Strings of the server's response
     * @throws IOException
     *             thrown if any I/O error occurred
     */
    public String[] readMultipleLinesRespone() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                getInputStream()));
        List<String> response = new ArrayList<String>();

        String line = "";
        while ((line = reader.readLine()) != null) {
            response.add(line);
        }
        reader.close();

        return (String[]) response.toArray(new String[0]);
    }

    public Object readJson() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            getInputStream()));
        Object o = JSONValue.parse(reader);
        return o;
    }

    public JSONObject readJSONObject() throws IOException {
        Object o = readJson();
        if (o instanceof JSONObject) {
            return (JSONObject) o;
        }
        return null;
    }

    /**
     * Closes the connection if opened
     */
    public void disconnect() {
        if (httpConn != null) {
            httpConn.disconnect();
        }
    }
}
