<template>
  <div class="main-layout">
    <div class="sidebar">
      <div class="logo">
        <h2>吃不胖后台</h2>
      </div>
      <el-menu router>
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>数据看板</span>
        </el-menu-item>

        <!-- 只有 admin 和 operator 能看到用户管理 -->
        <el-menu-item index="/user" v-if="hasRole(['admin', 'operator'])">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>

        <!-- 只有 admin 和 operator 能看到食物库管理 -->
        <el-menu-item index="/food" v-if="hasRole(['admin', 'operator'])">
          <el-icon><Apple /></el-icon>
          <span>食物库管理</span>
        </el-menu-item>

        <!-- 系统管理：只有 admin 能看到 -->
        <el-sub-menu index="system" v-if="hasRole(['admin'])">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/config">
            <el-icon><Tools /></el-icon>
            <span>系统配置</span>
          </el-menu-item>
          <el-menu-item index="/system/operlog">
            <el-icon><Document /></el-icon>
            <span>操作日志</span>
          </el-menu-item>
          <el-menu-item index="/system/role">
            <el-icon><UserFilled /></el-icon>
            <span>角色管理</span>
          </el-menu-item>
          <el-menu-item index="/system/admin">
            <el-icon><User /></el-icon>
            <span>管理员管理</span>
          </el-menu-item>
          <el-menu-item index="/system/permission">
            <el-icon><Lock /></el-icon>
            <span>权限分配</span>
          </el-menu-item>
          <el-menu-item index="/system/loginLog">
            <el-icon><List /></el-icon>
            <span>用户登录日志</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </div>
    <div class="content">
      <div class="header">
        <div class="header-right">
          <span>{{ adminName }}</span>
          <span class="role-badge">{{ adminRole }}</span>
          <el-button type="danger" size="small" @click="logout">退出</el-button>
        </div>
      </div>
      <div class="main">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataLine, User, Apple, Setting, Tools, Document, UserFilled, Lock, List } from '@element-plus/icons-vue'

const router = useRouter()
const adminName = ref('管理员')
const adminRole = ref('')

// 判断是否有指定角色
const hasRole = (roles) => {
  const currentRole = adminRole.value
  if (!currentRole) return false
  return roles.includes(currentRole)
}

onMounted(() => {
  const info = localStorage.getItem('admin_info')
  if (info) {
    const data = JSON.parse(info)
    adminName.value = data.nickname || data.username || '管理员'
    adminRole.value = data.roleCode || ''
  }
})

const logout = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_info')
  ElMessage.success('已退出')
  router.push('/login')
}
</script>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
}
.sidebar {
  width: 220px;
  background-color: #f0f2f5;
  color: #333;
  border-right: 1px solid #e4e7ed;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #e4e7ed;
}
.logo h2 {
  color: #409eff;
  font-size: 18px;
  margin: 0;
}
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.header {
  height: 60px;
  background-color: white;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 20px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}
.role-badge {
  background-color: #ecf5ff;
  color: #409eff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}
.main {
  flex: 1;
  padding: 20px;
  background-color: #f5f7fa;
  overflow: auto;
}

.el-menu {
  border-right: none;
  background-color: #f0f2f5;
}
.el-menu-item, .el-sub-menu__title {
  color: #606266;
}
.el-menu-item:hover, .el-sub-menu__title:hover {
  background-color: #e6e9f0;
  color: #409eff;
}
.el-menu-item.is-active {
  background-color: #ecf5ff;
  color: #409eff;
  border-right: 3px solid #409eff;
}
</style>