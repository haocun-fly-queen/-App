package com.eatnotfat.backend.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.eatnotfat.backend.entity.DietRecord;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.mapper.DietRecordMapper;
import com.eatnotfat.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
@Component
public class MealReminderScheduler {
//提醒功能
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DietRecordMapper dietRecordMapper;

    /**
     * 早餐提醒：每天9点检查（假设早餐时间8点，1小时后检查）
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void breakfastReminder() {
        checkAndRemind(1, "早餐");
    }

    /**
     * 午餐提醒：每天13点检查（假设午餐时间12点，1小时后检查）
     */
    @Scheduled(cron = "0 0 13 * * ?")
    public void lunchReminder() {
        checkAndRemind(2, "午餐");
    }

    /**
     * 晚餐提醒：每天20点检查（假设晚餐时间19点，1小时后检查）
     */
    @Scheduled(cron = "0 0 20 * * ?")
    public void dinnerReminder() {
        checkAndRemind(3, "晚餐");
    }

    private void checkAndRemind(int mealType, String mealName) {
        LocalDate today = LocalDate.now();

        // 用 MyBatis-Plus 已有的 selectList，不需要新增 Mapper 方法
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("status", 1);
        List<User> users = userMapper.selectList(query);

        for (User user : users) {
            Long userId = user.getId();

            // 检查该用户今天是否记录了这一餐
            List<DietRecord> records = dietRecordMapper.selectByDate(userId, today.toString());
            boolean hasMeal = false;
            for (DietRecord record : records) {
                if (record.getMealType() != null && record.getMealType() == mealType) {
                    hasMeal = true;
                    break;
                }
            }

            if (!hasMeal) {
                // 用户没有记录这一餐，发送提醒
                sendReminder(userId, mealName);

                // TODO: 发送微信订阅消息（需要用户授权 + 模板配置）
                // String openId = user.getWxOpenid();
                // if (openId != null && !openId.isEmpty()) {
                //     wechatService.sendSubscribeMessage(openId, mealName);
                // }

                System.out.println("[餐次提醒] 用户 " + userId + " 未记录" + mealName);
            }
        }
    }

    private void sendReminder(Long userId, String mealName) {
        // 方案1：存入数据库，前端轮询获取
        // 方案2：WebSocket 推送
        // 方案3：微信订阅消息

        // 当前实现：前端进入看板时通过 alerts 接口自动获取提醒
        System.out.println("发送提醒：用户" + userId + "，请记录" + mealName);
    }
}
