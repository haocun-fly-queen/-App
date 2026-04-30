import request from '@/utils/request'

// 获取数据看板统计
export const getDashboard = () => {
    return request.get('/admin/statistics/dashboard')
}