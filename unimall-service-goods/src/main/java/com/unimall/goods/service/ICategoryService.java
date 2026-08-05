package com.unimall.goods.service;

import com.unimall.goods.pojo.vo.CategoryVO;

import java.util.List;

public interface ICategoryService
{
    /**
     * 启用中的分类列表（按 sort 升序）
     */
    List<CategoryVO> listEnabled();
}
