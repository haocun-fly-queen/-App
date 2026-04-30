<template>
  <div class="config-container">
    <h2>系统配置</h2>

    <el-table :data="configList" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="configKey" label="配置键" width="200" />
      <el-table-column prop="configValue" label="配置值" min-width="200">
        <template #default="{ row }">
          <el-input
              v-model="row.configValue"
              :placeholder="row.description"
              @change="handleUpdateConfig(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" width="200" />
      <el-table-column prop="configType" label="类型" width="100" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigList, updateConfig } from '@/api/config'

const loading = ref(false)
const configList = ref([])

// 加载配置列表
const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await getConfigList()
    if (res.code === 200) {
      configList.value = res.data
    }
  } catch (err) {
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

// 更新配置（改名避免冲突）
const handleUpdateConfig = async (row) => {
  try {
    const res = await updateConfig(row.configKey, { value: row.configValue })
    if (res.code === 200) {
      ElMessage.success('更新成功')
    }
  } catch (err) {
    ElMessage.error('更新失败')
    loadConfigs() // 刷新恢复原值
  }
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.config-container {
  background: white;
  padding: 20px;
  border-radius: 8px;
}
</style>