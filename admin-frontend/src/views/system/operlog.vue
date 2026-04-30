<template>
  <div class="operlog-container">
    <h2>操作日志</h2>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
          v-model="searchForm.username"
          placeholder="操作人"
          style="width: 150px"
          clearable
          @clear="loadLogs"
      />
      <el-input
          v-model="searchForm.module"
          placeholder="模块"
          style="width: 150px"
          clearable
          @clear="loadLogs"
      />
      <el-select v-model="searchForm.status" placeholder="状态" style="width: 120px" clearable @change="loadLogs">
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadLogs">搜索</el-button>
      <el-button type="danger" @click="handleClean">清空日志</el-button>
    </div>

    <!-- 日志表格 -->
    <el-table :data="logList" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="操作人" width="120" />
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column prop="operation" label="操作" width="120" />
      <el-table-column prop="requestUrl" label="请求URL" width="200" />
      <el-table-column prop="ipAddress" label="IP地址" width="140" />
      <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="操作时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="showDetail(row)">详情</el-button>
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
          @current-change="loadLogs"
          @size-change="loadLogs"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.username }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentLog.operation }}</el-descriptions-item>
        <el-descriptions-item label="请求URL">{{ currentLog.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ currentLog.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre>{{ formatJson(currentLog.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="返回结果">
          <pre>{{ formatJson(currentLog.responseResult) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog.durationMs }} ms</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="currentLog.errorMsg">
          {{ currentLog.errorMsg }}
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentLog.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLogList, cleanLog, getLogDetail } from '@/api/operlog'

const loading = ref(false)
const logList = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(15)
const searchForm = ref({ username: '', module: '', status: '' })
const detailVisible = ref(false)
const currentLog = ref({})

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await getLogList({
      page: page.value,
      size: size.value,
      username: searchForm.value.username,
      module: searchForm.value.module,
      status: searchForm.value.status
    })
    if (res.code === 200) {
      logList.value = res.data.list
      total.value = res.data.total
    }
  } catch (err) {
    ElMessage.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

const showDetail = async (row) => {
  try {
    const res = await getLogDetail(row.id)
    if (res.code === 200) {
      currentLog.value = res.data
      detailVisible.value = true
    }
  } catch (err) {
    ElMessage.error('获取详情失败')
  }
}

const handleClean = async () => {
  try {
    await ElMessageBox.confirm('确定要清空30天前的日志吗？', '提示', { type: 'warning' })
    const res = await cleanLog()
    if (res.code === 200) {
      ElMessage.success('清空成功')
      loadLogs()
    }
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

const formatJson = (str) => {
  if (!str) return ''
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch (e) {
    return str
  }
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.operlog-container {
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
pre {
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  font-size: 12px;
}
</style>