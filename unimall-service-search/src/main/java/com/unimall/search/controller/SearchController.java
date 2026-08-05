package com.unimall.search.controller;

import com.unimall.common.result.Result;
import com.unimall.common.vo.GoodsVO;
import com.unimall.search.pojo.vo.SearchPageVO;
import com.unimall.search.service.ISearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
public class SearchController
{
    private final ISearchService searchService;

    public SearchController(ISearchService searchService)
    {
        this.searchService = searchService;
    }

    /**
     * 搜索商品（公开接口）
     */
    @GetMapping("/goods")
    public Result<SearchPageVO<GoodsVO>> search(@RequestParam String keyword,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize)
    {
        return Result.ok(searchService.search(keyword, pageNum, pageSize));
    }

    /**
     * 手动触发全量同步（管理接口）
     */
    @PostMapping("/sync")
    public Result<Void> sync()
    {
        searchService.sync();
        return Result.ok();
    }
}
