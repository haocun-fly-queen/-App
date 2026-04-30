package com.eatnotfat.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SimpleTest {
    public static void main(String[] args) {
        String url = "jdbc:mysql://dpshmy-pmfd2xs59xc8skne-pub.proxy.dms.aliyuncs.com:3306/health?useSSL=false&serverTimezone=Asia/Shanghai";
        String username = "prod_admin";
        String password = "123456";  // 改成实际密码

        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("✅ 数据库连接成功！");
            System.out.println("数据库: " + conn.getMetaData().getURL());
            conn.close();
        }  catch (SQLException e) {
            System.out.println("❌ 连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}