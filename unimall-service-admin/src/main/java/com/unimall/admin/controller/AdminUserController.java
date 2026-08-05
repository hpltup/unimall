package com.unimall.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.admin.client.IUserClient;
import com.unimall.common.dto.UserStatusDTO;
import com.unimall.common.result.Result;
import com.unimall.common.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/user")
public class AdminUserController
{
    private final IUserClient userClient;

    public AdminUserController(IUserClient userClient)
    {
        this.userClient = userClient;
    }

    @GetMapping("/list")
    public Result<Page<UserVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                     @RequestParam(required = false) String keyword)
    {
        return userClient.adminList(pageNum, pageSize, keyword);
    }

    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestBody UserStatusDTO dto)
    {
        return userClient.adminUpdateStatus(dto);
    }
}
