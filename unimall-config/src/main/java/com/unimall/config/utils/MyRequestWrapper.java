package com.unimall.config.utils;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class MyRequestWrapper extends HttpServletRequestWrapper
{
    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request The request to wrap
     * @throws IllegalArgumentException if the request is null
     */
    public MyRequestWrapper(HttpServletRequest request)
    {
        super(request);
    }

    /**
     * 返回一个空的输入流，防止其他拦截器读取/拦截到真实的请求体
     */
    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        return new ServletInputStream()
        {
            @Override
            public int read() throws IOException
            {
                return -1;
            }

            @Override
            public boolean isFinished()
            {
                return true;
            }

            @Override
            public boolean isReady()
            {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener)
            {
                // 流已结束，空实现
            }
        };
    }

    /**
     * 返回一个空的 reader，与 getInputStream 保持一致
     */
    @Override
    public BufferedReader getReader() throws IOException
    {
        return new BufferedReader(new StringReader(""));
    }
}

