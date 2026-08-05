package com.unimall.goods.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unimall.goods.mapper.ICategoryMapper;
import com.unimall.goods.pojo.entity.Category;
import com.unimall.goods.pojo.vo.CategoryVO;
import com.unimall.goods.service.ICategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<ICategoryMapper, Category> implements ICategoryService
{
    @Override
    public List<CategoryVO> listEnabled()
    {
        List<Category> list = lambdaQuery()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort)
                .list();
        return list.stream().map(this::toVO).toList();
    }

    private CategoryVO toVO(Category category)
    {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setParentId(category.getParentId());
        vo.setSort(category.getSort());
        return vo;
    }
}
