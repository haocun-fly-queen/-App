<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="title">吃不胖 - 管理后台</h2>
      <el-form :model="form">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" @click="handleLogin" :loading="loading">登录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const handleLogin = async () => {
  loading.value = true
  try {
    const res = await login(form)
    if (res.code === 200) {
      // 保存 token
      localStorage.setItem('admin_token', res.data.token)

      // 保存用户信息（包含 roleCode）
      localStorage.setItem('admin_info', JSON.stringify({
        userId: res.data.userId,
        username: res.data.username,
        nickname: res.data.nickname,
        roleCode: res.data.roleCode
      }))

      ElMessage.success('登录成功')
      router.push('/dashboard')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (err) {
    console.error('登录失败', err)
    ElMessage.error('登录失败，请检查网络')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}
.title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
</style>