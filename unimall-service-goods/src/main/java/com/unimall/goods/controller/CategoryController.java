package com.unimall.goods.controller;

import com.unimall.common.result.Result;
import com.unimall.goods.pojo.vo.CategoryVO;
import com.unimall.goods.service.ICategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController
{
    private final ICategoryService categoryService;

    public CategoryController(ICategoryService categoryService)
    {
        this.categoryService = categoryService;
    }

    @GetMapping("/list")
    public Result<List<CategoryVO>> list()
    {
        return Result.ok(categoryService.listEnabled());
    }
}
