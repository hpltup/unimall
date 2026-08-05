package com.unimall.goods.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.common.vo.GoodsVO;
import com.unimall.goods.pojo.dto.GoodsCreateDTO;
import com.unimall.goods.pojo.dto.GoodsQueryDTO;

public interface IGoodsService
{
    /**
     * 分页查询商品
     */
    Page<GoodsVO> pageQuery(GoodsQueryDTO dto);

    /**
     * 商品详情
     */
    GoodsVO detail(Long id);

    /**
     * 新增商品（默认下架）
     */
    Long create(GoodsCreateDTO dto);

    /**
     * 上下架
     */
    void updateStatus(Long id, Integer status);

    /**
     * 按 ids 批量查询（服务间调用用：购物车/订单查商品信息）
     */
    java.util.List<GoodsVO> batchByIds(java.util.List<Long> ids);

    /**
     * 扣减库存（原子操作，防超卖）：库存不足抛 2002
     */
    void deductStock(Long goodsId, Integer quantity);

    /**
     * 回补库存（订单取消时）
     */
    void restoreStock(Long goodsId, Integer quantity);

    /**
     * 全部上架商品（服务间调用：search 服务同步到 ES）
     */
    java.util.List<GoodsVO> allOnSale();
}
