package io.functionx.xpush.ios;

import com.google.common.base.Preconditions;
import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.util.ApnsPayloadBuilder;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import com.turo.pushy.apns.util.TokenUtil;
import io.functionx.xpush.PushResponse;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class IosMessagePush {
    private String defaultTopic;
    private volatile ApnsClient apnsClient;

    public IosMessagePush(InputStream p12InputStream, String p12Password, String defaultTopic, boolean isProdOpen) throws IOException {
        this(p12InputStream, p12Password, defaultTopic, isProdOpen, 10, 10);
    }

    public IosMessagePush(InputStream p12InputStream, String p12Password, String defaultTopic, boolean isProdOpen, int concurrentConnections, int nioEventLoopGroupThreads) throws IOException {
        apnsClient = new ApnsClientBuilder()
                .setApnsServer(isProdOpen ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                .setClientCredentials(p12InputStream, p12Password)
                .setConcurrentConnections(concurrentConnections)
                .setEventLoopGroup(new NioEventLoopGroup(nioEventLoopGroupThreads))
                .build();
        this.defaultTopic = defaultTopic;
    }

    public void closeConnect() {
        try {
            apnsClient.close().await(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void sendMessage(IosMessageData messageData, String deviceToken, IosMessagePushResult iosMessagePushResult) {
        sendMessage(messageData, deviceToken, defaultTopic, iosMessagePushResult);
    }

    public void sendMessage(IosMessageData messageData, String deviceToken, String topic, IosMessagePushResult iosMessagePushResult) {
        ApnsPayloadBuilder payloadBuilder = this.createPayload(messageData);
        // Maximum message body size is restricted as 4K, exceed part will be omitted
        String payload = payloadBuilder.buildWithDefaultMaximumLength();
        String token = TokenUtil.sanitizeTokenString(deviceToken);
        SimpleApnsPushNotification simpleApnsPushNotification = new SimpleApnsPushNotification(token, topic, payload);
        this.push(simpleApnsPushNotification, iosMessagePushResult);
    }

    private void push(SimpleApnsPushNotification pushNotification, IosMessagePushResult iosMessagePushResult) {
        Future<PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture;
        try {
            sendNotificationFuture = apnsClient.sendNotification(pushNotification);
        } catch (Exception e) {
            String errorMsg = "send error:".concat(toString(e.getMessage()));
            log.error(errorMsg, e);
            log.warn("send user message fail token:{}", pushNotification.getToken());
            iosMessagePushResult.operationComplete(PushResponse.withError(PushResponse.ErrorType.REQUEST_EXCEPTION, errorMsg));
            return;
        }

        sendNotificationFuture.addListener((GenericFutureListener<Future<PushNotificationResponse<SimpleApnsPushNotification>>>) future -> {
            final PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse;
            try {
                pushNotificationResponse = future.get();
            } catch (Exception e) {
                String errorMsg = "get error:".concat(toString(e.getMessage()));
                log.error(errorMsg, e);
                log.warn("send user message fail token:{}", pushNotification.getToken());
                iosMessagePushResult.operationComplete(PushResponse.withError(PushResponse.ErrorType.REQUEST_EXCEPTION, errorMsg));
                return;
            }
            log.debug("pushNotificationResponse: {}, future.isSuccess:{}", pushNotificationResponse, future.isSuccess());

            if (pushNotificationResponse.isAccepted()) {
                log.debug("<== push success apnsId: {}, pushNotification: {}", pushNotificationResponse.getApnsId(), pushNotificationResponse.getPushNotification());
                iosMessagePushResult.operationComplete(PushResponse.withOk());
                return;
            }
            log.warn("notification rejected by the APNs gateway:{}", pushNotificationResponse.getRejectionReason());
            if (pushNotificationResponse.getTokenInvalidationTimestamp() == null) {
                iosMessagePushResult.operationComplete(PushResponse.withError(pushNotificationResponse.getRejectionReason(), pushNotificationResponse));
                return;
            }

			String errorMsg = "token invalid timestamp:" +
				pushNotificationResponse.getTokenInvalidationTimestamp().getTime() +
				", reason:" +
				pushNotificationResponse.getRejectionReason();
            log.warn(errorMsg);
            iosMessagePushResult.operationComplete(PushResponse.withError(errorMsg, pushNotificationResponse));
        });
    }

    /**
     * create message body
     * @return ApnsPayloadBuilder
     */
    private ApnsPayloadBuilder createPayload(IosMessageData messageData) {
        Preconditions.checkNotNull(messageData);
        ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
        payloadBuilder.setAlertTitle(messageData.getTitle());
        payloadBuilder.setAlertSubtitle(messageData.getSubtitle());
        payloadBuilder.setAlertBody(messageData.getBody());
        payloadBuilder.setLaunchImageFileName(messageData.getIcon());
        payloadBuilder.setSound(messageData.getSound());
        payloadBuilder.setBadgeNumber(messageData.getBadgeNum());

        Map<String, Object> customKeyValue = messageData.getCustomKeyValue();
        if (customKeyValue != null && !customKeyValue.isEmpty()) {
            for (Map.Entry<String, Object> entry : customKeyValue.entrySet()) {
                payloadBuilder.addCustomProperty(entry.getKey(), entry.getValue());
            }
        }
        return payloadBuilder;
    }

    private String toString(String str) {
        return str == null ? "" : str;
    }

    /**
     * push result callback
     */
    public interface IosMessagePushResult {
        void operationComplete(PushResponse pushResponse);
    }
}
