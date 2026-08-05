package com.unimall.admin.config;

import com.unimall.admin.mapper.IAdminUserMapper;
import com.unimall.admin.pojo.entity.AdminUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动初始化：admin_user 表为空时创建初始管理员（admin / 123456）
 */
@Component
public class AdminInitializer implements CommandLineRunner
{
    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    private final IAdminUserMapper adminUserMapper;

    public AdminInitializer(IAdminUserMapper adminUserMapper)
    {
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public void run(String... args)
    {
        Long count = adminUserMapper.selectCount(null);
        if (count != null && count > 0)
        {
            return;
        }
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        AdminUser admin = new AdminUser();
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("123456"));
        admin.setStatus(1);
        adminUserMapper.insert(admin);
        logger.info("已初始化管理员账号：admin / 123456（请尽快修改）");
    }
}
