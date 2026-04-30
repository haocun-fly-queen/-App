<template>
  <div class="login-log-container">
    <h2>用户登录日志</h2>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
          v-model="keyword"
          placeholder="搜索手机号/昵称/openid"
          style="width: 200px"
          clearable
          @clear="loadLogs"
          @keyup.enter="loadLogs"
      />
      <el-select v-model="loginType" placeholder="登录方式" style="width: 120px" clearable @change="loadLogs">
        <el-option label="微信登录" value="wechat" />
        <el-option label="手机号登录" value="phone" />
      </el-select>
      <el-select v-model="loginStatus" placeholder="状态" style="width: 100px" clearable @change="loadLogs">
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadLogs">搜索</el-button>
    </div>

    <!-- 日志表格 -->
    <el-table :data="logList" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="nickname" label="昵称" width="150" />
      <el-table-column prop="loginType" label="登录方式" width="100">
        <template #default="{ row }">
          <el-tag :type="row.loginType === 'wechat' ? 'success' : 'primary'">
            {{ row.loginType === 'wechat' ? '微信' : '手机号' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="loginStatus" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.loginStatus === 1 ? 'success' : 'danger'">
            {{ row.loginStatus === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="failReason" label="失败原因" width="150" show-overflow-tooltip />
      <el-table-column prop="loginIp" label="登录IP" width="140" />
      <el-table-column prop="loginTime" label="登录时间" width="180" />
      <el-table-column label="操作" width="80" fixed="right">
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
    <el-dialog v-model="detailVisible" title="登录详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentLog.userId }}</el-descriptions-item>
        <el-descriptions-item label="OpenID">{{ currentLog.openid }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentLog.phone }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentLog.nickname }}</el-descriptions-item>
        <el-descriptions-item label="登录方式">
          {{ currentLog.loginType === 'wechat' ? '微信登录' : '手机号登录' }}
        </el-descriptions-item>
        <el-descriptions-item label="登录状态">
          <el-tag :type="currentLog.loginStatus === 1 ? 'success' : 'danger'">
            {{ currentLog.loginStatus === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="失败原因" v-if="currentLog.failReason">
          {{ currentLog.failReason }}
        </el-descriptions-item>
        <el-descriptions-item label="登录IP">{{ currentLog.loginIp }}</el-descriptions-item>
        <el-descriptions-item label="网络类型">{{ currentLog.networkType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备品牌">{{ currentLog.deviceBrand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备型号">{{ currentLog.deviceModel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="小程序版本">{{ currentLog.appVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登录时间">{{ currentLog.loginTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getLoginLogList } from '@/api/loginLog'

const loading = ref(false)
const logList = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(15)
const keyword = ref('')
const loginType = ref('')
const loginStatus = ref('')

const detailVisible = ref(false)
const currentLog = ref({})

// 加载日志列表
const loadLogs = async () => {
  loading.value = true
  try {
    const res = await getLoginLogList({
      page: page.value,
      size: size.value,
      keyword: keyword.value,
      loginType: loginType.value,
      loginStatus: loginStatus.value
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

// 查看详情
const showDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.login-log-container {
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