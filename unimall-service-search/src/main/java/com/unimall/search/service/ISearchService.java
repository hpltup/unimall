package com.unimall.search.service;

import com.unimall.common.vo.GoodsVO;
import com.unimall.search.pojo.vo.SearchPageVO;

public interface ISearchService
{
    /**
     * 全量同步商品数据到 ES（定时任务 + 手动触发）
     */
    void sync();

    /**
     * 搜索商品（IK + 拼音分词，按销量降序）
     */
    SearchPageVO<GoodsVO> search(String keyword, Integer pageNum, Integer pageSize);
}
