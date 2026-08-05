package com.unimall.comments.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.comments.pojo.dto.CommentCreateDTO;
import com.unimall.comments.pojo.vo.CommentVO;

public interface ICommentService
{
    /**
     * 发表评论，返回评论 id
     */
    Long create(Long userId, CommentCreateDTO dto);

    /**
     * 按商品查已显示评论（分页，时间倒序）
     */
    Page<CommentVO> pageByGoods(Long goodsId, Integer pageNum, Integer pageSize);

    /**
     * 我的评论（分页）
     */
    Page<CommentVO> pageMy(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 删除自己的评论
     */
    void remove(Long userId, Long id);
}
