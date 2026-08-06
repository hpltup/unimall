package com.unimall.search.pojo.doc;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 ES 文档（索引 goods，IK + 拼音分词）
 */
@Data
@Document(indexName = "goods", createIndex = true)
@Setting(settingPath = "es/goods-settings.json")
@Mapping(mappingPath = "es/goods-mapping.json")
public class GoodsDoc
{
    @Id
    private Long id;

    private String name;

    private String subTitle;

    private Long categoryId;

    private String categoryName;

    private String mainImage;

    private BigDecimal price;

    private Integer sales;

    private Integer stock;

    private Integer status;

    /** epoch millis（Spring Data ES 对 LocalDateTime 存成 Long 后无法读回，故用 Long 存） */
    private Long createTime;
}
