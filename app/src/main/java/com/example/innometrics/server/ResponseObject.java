package com.example.innometrics.server;

import org.json.JSONObject;

/**
 * Class to work with responses from server
 */
public class ResponseObject {
    private int responseCode;
    private JSONObject response;

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public void setResponse(JSONObject response) {
        this.response = response;
    }

    public int getResponseCode() {

        return responseCode;
    }

    public JSONObject getResponse() {
        return response;
    }

    public ResponseObject(int responseCode, JSONObject response) {

        this.responseCode = responseCode;
        this.response = response;
    }
}
