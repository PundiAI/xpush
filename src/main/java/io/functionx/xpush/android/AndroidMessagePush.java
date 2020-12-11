package io.functionx.xpush.android;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import io.functionx.xpush.PushResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class AndroidMessagePush {

    private final String authorization;
    private final String sendMessageUrl;

    private OkHttpClient client;

    public AndroidMessagePush(String authorization, String sendMessageUrl) {
        this.authorization = authorization;
        this.sendMessageUrl = sendMessageUrl;
        client = new OkHttpClient().newBuilder().build();
    }

    public AndroidMessagePush(String authorization, String sendMessageUrl, String proxyHost, String proxyPort) {
        this.authorization = authorization;
        this.sendMessageUrl = sendMessageUrl;
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        client = builder.proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)))).build();
    }

    public AndroidMessagePush(String authorization, String sendMessageUrl, OkHttpClient client) {
        this.authorization = authorization;
        this.sendMessageUrl = sendMessageUrl;
        this.client = client;
    }

    public PushResponse sendToUser(AndroidMessage androidMessage) {
        Preconditions.checkNotNull(androidMessage, "androidMessage is null");
        Preconditions.checkNotNull(androidMessage.getTo(), "androidMessage.to is null");

        final PushResponse<AndroidMessageResponse> pushResponse;
        try {
            pushResponse = send(androidMessage);
        } catch (IOException e) {
            final String errorMsg = "send fail:".concat(toString(e.getMessage()));
            log.warn(errorMsg, e);
            log.warn("send user message fail token:{}", androidMessage.getTo());
            return PushResponse.withError(PushResponse.ErrorType.REQUEST_EXCEPTION, errorMsg);
        }
        if (!pushResponse.isSuccess()) {
            return PushResponse.withError(pushResponse.getErrorType(), pushResponse.getErrorMsg());
        }
        AndroidMessageResponse androidMessageResponse = pushResponse.getData();

        if (androidMessageResponse.getFailure() == 0 && androidMessageResponse.getCanonicalIds() == 0) {
            log.debug("message send success to:{}", androidMessage.getTo());
            return PushResponse.withOk();
        }
        List<Map<String, Object>> results = androidMessageResponse.getResults();
        if (results == null || results.size() <= 0) {
            log.warn("message send fail to:{}, reason:{}", androidMessage.getTo(), androidMessageResponse);
            return PushResponse.withError(JSON.toJSONString(androidMessageResponse));
        }
        Object error = results.get(0).get("error");
        String errorMsg = error == null ? "" : error.toString();

        log.warn("message send fail to:{}, reason:{}", androidMessage.getTo(), results);
        if ("".equals(errorMsg)) {
            return PushResponse.withError(JSON.toJSONString(results));
        }
        return PushResponse.withError(errorMsg);
    }

    public PushResponse sendToUsersGroup(AndroidMessage androidMessage) {
        Preconditions.checkNotNull(androidMessage, "androidMessage is null");
        Preconditions.checkNotNull(androidMessage.getRegistrationIds());
        final AndroidMessageResponse androidMessageResponse;
        try {
            androidMessageResponse = send(androidMessage).getData();
        } catch (IOException e) {
            final String errorMsg = "group send fail:".concat(toString(e.getMessage()));
            log.warn(errorMsg, e);
            log.warn("group send user message fail registration_ids:{}", Arrays.toString(androidMessage.getRegistrationIds()));
            return PushResponse.withError(PushResponse.ErrorType.REQUEST_EXCEPTION, errorMsg);
        }
        if (androidMessageResponse.getFailure() == 0 && androidMessageResponse.getCanonicalIds() == 0) {
            log.debug("message group send success registration_ids:{}", Arrays.toString(androidMessage.getRegistrationIds()));
            return PushResponse.withOk();
        }

        List<Map<String, Object>> results = androidMessageResponse.getResults();
        if (results == null || results.size() <= 0) {
            log.warn("message group send fail registration_ids:{}, reason:{}", Arrays.toString(androidMessage.getRegistrationIds()), androidMessageResponse);
            return PushResponse.withError(JSON.toJSONString(androidMessageResponse));
        }
        Object error = results.get(0).get("error");
        String errorMsg = error == null ? "" : error.toString();

        log.warn("message group send fail registration_ids:{}, reason:{}", Arrays.toString(androidMessage.getRegistrationIds()), results);
        if ("".equals(errorMsg)) {
            return PushResponse.withError(JSON.toJSONString(results));
        }
        return PushResponse.withError(errorMsg);
    }

    public PushResponse sendToTopic(AndroidMessage androidMessage) {
        if (androidMessage.getTo() == null && androidMessage.getCondition() == null) {
            log.warn("androidMessage.to, androidMessage.condition cannot be empty at the same time!");
            throw new NullPointerException();
        }

        if (androidMessage.getCondition() == null && !androidMessage.getTo().startsWith("/topics/")) {
            androidMessage.setTo("/topics/" + androidMessage.getTo());
        }

        final AndroidMessageResponse androidMessageResponse;
        try {
            androidMessageResponse = send(androidMessage).getData();
        } catch (IOException e) {
            final String errorMsg = "topic send fail:".concat(toString(e.getMessage()));
            log.warn(errorMsg, e);
            log.warn("topic send fail token:{}, topics:{}", androidMessage.getTo(), androidMessage.getCondition());
            return PushResponse.withError(PushResponse.ErrorType.REQUEST_EXCEPTION, errorMsg);
        }
        if (androidMessageResponse.getMessageId() != null) {
            log.debug("message send topics success token:{}, topics:{}", androidMessage.getTo(), androidMessage.getCondition());
            return PushResponse.withOk();
        }
        log.warn("message send topics fail token:{}, topics:{}, error:{}", androidMessage.getTo(), androidMessage.getCondition(), androidMessageResponse);
        return PushResponse.withError("send fail:".concat(toString(androidMessageResponse.getError())));
    }

    private PushResponse<AndroidMessageResponse> send(AndroidMessage androidMessage) throws IOException {
        String jsonData = JSON.toJSONString(androidMessage);

        log.info("==> android send jsonData: {}", jsonData);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonData);

        Request okRequest = new Request.Builder()
                .url(sendMessageUrl)
                .addHeader("Authorization", authorization)
                .post(body)
                .build();

        Response response = client.newCall(okRequest).execute();
        if (response.code() != 200 ) {
            log.info("<== request error code:{}", response.code());
            return PushResponse.withError(PushResponse.ErrorType.HTTP_STATUS, String.valueOf(response.code()));
        }
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            log.info("<== response body is null");
            return PushResponse.withError(PushResponse.ErrorType.HTTP_STATUS, "response body is null");
        }
        String responseStr = responseBody.string();

        AndroidMessageResponse androidMessageResponse = JSON.parseObject(responseStr, AndroidMessageResponse.class);
        log.info("<== androidMessageResponse: {}", androidMessageResponse);

        return PushResponse.withOk(androidMessageResponse);
    }

    private String toString(String str) {
        return str == null ? "" : str;
    }

}
