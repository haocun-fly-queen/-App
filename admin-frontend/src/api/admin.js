import request from '@/utils/request'

// 获取管理员列表
export const getAdminList = (params) => {
    return request.get('/admin/manage/list', { params })
}

// 获取所有角色（下拉框用）
export const getAllRoles = () => {
    return request.get('/admin/role/all')
}

// 新增管理员
export const addAdmin = (data) => {
    return request.post('/admin/manage', data)
}

// 更新管理员
export const updateAdmin = (id, data) => {
    return request.put(`/admin/manage/${id}`, data)
}

// 删除管理员
export const deleteAdmin = (id) => {
    return request.delete(`/admin/manage/${id}`)
}

// 更新管理员状态
export const updateAdminStatus = (id, status) => {
    return request.put(`/admin/manage/${id}/status`, { status })
}

// 重置密码
export const resetPassword = (id, data) => {
    return request.put(`/admin/manage/${id}/password`, data)
}