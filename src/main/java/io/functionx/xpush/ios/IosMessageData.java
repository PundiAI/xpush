package io.functionx.xpush.ios;

import lombok.Data;

import java.util.Map;

@Data
public class IosMessageData {

    /**
     * message title
     */
    private String title;

    /**
     * message subtitle
     */
    private String subtitle;

    /**
     * message body
     */
    private String body;

    /**
     * message icon
     */
    private String icon;

    /**
     * message sound
     */
    private String sound;

    /**
     * number of messages marked
     */
    private Integer badgeNum;

    /**
     * other attribute key-value pairs in custom messages
     */
    private Map<String, Object> customKeyValue;

}