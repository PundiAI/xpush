package io.functionx.xpush.android;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AndroidMessageResponse {

    @JSONField(name="multicast_id")
    private Long multicastId;

    private Integer success;

    private Integer failure;

    @JSONField(name="canonical_ids")
    private Integer canonicalIds;

    private List<Map<String, Object>> results;

    @JSONField(name="message_id")
    private Long messageId;

    private String error;

}
