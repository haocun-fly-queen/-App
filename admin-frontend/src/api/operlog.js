import request from '@/utils/request'

// 获取日志列表
export const getLogList = (params) => {
    return request.get('/admin/operlog/list', { params })
}

// 获取日志详情
export const getLogDetail = (id) => {
    return request.get(`/admin/operlog/${id}`)
}

// 清空日志
export const cleanLog = () => {
    return request.delete('/admin/operlog/clean')
}