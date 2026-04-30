<template>
  <div class="permission-container">
    <h2>权限分配</h2>

    <!-- 角色选择 -->
    <div class="role-select">
      <span class="label">选择角色：</span>
      <el-select v-model="selectedRoleId" placeholder="请选择角色" @change="loadRolePermissions" style="width: 200px">
        <el-option
            v-for="role in roleList"
            :key="role.id"
            :label="role.roleName"
            :value="role.id"
        />
      </el-select>
    </div>

    <!-- 权限树 -->
    <div class="permission-tree" v-if="selectedRoleId">
      <el-tree
          ref="treeRef"
          :data="permissionTree"
          show-checkbox
          node-key="id"
          :props="treeProps"
          :default-checked-keys="checkedKeys"
          :expand-on-click-node="false"
      />
      <div class="tree-buttons">
        <el-button @click="expandAll">展开全部</el-button>
        <el-button @click="collapseAll">收起全部</el-button>
        <el-button type="primary" @click="savePermissions" :loading="saving">保存权限</el-button>
      </div>
    </div>

    <!-- 未选择角色提示 -->
    <div v-else class="empty-tip">
      <el-empty description="请先选择角色" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllRoles } from '@/api/role'
import { getPermissionTree, getRolePermissions, assignPermissions } from '@/api/permission'

const roleList = ref([])
const selectedRoleId = ref(null)
const permissionTree = ref([])
const checkedKeys = ref([])
const saving = ref(false)
const treeRef = ref(null)

const treeProps = {
  label: 'name',
  children: 'children'
}

// 加载角色列表
const loadRoles = async () => {
  try {
    const res = await getAllRoles()
    if (res.code === 200) {
      roleList.value = res.data
    }
  } catch (err) {
    ElMessage.error('加载角色失败')
  }
}

// 加载权限树
const loadPermissionTree = async () => {
  try {
    const res = await getPermissionTree()
    if (res.code === 200) {
      permissionTree.value = res.data
    }
  } catch (err) {
    ElMessage.error('加载权限树失败')
  }
}

// 加载角色已分配的权限
const loadRolePermissions = async () => {
  if (!selectedRoleId.value) return

  try {
    const res = await getRolePermissions(selectedRoleId.value)
    if (res.code === 200) {
      checkedKeys.value = res.data || []
      // 设置树的选中状态
      if (treeRef.value) {
        treeRef.value.setCheckedKeys(checkedKeys.value)
      }
    }
  } catch (err) {
    ElMessage.error('加载角色权限失败')
  }
}

// 保存权限
const savePermissions = async () => {
  if (!selectedRoleId.value) {
    ElMessage.warning('请先选择角色')
    return
  }

  saving.value = true
  try {
    const checkedNodes = treeRef.value.getCheckedKeys()
    const halfCheckedNodes = treeRef.value.getHalfCheckedKeys()
    const permissionIds = [...checkedNodes, ...halfCheckedNodes]

    const res = await assignPermissions(selectedRoleId.value, permissionIds)
    if (res.code === 200) {
      ElMessage.success('权限保存成功')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (err) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 展开全部
const expandAll = () => {
  const nodes = treeRef.value.store.nodesMap
  for (const key in nodes) {
    nodes[key].expanded = true
  }
}

// 收起全部
const collapseAll = () => {
  const nodes = treeRef.value.store.nodesMap
  for (const key in nodes) {
    nodes[key].expanded = false
  }
}

onMounted(() => {
  loadRoles()
  loadPermissionTree()
})
</script>

<style scoped>
.permission-container {
  background: white;
  padding: 20px;
  border-radius: 8px;
}

.role-select {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e4e7ed;
}

.label {
  font-size: 14px;
  color: #606266;
}

.permission-tree {
  max-height: 500px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 15px;
}

.tree-buttons {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  justify-content: center;
}

.empty-tip {
  padding: 50px;
  text-align: center;
}
</style>