package com.unimall.comments.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.comments.pojo.dto.CommentCreateDTO;
import com.unimall.comments.pojo.vo.CommentVO;
import com.unimall.comments.service.ICommentService;
import com.unimall.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comment")
public class CommentController
{
    private final ICommentService commentService;

    public CommentController(ICommentService commentService)
    {
        this.commentService = commentService;
    }

    @PostMapping
    public Result<Long> create(@RequestHeader("X-User-Id") Long userId, @RequestBody @Valid CommentCreateDTO dto)
    {
        return Result.ok(commentService.create(userId, dto));
    }

    @GetMapping("/list")
    public Result<Page<CommentVO>> list(@RequestParam Long goodsId,
                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize)
    {
        return Result.ok(commentService.pageByGoods(goodsId, pageNum, pageSize));
    }

    @GetMapping("/list/my")
    public Result<Page<CommentVO>> listMy(@RequestHeader("X-User-Id") Long userId,
                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize)
    {
        return Result.ok(commentService.pageMy(userId, pageNum, pageSize));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id)
    {
        commentService.remove(userId, id);
        return Result.ok();
    }
}
