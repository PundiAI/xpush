package io.functionx.xpush.android;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class AndroidMessage implements Serializable {

    /**
     * receiver
     */
    private String to;

    /**
     * multiple recipients
     */
    @JSONField(name="registration_ids")
    private String[] registrationIds;

    /**
     * subject condition
     */
    private String condition;

    /**
     * priority normal or high default normal
     */
    private String priority;

    /**
     * time to live for messages and notifications default 2419200 4 weeks
     */
    @JSONField(name="time_to_live")
    private Integer timeToLive;

    private AndroidMessageNotification notification;

    /**
     * custom load message key-value pair
     */
    private Map<String, Object> data;

    @Data
    public static class AndroidMessageNotification {

        private String title;

        private String body;

        private String icon;

        private String sound;
    }

}
