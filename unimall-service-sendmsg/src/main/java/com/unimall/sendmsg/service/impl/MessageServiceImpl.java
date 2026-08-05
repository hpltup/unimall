package com.unimall.sendmsg.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unimall.common.exception.BusinessException;
import com.unimall.sendmsg.mapper.IMessageMapper;
import com.unimall.sendmsg.pojo.dto.MessageSendDTO;
import com.unimall.sendmsg.pojo.entity.Message;
import com.unimall.sendmsg.pojo.vo.MessageVO;
import com.unimall.sendmsg.service.IMessageService;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl extends ServiceImpl<IMessageMapper, Message> implements IMessageService
{
    @Override
    public Long send(MessageSendDTO dto)
    {
        Message message = new Message();
        message.setUserId(dto.getUserId());
        message.setTitle(dto.getTitle());
        message.setContent(dto.getContent());
        message.setIsRead(0);
        save(message);
        return message.getId();
    }

    @Override
    public Page<MessageVO> page(Long userId, Integer pageNum, Integer pageSize)
    {
        Page<Message> page = lambdaQuery()
                .eq(Message::getUserId, userId)
                .orderByDesc(Message::getCreateTime)
                .page(new Page<>(pageNum, pageSize));

        Page<MessageVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public Long unreadCount(Long userId)
    {
        return lambdaQuery()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0)
                .count();
    }

    @Override
    public void markRead(Long userId, Long id)
    {
        Message message = lambdaQuery()
                .eq(Message::getId, id)
                .eq(Message::getUserId, userId)
                .one();
        if (message == null)
        {
            throw new BusinessException(8004, "消息不存在");
        }
        lambdaUpdate()
                .eq(Message::getId, id)
                .set(Message::getIsRead, 1)
                .update();
    }

    private MessageVO toVO(Message message)
    {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
