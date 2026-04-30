package com.eatnotfat.backend.utils;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class WeChatUtil {

    // 微信小程序配置
    private static final String APP_ID = "wxb7873c565ccbfb58";
    private static final String APP_SECRET = "55c7499208467fcf071d2314d52b7067";

    // 开发环境开关 - 上线前改为 false
    private static final boolean IS_DEV_MODE = true;

    // 开发环境固定的 openid（同一个用户）
    private static final String DEV_OPENID = "test_fixed_openid_001";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 通过 code 获取 openid 和 session_key
     */
    public Map<String, String> getOpenIdAndSessionKey(String code) {
        // 开发环境：使用固定 openid，避免每次创建新用户
        if (IS_DEV_MODE) {
            System.out.println("========== 开发模式：使用固定 openid ==========");
            System.out.println("固定 openid: " + DEV_OPENID);
            Map<String, String> data = new HashMap<>();
            data.put("openid", DEV_OPENID);
            data.put("session_key", "dev_session_key");
            return data;
        }

        // 生产环境：真实微信接口调用
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + APP_ID
                + "&secret=" + APP_SECRET + "&js_code=" + code + "&grant_type=authorization_code";

        try {
            System.out.println("========== 真实微信登录 ==========");
            System.out.println("请求URL: " + url);
            String result = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.parseObject(result);

            System.out.println("微信接口返回: " + result);

            Map<String, String> data = new HashMap<>();

            if (json.getString("errcode") != null && !"0".equals(json.getString("errcode"))) {
                System.err.println("微信接口返回错误: errcode=" + json.getString("errcode") + ", errmsg=" + json.getString("errmsg"));
                throw new RuntimeException("微信接口返回错误: " + json.getString("errmsg"));
            }

            data.put("openid", json.getString("openid"));
            data.put("session_key", json.getString("session_key"));

            System.out.println("获取到的 openid: " + json.getString("openid"));
            System.out.println("获取到的 session_key: " + json.getString("session_key"));

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取微信openid失败: " + e.getMessage());
        }
    }

    /**
     * 解密用户信息
     */
    public UserInfo decryptUserInfo(String encryptedData, String iv, String sessionKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(sessionKey);
            byte[] ivBytes = Base64.getDecoder().decode(iv);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String decrypted = new String(decryptedBytes, StandardCharsets.UTF_8);

            JSONObject json = JSONObject.parseObject(decrypted);

            UserInfo userInfo = new UserInfo();
            userInfo.setOpenId(json.getString("openId"));
            userInfo.setNickName(json.getString("nickName"));
            userInfo.setAvatarUrl(json.getString("avatarUrl"));
            userInfo.setGender(json.getInteger("gender"));
            userInfo.setCountry(json.getString("country"));
            userInfo.setProvince(json.getString("province"));
            userInfo.setCity(json.getString("city"));

            return userInfo;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("解密用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 用户信息类
     */
    public static class UserInfo {
        private String openId;
        private String nickName;
        private String avatarUrl;
        private Integer gender;
        private String country;
        private String province;
        private String city;

        public String getOpenId() { return openId; }
        public void setOpenId(String openId) { this.openId = openId; }
        public String getNickName() { return nickName; }
        public void setNickName(String nickName) { this.nickName = nickName; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public Integer getGender() { return gender; }
        public void setGender(Integer gender) { this.gender = gender; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
    }
}