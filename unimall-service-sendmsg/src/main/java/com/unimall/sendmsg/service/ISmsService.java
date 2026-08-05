package com.unimall.sendmsg.service;

public interface ISmsService
{
    /**
     * 发送短信验证码（模拟发送，验证码存 Redis TTL 5 分钟）
     */
    void send(String phone);

    /**
     * 校验验证码（一次性：校验通过即删除）
     */
    void verify(String phone, String code);
}
