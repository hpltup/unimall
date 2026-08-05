package com.unimall.sendmsg.controller;

import com.unimall.common.result.Result;
import com.unimall.sendmsg.pojo.dto.SmsSendDTO;
import com.unimall.sendmsg.pojo.dto.SmsVerifyDTO;
import com.unimall.sendmsg.service.ISmsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
public class SmsController
{
    private final ISmsService smsService;

    public SmsController(ISmsService smsService)
    {
        this.smsService = smsService;
    }

    /**
     * 发送验证码（公开接口：注册/登录等免登录场景）
     */
    @PostMapping("/send")
    public Result<Void> send(@RequestBody @Valid SmsSendDTO dto)
    {
        smsService.send(dto.getPhone());
        return Result.ok();
    }

    @PostMapping("/verify")
    public Result<Void> verify(@RequestBody @Valid SmsVerifyDTO dto)
    {
        smsService.verify(dto.getPhone(), dto.getCode());
        return Result.ok();
    }
}
