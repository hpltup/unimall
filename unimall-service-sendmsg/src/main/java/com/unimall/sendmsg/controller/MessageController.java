package com.unimall.sendmsg.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.common.result.Result;
import com.unimall.sendmsg.pojo.dto.MessageSendDTO;
import com.unimall.sendmsg.pojo.vo.MessageVO;
import com.unimall.sendmsg.service.IMessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/message")
public class MessageController
{
    private final IMessageService messageService;

    public MessageController(IMessageService messageService)
    {
        this.messageService = messageService;
    }

    /**
     * 发送站内信（管理/系统调用）
     */
    @PostMapping("/send")
    public Result<Long> send(@RequestBody @Valid MessageSendDTO dto)
    {
        return Result.ok(messageService.send(dto));
    }

    @GetMapping("/list")
    public Result<Page<MessageVO>> list(@RequestHeader("X-User-Id") Long userId,
                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize)
    {
        return Result.ok(messageService.page(userId, pageNum, pageSize));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestHeader("X-User-Id") Long userId)
    {
        return Result.ok(messageService.unreadCount(userId));
    }

    @PutMapping("/read/{id}")
    public Result<Void> markRead(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id)
    {
        messageService.markRead(userId, id);
        return Result.ok();
    }
}
