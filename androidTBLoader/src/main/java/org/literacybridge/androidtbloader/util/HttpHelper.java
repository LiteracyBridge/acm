package org.literacybridge.androidtbloader.util;

import android.support.annotation.Nullable;
import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.signin.UserHelper;

import java.util.HashMap;
import java.util.Map;

public class HttpHelper {

    /**
     * Make a REST call with the current signed-in credentials.
     * @param requestURL URL to request.
     */
    public static void authenticatedRestCall(String requestURL,
                                      Response.Listener<org.json.JSONObject> listener,
                                      Response.ErrorListener errorListener) {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(TBLoaderAppContext.getInstance());

        // Request a string response from the provided URL.
        AuthenticatedJsonRequest stringRequest = new AuthenticatedJsonRequest(requestURL, null,
                listener, errorListener);

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public static class AuthenticatedJsonRequest extends JsonObjectRequest {
        Map<String,String> headers = new HashMap<>();
        public AuthenticatedJsonRequest(String url,
                                        @Nullable org.json.JSONObject jsonRequest,
                                        Response.Listener<org.json.JSONObject> listener,
                                        @Nullable Response.ErrorListener errorListener,
                                        @Nullable Map<String, String> headers) {
            super(url, jsonRequest, listener, errorListener);
            this.headers.put("Accept", "application/json");
            this.headers.put("Content-Type", "text/plain");
            String jwtToken = UserHelper.getJwtToken();
            if (jwtToken != null) {
                this.headers.put("Authorization", jwtToken);
            }
            if (headers != null) {
                this.headers.putAll(headers);
            }
        }

        public AuthenticatedJsonRequest(int method, String url, @Nullable org.json.JSONObject jsonRequest, Response.Listener<org.json.JSONObject> listener, @Nullable Response.ErrorListener errorListener) {
            super(method, url, jsonRequest, listener, errorListener);
        }

        public AuthenticatedJsonRequest(String url, @Nullable org.json.JSONObject jsonRequest, Response.Listener<org.json.JSONObject> listener, @Nullable Response.ErrorListener errorListener) {
            this(url, jsonRequest, listener, errorListener, null);
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String,String> headers = new HashMap<>();
            headers.putAll(super.getHeaders());
            headers.putAll(this.headers);
            return headers;
        }
    }
}
