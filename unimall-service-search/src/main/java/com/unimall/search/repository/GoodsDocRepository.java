package com.unimall.search.repository;

import com.unimall.search.pojo.doc.GoodsDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface GoodsDocRepository extends ElasticsearchRepository<GoodsDoc, Long>
{
}
