<template>
  <div class="admin-container">
    <h2>管理员管理</h2>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
          v-model="keyword"
          placeholder="搜索用户名/昵称"
          style="width: 200px"
          clearable
          @clear="loadAdmins"
          @keyup.enter="loadAdmins"
      />
      <el-button type="primary" @click="loadAdmins">搜索</el-button>
      <el-button type="success" @click="openAddDialog">+ 新增管理员</el-button>
    </div>

    <!-- 管理员表格 -->
    <el-table :data="adminList" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="nickname" label="昵称" width="150" />
      <el-table-column prop="roleName" label="角色" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginTime" label="最后登录时间" width="180" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button type="warning" size="small" @click="openResetPasswordDialog(row)">重置密码</el-button>
          <el-button
              v-if="row.status === 1"
              type="warning"
              size="small"
              @click="handleStatus(row.id, 0)"
          >禁用</el-button>
          <el-button
              v-else
              type="success"
              size="small"
              @click="handleStatus(row.id, 1)"
          >启用</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 15, 30, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadAdmins"
          @size-change="loadAdmins"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option
                v-for="role in roleList"
                :key="role.id"
                :label="role.roleName"
                :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAdmin" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetDialogVisible" title="重置密码" width="400px">
      <el-form :model="resetForm" :rules="resetRules" ref="resetFormRef" label-width="100px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetForm.password" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="resetForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmResetPassword" :loading="resetLoading">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminList, getAllRoles, addAdmin, updateAdmin, deleteAdmin, updateAdminStatus, resetPassword } from '@/api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const resetLoading = ref(false)
const adminList = ref([])
const roleList = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(15)
const keyword = ref('')

const dialogVisible = ref(false)
const resetDialogVisible = ref(false)
const dialogTitle = ref('新增管理员')
const isEdit = ref(false)
const editId = ref(null)
const currentAdmin = ref(null)
const formRef = ref(null)
const resetFormRef = ref(null)

const form = ref({
  username: '',
  nickname: '',
  password: '',
  roleId: ''
})

const resetForm = ref({
  password: '',
  confirmPassword: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const resetRules = {
  password: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetForm.value.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 加载管理员列表
const loadAdmins = async () => {
  loading.value = true
  try {
    const res = await getAdminList({
      page: page.value,
      size: size.value,
      keyword: keyword.value
    })
    if (res.code === 200) {
      adminList.value = res.data.list
      total.value = res.data.total
    }
  } catch (err) {
    ElMessage.error('加载管理员失败')
  } finally {
    loading.value = false
  }
}

// 加载角色列表
const loadRoles = async () => {
  try {
    const res = await getAllRoles()
    if (res.code === 200) {
      roleList.value = res.data
    }
  } catch (err) {
    console.error('加载角色失败', err)
  }
}

// 打开新增弹窗
const openAddDialog = () => {
  isEdit.value = false
  editId.value = null
  dialogTitle.value = '新增管理员'
  form.value = {
    username: '',
    nickname: '',
    password: '',
    roleId: ''
  }
  dialogVisible.value = true
}

// 打开编辑弹窗
const openEditDialog = (row) => {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑管理员'
  form.value = {
    username: row.username,
    nickname: row.nickname || '',
    password: '',
    roleId: row.roleId
  }
  dialogVisible.value = true
}

// 打开重置密码弹窗
const openResetPasswordDialog = (row) => {
  currentAdmin.value = row
  resetForm.value = { password: '', confirmPassword: '' }
  resetDialogVisible.value = true
}

// 保存管理员
const saveAdmin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      let res
      if (isEdit.value) {
        const data = { ...form.value }
        delete data.password
        res = await updateAdmin(editId.value, data)
      } else {
        res = await addAdmin(form.value)
      }
      if (res.code === 200) {
        ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
        dialogVisible.value = false
        loadAdmins()
      } else {
        ElMessage.error(res.message || '保存失败')
      }
    } catch (err) {
      ElMessage.error('保存失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 确认重置密码
const confirmResetPassword = async () => {
  if (!resetFormRef.value) return
  await resetFormRef.value.validate(async (valid) => {
    if (!valid) return

    resetLoading.value = true
    try {
      const res = await resetPassword(currentAdmin.value.id, { password: resetForm.value.password })
      if (res.code === 200) {
        ElMessage.success('密码重置成功')
        resetDialogVisible.value = false
      } else {
        ElMessage.error(res.message || '重置失败')
      }
    } catch (err) {
      ElMessage.error('重置失败')
    } finally {
      resetLoading.value = false
    }
  })
}

// 更新状态
const handleStatus = async (id, status) => {
  const action = status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}该管理员吗？`, '提示', { type: 'warning' })
    const res = await updateAdminStatus(id, status)
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      loadAdmins()
    }
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 删除管理员
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该管理员吗？', '提示', { type: 'warning' })
    const res = await deleteAdmin(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadAdmins()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadAdmins()
  loadRoles()
})
</script>

<style scoped>
.admin-container {
  background: white;
  padding: 20px;
  border-radius: 8px;
}
.search-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>