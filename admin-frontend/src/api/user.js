import request from '@/utils/request'

// 获取用户列表
export const getUserList = (params) => {
    return request({
        url: '/admin/user/list',
        method: 'GET',
        params
    })
}

// 获取用户详情
export const getUserDetail = (id) => {
    return request({
        url: `/admin/user/${id}`,
        method: 'GET'
    })
}

// 更新用户状态（启用/禁用）
export const updateUserStatus = (id, status) => {
    return request({
        url: `/admin/user/${id}/status`,
        method: 'PUT',
        data: { status }
    })
}
export const getUserActivity = (id) => {
    return request({
        url: `/admin/user/${id}/activity`,
        method: 'GET'
    })
}