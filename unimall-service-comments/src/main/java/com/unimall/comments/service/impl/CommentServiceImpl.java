package com.unimall.comments.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unimall.comments.mapper.ICommentMapper;
import com.unimall.comments.pojo.dto.CommentCreateDTO;
import com.unimall.comments.pojo.entity.Comment;
import com.unimall.comments.pojo.vo.CommentVO;
import com.unimall.comments.service.ICommentService;
import com.unimall.common.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl extends ServiceImpl<ICommentMapper, Comment> implements ICommentService
{
    @Override
    public Long create(Long userId, CommentCreateDTO dto)
    {
        Comment comment = new Comment();
        comment.setGoodsId(dto.getGoodsId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setImages(dto.getImages());
        comment.setRating(dto.getRating());
        comment.setStatus(1); // 简化：直接显示
        save(comment);
        return comment.getId();
    }

    @Override
    public Page<CommentVO> pageByGoods(Long goodsId, Integer pageNum, Integer pageSize)
    {
        Page<Comment> page = lambdaQuery()
                .eq(Comment::getGoodsId, goodsId)
                .eq(Comment::getStatus, 1)
                .orderByDesc(Comment::getCreateTime)
                .page(new Page<>(pageNum, pageSize));

        Page<CommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public Page<CommentVO> pageMy(Long userId, Integer pageNum, Integer pageSize)
    {
        Page<Comment> page = lambdaQuery()
                .eq(Comment::getUserId, userId)
                .orderByDesc(Comment::getCreateTime)
                .page(new Page<>(pageNum, pageSize));

        Page<CommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public void remove(Long userId, Long id)
    {
        Comment comment = lambdaQuery()
                .eq(Comment::getId, id)
                .eq(Comment::getUserId, userId)
                .one();
        if (comment == null)
        {
            throw new BusinessException(6001, "评论不存在");
        }
        removeById(id);
    }

    private CommentVO toVO(Comment comment)
    {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setGoodsId(comment.getGoodsId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setImages(comment.getImages());
        vo.setRating(comment.getRating());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
