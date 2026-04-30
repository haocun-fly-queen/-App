import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/login/index.vue')
    },
    {
        path: '/',
        component: () => import('@/layouts/MainLayout.vue'),
        redirect: '/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('@/views/dashboard/index.vue'),
                meta: { title: '数据看板', icon: 'data-line' }
            },
            {
                path: 'user',
                name: 'User',
                component: () => import('@/views/user/index.vue'),
                meta: { title: '用户管理', icon: 'user' }
            },
            {
                path: 'food',
                name: 'Food',
                component: () => import('@/views/food/index.vue'),
                meta: { title: '食物库管理', icon: 'food' }
            },
            {
                path: 'system',
                name: 'System',
                redirect: '/system/config',
                meta: { title: '系统管理', icon: 'setting' },
                children: [
                    {
                        path: 'config',
                        name: 'SystemConfig',
                        component: () => import('@/views/system/config.vue'),
                        meta: { title: '系统配置' }
                    },
                    {
                        path: 'operlog',
                        name: 'SystemOperLog',
                        component: () => import('@/views/system/operlog.vue'),
                        meta: { title: '操作日志' }
                    },
                    {
                        path: 'role',
                        name: 'SystemRole',
                        component: () => import('@/views/system/role.vue'),
                        meta: { title: '角色管理' }
                    },
                    {
                        path: 'admin',
                        name: 'SystemAdmin',
                        component: () => import('@/views/system/admin.vue'),
                        meta: { title: '管理员管理' }
                    },
                    {
                        path: 'permission',
                        name: 'SystemPermission',
                        component: () => import('@/views/system/permission.vue'),
                        meta: { title: '权限分配' }
                    },
                    {
                        path: 'loginLog',
                        name: 'LoginLog',
                        component: () => import('@/views/system/loginLog.vue'),
                        meta: { title: '用户登录日志' }
                    }

                ]
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('admin_token')
    if (to.path !== '/login' && !token) {
        next('/login')
    } else {
        next()
    }
})

export default router