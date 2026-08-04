package com.unimall.config.filter;

import com.unimall.config.utils.MyRequestWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MyFilter implements Filter
{

    private static final Logger logger = LoggerFactory.getLogger(MyFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        logger.info("过滤器启动");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String url = httpServletRequest.getRequestURI();

        String suffix = "/refresh-config-bus";
        if(!url.endsWith(suffix))
        {
            chain.doFilter(request, response);
            return;
        }

        // 将请求体替换为空流，避免其他拦截器读取/拦截该流
        MyRequestWrapper requestWrapper = new MyRequestWrapper(httpServletRequest);
        chain.doFilter(requestWrapper, response);
    }

    @Override
    public void destroy()
    {
        logger.info("过滤器销毁");
    }
}
