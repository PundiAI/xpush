package io.functionx.xpush.android;

import io.functionx.xpush.PushResponse;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AndroidMessagePushTest {

    private static final String url = "https://fcm.googleapis.com/fcm/send";
    private static final String authorization = "<your firebase key>";
    private static final String token = "<android device token>";



    @Test
    public void sendToUser() {
        AndroidMessage.AndroidMessageNotification androidMessageNotification = new AndroidMessage.AndroidMessageNotification();
        androidMessageNotification.setTitle("Hello Function X");
        androidMessageNotification.setBody("This is a test message");
        androidMessageNotification.setIcon("");
        androidMessageNotification.setSound("default");

        AndroidMessage androidMessage = new AndroidMessage();
        androidMessage.setTo(token);
        androidMessage.setNotification(androidMessageNotification);

        Map<String, Object> data = new HashMap<>();
        data.put("<customize field>", "<value>");

        androidMessage.setData(data);
        AndroidMessagePush androidMessagePush = new AndroidMessagePush(authorization, url);
        PushResponse pushResponse = androidMessagePush.sendToUser(androidMessage);
        Assert.assertTrue(pushResponse.getErrorMsg(), pushResponse.isSuccess());
    }

}