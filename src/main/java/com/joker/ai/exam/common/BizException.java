package com.joker.ai.exam.common;

public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(String message) {
        super(message);
        this.errorCode = ErrorCode.BUSINESS_ERROR;
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
