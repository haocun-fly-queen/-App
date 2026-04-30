import request from '@/utils/request'

// 获取配置列表
export const getConfigList = () => {
    return request.get('/admin/config/list')
}

// 更新配置
export const updateConfig = (key, data) => {
    return request.put(`/admin/config/${key}`, data)
}