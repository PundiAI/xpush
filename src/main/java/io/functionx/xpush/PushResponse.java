package io.functionx.xpush;

import lombok.Data;

@Data
public class PushResponse<T> {
    private boolean isSuccess;
    private T data;
    private String errorMsg;
    private ErrorType errorType;

    public static <T> PushResponse<T> withOk() {
        return withOk(null);
    }
    public static <T> PushResponse<T> withOk(T t) {
        PushResponse<T> response = new PushResponse<>();
        response.setSuccess(true);
        response.setData(t);
        return response;
    }

    public static <T> PushResponse<T> withError(ErrorType errorType, String errorMsg, T t) {
        PushResponse<T> response = new PushResponse<>();
        response.setSuccess(false);
        response.setErrorType(errorType);
        response.setErrorMsg(errorMsg);
        response.setData(t);
        return response;
    }

    public static <T> PushResponse<T> withError(ErrorType errorType, String errorMsg) {
        return withError(errorType, errorMsg, null);
    }

    public static <T> PushResponse<T> withError(String errorMsg) {
        return withError(ErrorType.RETURN_FAIL_INFO, errorMsg, null);
    }

    public static <T> PushResponse<T> withError(String errorMsg, T t) {
        return withError(ErrorType.RETURN_FAIL_INFO, errorMsg, t);
    }

    public enum ErrorType {
        HTTP_STATUS,
        REQUEST_EXCEPTION,
        RETURN_FAIL_INFO,
        ;
    }
}
