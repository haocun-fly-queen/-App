<template>
  <div class="role-container">
    <h2>角色管理</h2>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
          v-model="keyword"
          placeholder="搜索角色名称/编码"
          style="width: 200px"
          clearable
          @clear="loadRoles"
          @keyup.enter="loadRoles"
      />
      <el-button type="primary" @click="loadRoles">搜索</el-button>
      <el-button type="success" @click="openAddDialog">+ 新增角色</el-button>
    </div>

    <!-- 角色表格 -->
    <el-table :data="roleList" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roleName" label="角色名称" width="150" />
      <el-table-column prop="roleCode" label="角色编码" width="150" />
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
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
          <el-button
              type="danger"
              size="small"
              :disabled="isBuiltInRole(row.roleCode)"
              @click="handleDelete(row.id)"
          >删除</el-button>
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
          @current-change="loadRoles"
          @size-change="loadRoles"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="请输入角色编码（如：admin, operator）" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoleList, getAllRoles, addRole, updateRole, deleteRole, updateRoleStatus } from '@/api/role'

const loading = ref(false)
const submitLoading = ref(false)
const roleList = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(15)
const keyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)
const form = ref({
  roleName: '',
  roleCode: '',
  description: ''
})

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

// 判断是否为内置角色（不可删除）
const isBuiltInRole = (roleCode) => {
  return ['admin', 'operator', 'viewer'].includes(roleCode)
}

// 加载角色列表
const loadRoles = async () => {
  loading.value = true
  try {
    const res = await getRoleList({
      page: page.value,
      size: size.value,
      keyword: keyword.value
    })
    if (res.code === 200) {
      roleList.value = res.data.list
      total.value = res.data.total
    }
  } catch (err) {
    ElMessage.error('加载角色失败')
  } finally {
    loading.value = false
  }
}

// 打开新增弹窗
const openAddDialog = () => {
  isEdit.value = false
  editId.value = null
  dialogTitle.value = '新增角色'
  form.value = {
    roleName: '',
    roleCode: '',
    description: ''
  }
  dialogVisible.value = true
}

// 打开编辑弹窗
const openEditDialog = (row) => {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑角色'
  form.value = {
    roleName: row.roleName,
    roleCode: row.roleCode,
    description: row.description || ''
  }
  dialogVisible.value = true
}

// 保存角色
const saveRole = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      let res
      if (isEdit.value) {
        res = await updateRole(editId.value, form.value)
      } else {
        res = await addRole(form.value)
      }
      if (res.code === 200) {
        ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
        dialogVisible.value = false
        loadRoles()
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

// 更新状态
const handleStatus = async (id, status) => {
  const action = status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}该角色吗？`, '提示', { type: 'warning' })
    const res = await updateRoleStatus(id, status)
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      loadRoles()
    }
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 删除角色
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该角色吗？', '提示', { type: 'warning' })
    const res = await deleteRole(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadRoles()
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
  loadRoles()
})
</script>

<style scoped>
.role-container {
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