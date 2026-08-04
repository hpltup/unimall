package com.unimall.common.exception;

/**
 * 业务异常：携带业务状态码，由各服务全局异常处理器统一转为 Result
 */
public class BusinessException extends RuntimeException
{
    private final int code;

    public BusinessException(int code, String message)
    {
        super(message);
        this.code = code;
    }

    public int getCode()
    {
        return code;
    }
}
