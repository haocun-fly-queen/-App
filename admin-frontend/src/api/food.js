import request from '@/utils/request'

// 获取食物列表
export const getFoodList = (params) => {
    return request({
        url: '/admin/food/list',
        method: 'GET',
        params
    })
}

// 获取分类列表
export const getCategories = () => {
    return request({
        url: '/admin/food/categories',
        method: 'GET'
    })
}

// 获取食物详情
export const getFoodDetail = (id) => {
    return request({
        url: `/admin/food/${id}`,
        method: 'GET'
    })
}

// 新增食物
export const addFood = (data) => {
    return request({
        url: '/admin/food',
        method: 'POST',
        data
    })
}

// 编辑食物
export const updateFood = (id, data) => {
    return request({
        url: `/admin/food/${id}`,
        method: 'PUT',
        data
    })
}

// 删除食物
export const deleteFood = (id) => {
    return request({
        url: `/admin/food/${id}`,
        method: 'DELETE'
    })
}

// 更新食物状态
export const updateFoodStatus = (id, status) => {
    return request({
        url: `/admin/food/${id}/status`,
        method: 'PUT',
        data: { status }
    })
}
// 获取AI识别日志列表
export const getAiLogs = (params) => {
    return request.get('/admin/user/ai-logs', { params })
}

// 校准AI识别匹配
export const calibrateAiLog = (id, foodIds) => {
    return request.put(`/admin/user/ai-logs/${id}/calibrate`, { foodIds })
}