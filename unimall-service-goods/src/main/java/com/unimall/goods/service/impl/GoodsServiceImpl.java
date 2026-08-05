package com.unimall.goods.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unimall.common.exception.BusinessException;
import com.unimall.goods.mapper.IGoodsMapper;
import com.unimall.goods.pojo.dto.GoodsCreateDTO;
import com.unimall.goods.pojo.dto.GoodsQueryDTO;
import com.unimall.common.vo.GoodsVO;
import com.unimall.goods.pojo.entity.Goods;
import com.unimall.goods.service.IGoodsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodsServiceImpl extends ServiceImpl<IGoodsMapper, Goods> implements IGoodsService
{
    @Override
    public Page<GoodsVO> pageQuery(GoodsQueryDTO dto)
    {
        Page<Goods> page = lambdaQuery()
                .eq(dto.getCategoryId() != null, Goods::getCategoryId, dto.getCategoryId())
                .like(StringUtils.isNotBlank(dto.getKeyword()), Goods::getName, dto.getKeyword())
                .eq(dto.getStatus() != null, Goods::getStatus, dto.getStatus())
                .orderByDesc(Goods::getCreateTime)
                .page(new Page<>(dto.getPageNum(), dto.getPageSize()));

        Page<GoodsVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public GoodsVO detail(Long id)
    {
        Goods goods = getById(id);
        if (goods == null)
        {
            throw new BusinessException(2001, "商品不存在");
        }
        return toVO(goods);
    }

    @Override
    public Long create(GoodsCreateDTO dto)
    {
        Goods goods = new Goods();
        goods.setCategoryId(dto.getCategoryId());
        goods.setName(dto.getName());
        goods.setSubTitle(dto.getSubTitle());
        goods.setMainImage(dto.getMainImage());
        goods.setImages(dto.getImages());
        goods.setDetail(dto.getDetail());
        goods.setPrice(dto.getPrice());
        goods.setMarketPrice(dto.getMarketPrice());
        goods.setStock(dto.getStock());
        goods.setSales(0);
        goods.setStatus(0); // 新增默认下架，审核后上架
        save(goods);
        return goods.getId();
    }

    @Override
    public void updateStatus(Long id, Integer status)
    {
        Goods goods = getById(id);
        if (goods == null)
        {
            throw new BusinessException(2001, "商品不存在");
        }
        lambdaUpdate().eq(Goods::getId, id).set(Goods::getStatus, status).update();
    }

    @Override
    public List<GoodsVO> batchByIds(List<Long> ids)
    {
        if (ids == null || ids.isEmpty())
        {
            return List.of();
        }
        return listByIds(ids).stream().map(this::toVO).toList();
    }

    @Override
    public void deductStock(Long goodsId, Integer quantity)
    {
        // 原子扣减：stock = stock - ? AND stock >= ?，影响行数为 0 说明库存不足
        boolean ok = lambdaUpdate()
                .setSql("stock = stock - " + quantity)
                .eq(Goods::getId, goodsId)
                .ge(Goods::getStock, quantity)
                .update();
        if (!ok)
        {
            throw new BusinessException(2002, "商品库存不足");
        }
    }

    @Override
    public void restoreStock(Long goodsId, Integer quantity)
    {
        lambdaUpdate()
                .setSql("stock = stock + " + quantity)
                .eq(Goods::getId, goodsId)
                .update();
    }

    private GoodsVO toVO(Goods goods)
    {
        GoodsVO vo = new GoodsVO();
        vo.setId(goods.getId());
        vo.setCategoryId(goods.getCategoryId());
        vo.setName(goods.getName());
        vo.setSubTitle(goods.getSubTitle());
        vo.setMainImage(goods.getMainImage());
        vo.setImages(goods.getImages());
        vo.setDetail(goods.getDetail());
        vo.setPrice(goods.getPrice());
        vo.setMarketPrice(goods.getMarketPrice());
        vo.setStock(goods.getStock());
        vo.setSales(goods.getSales());
        vo.setStatus(goods.getStatus());
        vo.setCreateTime(goods.getCreateTime());
        return vo;
    }
}
