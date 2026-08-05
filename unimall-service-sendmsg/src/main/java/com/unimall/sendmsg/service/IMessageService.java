package com.unimall.sendmsg.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.sendmsg.pojo.dto.MessageSendDTO;
import com.unimall.sendmsg.pojo.vo.MessageVO;

public interface IMessageService
{
    /**
     * 发送站内信，返回消息 id
     */
    Long send(MessageSendDTO dto);

    /**
     * 我的消息分页（时间倒序）
     */
    Page<MessageVO> page(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 未读消息数
     */
    Long unreadCount(Long userId);

    /**
     * 标记已读（只能标自己的）
     */
    void markRead(Long userId, Long id);
}
