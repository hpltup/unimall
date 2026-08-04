package com.unimall.user.pojo.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserVO
{
    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String email;

    private String avatar;

    private Integer gender;

    private LocalDate birthday;

    private Integer level;

    private LocalDateTime createTime;
}
