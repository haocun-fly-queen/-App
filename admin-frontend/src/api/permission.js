import request from '@/utils/request'

// 获取权限树
export const getPermissionTree = () => {
    return request.get('/admin/permission/tree')
}

// 获取角色已分配的权限
export const getRolePermissions = (roleId) => {
    return request.get(`/admin/permission/role/${roleId}`)
}

// 分配权限给角色
export const assignPermissions = (roleId, permissionIds) => {
    return request.put(`/admin/permission/role/${roleId}`, { permissionIds })
}