package com.unimall.user.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO
{
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z]\\w{3,19}$", message = "用户名需以字母开头，4~20位字母数字下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在6~32位之间")
    private String password;

    private String nickname;
}
