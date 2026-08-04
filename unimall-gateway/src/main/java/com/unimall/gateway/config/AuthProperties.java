package com.unimall.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权配置：白名单路径（无需登录即可访问，如登录/注册接口）
 */
@Component
@ConfigurationProperties(prefix = "unimall.gateway.auth")
public class AuthProperties
{
    private List<String> whitelist = new ArrayList<>();

    public List<String> getWhitelist()
    {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist)
    {
        this.whitelist = whitelist;
    }
}
