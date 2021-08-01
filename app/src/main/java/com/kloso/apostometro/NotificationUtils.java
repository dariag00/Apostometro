package com.kloso.apostometro;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NotificationUtils {

    private String FCM_API = "https://fcm.googleapis.com/fcm/send";
    private RequestQueue requestQueue;
    private Context context;

    private static final String TAG = NotificationUtils.class.getName();

    public NotificationUtils(Context appContext, RequestQueue requestQueue){
        context = appContext;
        this.requestQueue = requestQueue;
    }

    public void notifyUsers(String token, String title, String message){

        String topic = "/topics/bet_notifications";
        FirebaseMessaging.getInstance().subscribeToTopic(topic);

        JSONObject notification = new JSONObject();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put("title", title);
            notificationBody.put("message", message);
            notification.put("to", token);
            notification.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e(TAG, "onCreate: " + e.getMessage());
        }

        sendNotification(notification);

    }

    public void notifyUsers(String token, String title, String message, String betId){

        JSONObject notification = new JSONObject();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put("title", title);
            notificationBody.put("message", message);
            if(betId != null){
                notificationBody.put("type", "edit");
                notificationBody.put("bet_id", betId);
            }
            notification.put("to", token);
            notification.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e(TAG, "onCreate: " + e.getMessage());
        }

        sendNotification(notification);

    }

    private void sendNotification(JSONObject notification){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(FCM_API, notification, response -> {
            Log.i(TAG, "onResponse: " + response);
        }, error -> {
            Toast.makeText(context, "ERROR", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "onErrorResponse: Didn't work", error);
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization",  "key=" + Constants.KEY);
                params.put("Content-Type", "application/json");
                return params;
            }
        };
        requestQueue.add(jsonObjectRequest);
    }


}
