package com.xiehua.exception;

public class BizException extends RuntimeException {
    private static final long serialVersionUID = -1827198664611457387L;
    private String code = "0000";
    private String msg;

    public BizException() {
        super();
    }

    public BizException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BizException(String errorCode, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = errorCode;
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
    }

    public BizException(String message) {
        super(message);
        this.msg = message;
    }

    public BizException(String errorCode, String message) {
        super(message);
        this.code = errorCode;
    }

    public BizException(Throwable cause) {
        super(cause);
    }

    public String getErrorCode() {
        return code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


}
