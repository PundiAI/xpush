package io.functionx.xpush.ios;

import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IosMessagePushTest {

    private static final String p12Path = "<.p12 certificate of your ios developer>";
    private static final String p12Password = "<certificate password>";
    private static final String topic = "mytopic";
    private static final boolean isProdOpen = false;
    private static final String token = "<ios device token>";



    @Test
    public void sendMessage() throws IOException {
        IosMessageData iosMessageData = new IosMessageData();
        iosMessageData.setTitle("Hello Function X");
        iosMessageData.setBody("This is a test message");
        iosMessageData.setIcon("");
        iosMessageData.setSound("default");

        Map<String, Object> data = new HashMap<>();
        data.put("<customize field>", "<value>");

        iosMessageData.setCustomKeyValue(data);
        IosMessagePush iosMessagePush = new IosMessagePush(new FileInputStream(p12Path), p12Password, topic, isProdOpen);
        iosMessagePush.sendMessage(iosMessageData, token, pushResponse -> Assert.assertTrue(pushResponse.getErrorMsg(), pushResponse.isSuccess()));
    }

}