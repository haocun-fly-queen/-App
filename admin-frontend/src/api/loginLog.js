import request from '@/utils/request'

// 获取用户登录日志列表
export const getLoginLogList = (params) => {
    return request.get('/admin/login-log/list', { params })
}