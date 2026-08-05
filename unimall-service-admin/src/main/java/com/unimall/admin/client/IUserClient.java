package com.unimall.admin.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.common.dto.UserStatusDTO;
import com.unimall.common.result.Result;
import com.unimall.common.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "unimall-service-user")
public interface IUserClient
{
    @GetMapping("/user/internal/admin-list")
    Result<Page<UserVO>> adminList(@RequestParam("pageNum") Integer pageNum,
                                   @RequestParam("pageSize") Integer pageSize,
                                   @RequestParam(value = "keyword", required = false) String keyword);

    @PutMapping("/user/internal/admin-status")
    Result<Void> adminUpdateStatus(@RequestBody UserStatusDTO dto);
}
