package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.Admin;

/**
 * 管理员 Service 接口
 * IService 是 MyBatis-Plus 提供的通用 Service 接口
 * 包含 save(), update(), getById() 等基础方法
 */
public interface AdminService extends IService<Admin> {

    /**
     * 管理员登录
     * @param username 用户名
     * @param password 密码（明文）
     * @return 登录成功返回 Admin 对象，失败返回 null
     */
    Admin login(String username, String password);

    /**
     * 更新最后登录信息
     * @param id 管理员ID
     * @param ip 登录IP
     */
    void updateLoginInfo(Long id, String ip);

    // ========== 新增方法（用于管理员管理）==========

    /**
     * 根据用户名查询管理员
     * @param username 用户名
     * @return 管理员对象
     */
    Admin getByUsername(String username);

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @param excludeId 排除的ID（编辑时排除自己）
     * @return true-存在，false-不存在
     */
    boolean isUsernameExist(String username, Long excludeId);
}