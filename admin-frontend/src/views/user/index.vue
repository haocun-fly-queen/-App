<template>
  <div class="user-container">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索昵称/手机号" style="width: 250px" clearable @clear="loadData" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">搜索</el-button>
    </div>

    <el-table :data="userList" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="nickname" label="昵称" width="150" />
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column prop="gender" label="性别" width="80">
        <template #default="{ row }">
          {{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '未知' }}
        </template>
      </el-table-column>
      <el-table-column prop="height" label="身高(cm)" width="100" />
      <el-table-column prop="currentWeight" label="体重(kg)" width="100" />
      <el-table-column prop="goalType" label="目标" width="100">
        <template #default="{ row }">
          {{ row.goalType === 1 ? '减脂' : row.goalType === 2 ? '增肌' : '保持' }}
        </template>
      </el-table-column>
      <el-table-column label="活跃情况" width="120">
        <template #default="{ row }">
          <div class="activity-cell">
            <span class="active-days">活跃: {{ getActiveDays(row.id) }}天/7</span>
            <el-progress :percentage="getActiveRate(row.id)" :color="getProgressColor(getActiveRate(row.id))" :stroke-width="8" />
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="info" size="small" @click="showActivityDetail(row)">活跃详情</el-button>
          <el-button v-if="row.status === 1" type="danger" size="small" @click="handleStatus(row.id, 0)">禁用</el-button>
          <el-button v-else type="success" size="small" @click="handleStatus(row.id, 1)">启用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 活跃详情弹窗 -->
    <el-dialog v-model="activityDialogVisible" :title="`${selectedUser?.nickname} - 近7天活跃情况`" width="500px">
      <div class="activity-detail">
        <div class="summary">
          <span>活跃天数: {{ activityData.activeDays }} / 7天</span>
          <span>活跃率: {{ activityData.activeRate?.toFixed(1) }}%</span>
        </div>
        <div class="week-chart">
          <div v-for="day in activityData.weekData" :key="day.date" class="day-item">
            <div class="day-date">{{ day.date.substring(5) }}</div>
            <div class="day-bar">
              <div class="bar-fill" :style="{ width: getBarWidth(day.calories) + '%', backgroundColor: day.hasRecord ? '#67c23a' : '#e6a23c' }"></div>
            </div>
            <div class="day-calorie">{{ day.calories > 0 ? day.calories + 'kcal' : '无记录' }}</div>
          </div>
        </div>
      </div>
    </el-dialog>

    <div class="pagination">
      <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadData"
          @size-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, updateUserStatus, getUserActivity } from '@/api/user'

const loading = ref(false)
const userList = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')

// 只存储活跃天数（用于列表页进度条）
const activeDaysCache = ref({})

// 存储完整活跃详情（用于弹窗）
const activityDetailCache = ref({})

// 弹窗相关
const activityDialogVisible = ref(false)
const selectedUser = ref(null)
const activityData = ref({ weekData: [], activeDays: 0, activeRate: 0 })

// 获取活跃天数（从缓存读取，没有则返回0）
const getActiveDays = (userId) => {
  return activeDaysCache.value[userId] ?? 0
}

// 获取活跃率（用于进度条）
const getActiveRate = (userId) => {
  const days = activeDaysCache.value[userId] ?? 0
  return Math.round((days / 7) * 100)
}

// 获取进度条颜色
const getProgressColor = (rate) => {
  if (rate >= 70) return '#67c23a'
  if (rate >= 30) return '#e6a23c'
  return '#f56c6c'
}

// 获取柱状图宽度
const getBarWidth = (calories) => {
  if (calories === 0) return 0
  return Math.min(100, calories / 500 * 100)
}

// 加载单个用户的活跃情况（只获取天数，用于缓存）
const loadUserActiveDays = async (userId) => {
  // 如果已经有缓存，直接返回
  if (activeDaysCache.value[userId] !== undefined) return

  try {
    const res = await getUserActivity(userId)
    if (res.code === 200) {
      // 缓存活跃天数
      activeDaysCache.value[userId] = res.data.activeDays || 0
      // 同时缓存完整详情
      activityDetailCache.value[userId] = res.data
    }
  } catch (err) {
    console.error('加载活跃情况失败', err)
    // 失败时设为0，避免重复请求
    activeDaysCache.value[userId] = 0
  }
}

// 显示活跃详情弹窗
const showActivityDetail = async (user) => {
  selectedUser.value = user

  // 如果没有完整详情缓存，先加载
  if (!activityDetailCache.value[user.id]) {
    try {
      const res = await getUserActivity(user.id)
      if (res.code === 200) {
        activityDetailCache.value[user.id] = res.data
        activeDaysCache.value[user.id] = res.data.activeDays || 0
      }
    } catch (err) {
      console.error('加载活跃详情失败', err)
    }
  }

  activityData.value = activityDetailCache.value[user.id] || { weekData: [], activeDays: 0, activeRate: 0 }
  activityDialogVisible.value = true
}

// 加载用户数据（不自动加载活跃情况）
const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserList({
      page: page.value,
      size: size.value,
      keyword: keyword.value
    })
    if (res.code === 200) {
      userList.value = res.data.list || res.data.records || []
      total.value = res.data.total

      // ⭐ 注意：这里不再循环调用活跃接口
      // 列表页的活跃天数初始显示0，点击详情时才加载
    }
  } catch (err) {
    console.error('加载用户失败', err)
    ElMessage.error('加载用户失败')
  } finally {
    loading.value = false
  }
}

// 更新用户状态
const handleStatus = async (id, status) => {
  const action = status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}该用户吗？`, '提示', { type: 'warning' })
    const res = await updateUserStatus(id, status)
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      loadData()
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('操作失败', err)
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.user-container {
  background: white;
  padding: 20px;
  border-radius: 8px;
}

.search-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.activity-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.active-days {
  font-size: 12px;
  color: #666;
}

.activity-detail {
  padding: 10px;
}

.summary {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 8px;
  font-weight: bold;
}

.week-chart {
  display: flex;
  justify-content: space-around;
  gap: 10px;
}

.day-item {
  text-align: center;
  flex: 1;
}

.day-date {
  font-size: 12px;
  color: #666;
  margin-bottom: 5px;
}

.day-bar {
  height: 80px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.bar-fill {
  width: 20px;
  min-height: 4px;
  border-radius: 4px;
  transition: height 0.3s;
}

.day-calorie {
  font-size: 10px;
  color: #999;
  margin-top: 5px;
}
</style>