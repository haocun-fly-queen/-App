import request from '@/utils/request'

// 获取角色列表（分页）
export const getRoleList = (params) => {
    return request.get('/admin/role/list', { params })
}

// 获取所有角色（下拉框用）
export const getAllRoles = () => {
    return request.get('/admin/role/all')
}

// 新增角色
export const addRole = (data) => {
    return request.post('/admin/role', data)
}

// 更新角色
export const updateRole = (id, data) => {
    return request.put(`/admin/role/${id}`, data)
}

// 删除角色
export const deleteRole = (id) => {
    return request.delete(`/admin/role/${id}`)
}

// 更新角色状态
export const updateRoleStatus = (id, status) => {
    return request.put(`/admin/role/${id}/status`, { status })
}