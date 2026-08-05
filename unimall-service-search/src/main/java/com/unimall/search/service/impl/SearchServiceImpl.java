package com.unimall.search.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.unimall.common.result.Result;
import com.unimall.common.vo.GoodsVO;
import com.unimall.search.client.IGoodsClient;
import com.unimall.search.pojo.doc.GoodsDoc;
import com.unimall.search.pojo.vo.SearchPageVO;
import com.unimall.search.service.ISearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchServiceImpl implements ISearchService
{
    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
    private static final long SYNC_DELAY_MS = 600_000L;

    private final IGoodsClient goodsClient;
    private final ElasticsearchOperations operations;

    public SearchServiceImpl(IGoodsClient goodsClient, ElasticsearchOperations operations)
    {
        this.goodsClient = goodsClient;
        this.operations = operations;
    }

    @Override
    @Scheduled(fixedDelay = SYNC_DELAY_MS)
    public void sync()
    {
        Result<List<GoodsVO>> result = goodsClient.allOnSale();
        if (result == null || result.getCode() != 0 || result.getData() == null)
        {
            logger.warn("商品同步失败：goods 服务返回异常");
            return;
        }
        List<GoodsDoc> docs = result.getData().stream().map(this::toDoc).toList();
        operations.save(docs);
        logger.info("商品同步完成：{} 条", docs.size());
    }

    @Override
    public SearchPageVO<GoodsVO> search(String keyword, Integer pageNum, Integer pageSize)
    {
        Query query = new Query.Builder()
                .bool(b -> b
                        .must(m -> m.multiMatch(mm -> mm
                                .query(keyword)
                                .fields("name", "subTitle")))
                        .filter(f -> f.term(t -> t.field("status").value(1))))
                .build();

        NativeQuery nativeQuery = new NativeQueryBuilder()
                .withQuery(query)
                .withSort(s -> s.field(f -> f.field("sales").order(SortOrder.Desc)))
                .withPageable(PageRequest.of(Math.max(pageNum - 1, 0), pageSize))
                .build();

        SearchHits<GoodsDoc> hits = operations.search(nativeQuery, GoodsDoc.class);

        List<GoodsVO> records = hits.getSearchHits().stream()
                .map(hit -> toVO(hit.getContent()))
                .toList();

        SearchPageVO<GoodsVO> vo = new SearchPageVO<>();
        vo.setRecords(records);
        vo.setTotal(hits.getTotalHits());
        vo.setCurrent(pageNum);
        vo.setSize(pageSize);
        return vo;
    }

    private GoodsDoc toDoc(GoodsVO goods)
    {
        GoodsDoc doc = new GoodsDoc();
        doc.setId(goods.getId());
        doc.setName(goods.getName());
        doc.setSubTitle(goods.getSubTitle());
        doc.setCategoryId(goods.getCategoryId());
        doc.setMainImage(goods.getMainImage());
        doc.setPrice(goods.getPrice());
        doc.setSales(goods.getSales());
        doc.setStatus(goods.getStatus());
        doc.setCreateTime(goods.getCreateTime());
        return doc;
    }

    private GoodsVO toVO(GoodsDoc doc)
    {
        GoodsVO vo = new GoodsVO();
        vo.setId(doc.getId());
        vo.setName(doc.getName());
        vo.setSubTitle(doc.getSubTitle());
        vo.setCategoryId(doc.getCategoryId());
        vo.setMainImage(doc.getMainImage());
        vo.setPrice(doc.getPrice());
        vo.setSales(doc.getSales());
        vo.setStatus(doc.getStatus());
        vo.setCreateTime(doc.getCreateTime());
        return vo;
    }
}
