<template>
  <div class="dashboard-container">
    <!-- 标题栏 + 刷新按钮 -->
    <div class="header-bar">
      <h2 class="page-title">数据统计看板</h2>
      <div class="refresh-area">
        <span class="last-update">最后更新: {{ lastUpdateTime }}</span>
        <el-button
            :icon="Refresh"
            :loading="refreshing"
            @click="manualRefresh"
            size="small"
            type="primary"
        >刷新</el-button>
      </div>
    </div>

    <!-- 统计卡片 - 加载状态 -->
    <div class="stats-grid" v-if="loading">
      <div v-for="i in 6" :key="i" class="stat-card skeleton-card">
        <div class="skeleton-icon"></div>
        <div class="stat-info">
          <div class="skeleton-value"></div>
          <div class="skeleton-label"></div>
        </div>
      </div>
    </div>

    <!-- 统计卡片 - 正常显示 -->
    <div class="stats-grid" v-else>
      <div class="stat-card fade-in">
        <div class="stat-icon">👥</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
          <div class="stat-label">累计用户</div>
        </div>
      </div>

      <div class="stat-card fade-in" style="animation-delay: 0.05s">
        <div class="stat-icon">📈</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayNewUsers || 0 }}</div>
          <div class="stat-label">今日新增</div>
          <div class="stat-sub">本周新增: {{ stats.weekNewUsers || 0 }}</div>
        </div>
      </div>

      <div class="stat-card fade-in" style="animation-delay: 0.1s">
        <div class="stat-icon">🌟</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.dailyActiveUsers || 0 }}</div>
          <div class="stat-label">今日活跃</div>
        </div>
      </div>

      <div class="stat-card fade-in" style="animation-delay: 0.15s">
        <div class="stat-icon">🍽️</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayDietCount || 0 }}</div>
          <div class="stat-label">今日饮食记录</div>
        </div>
      </div>

      <div class="stat-card fade-in" style="animation-delay: 0.2s">
        <div class="stat-icon">🤖</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayAiCalls || 0 }}</div>
          <div class="stat-label">今日AI识别</div>
        </div>
      </div>

      <div class="stat-card fade-in" style="animation-delay: 0.25s">
        <div class="stat-icon">📊</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.monthNewUsers || 0 }}</div>
          <div class="stat-label">本月新增</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <!-- 用户目标分布 -->
      <div class="chart-card fade-in" style="animation-delay: 0.3s">
        <h3>用户目标分布</h3>
        <div v-if="loading" class="goal-chart">
          <div v-for="i in 3" :key="i" class="goal-item">
            <div class="skeleton-name"></div>
            <div class="goal-bar-container skeleton-bar"></div>
            <div class="skeleton-count"></div>
          </div>
        </div>
        <div v-else class="goal-chart">
          <div class="goal-item">
            <span class="goal-name">减脂</span>
            <div class="goal-bar-container">
              <div class="goal-bar" :style="{ width: getGoalPercent('减脂') + '%' }"></div>
            </div>
            <span class="goal-count">{{ stats.goalDistribution?.减脂 || 0 }}</span>
          </div>
          <div class="goal-item">
            <span class="goal-name">增肌</span>
            <div class="goal-bar-container">
              <div class="goal-bar" :style="{ width: getGoalPercent('增肌') + '%' }"></div>
            </div>
            <span class="goal-count">{{ stats.goalDistribution?.增肌 || 0 }}</span>
          </div>
          <div class="goal-item">
            <span class="goal-name">保持体重</span>
            <div class="goal-bar-container">
              <div class="goal-bar" :style="{ width: getGoalPercent('保持体重') + '%' }"></div>
            </div>
            <span class="goal-count">{{ stats.goalDistribution?.保持体重 || 0 }}</span>
          </div>
        </div>
        <div class="total-info" v-if="!loading">总人数: {{ goalTotal }} 人</div>
      </div>

      <!-- 近7天新增趋势 -->
      <div class="chart-card fade-in" style="animation-delay: 0.35s">
        <h3>近7天新增用户趋势</h3>
        <div v-if="loading" class="trend-chart">
          <div v-for="i in 7" :key="i" class="trend-bar-item">
            <div class="skeleton-trend-bar"></div>
            <div class="skeleton-trend-label"></div>
          </div>
        </div>
        <div v-else class="trend-chart">
          <div v-for="(count, date) in stats.last7DaysNewUsers" :key="date" class="trend-bar-item">
            <div class="trend-bar" :style="{ height: getTrendHeight(count, 'user') + 'px' }"></div>
            <div class="trend-label">{{ date }}</div>
            <div class="trend-value">{{ count }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="charts-row">
      <!-- 近7天AI调用趋势 -->
      <div class="chart-card full-width fade-in" style="animation-delay: 0.4s">
        <h3>近7天AI识别调用趋势</h3>
        <div v-if="loading" class="trend-chart">
          <div v-for="i in 7" :key="i" class="trend-bar-item">
            <div class="skeleton-trend-bar"></div>
            <div class="skeleton-trend-label"></div>
          </div>
        </div>
        <div v-else class="trend-chart">
          <div v-for="(count, date) in stats.last7DaysAiCalls" :key="date" class="trend-bar-item">
            <div class="trend-bar ai-bar" :style="{ height: getTrendHeight(count, 'ai') + 'px' }"></div>
            <div class="trend-label">{{ date }}</div>
            <div class="trend-value">{{ count }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getDashboard } from '@/api/statistics'

const loading = ref(true)
const refreshing = ref(false)
const lastUpdateTime = ref('')
let timer = null

const stats = ref({
  totalUsers: 0,
  todayNewUsers: 0,
  weekNewUsers: 0,
  monthNewUsers: 0,
  dailyActiveUsers: 0,
  todayDietCount: 0,
  todayAiCalls: 0,
  goalDistribution: {},
  last7DaysNewUsers: {},
  last7DaysAiCalls: {}
})

// 计算总人数
const goalTotal = computed(() => {
  const distribution = stats.value.goalDistribution || {}
  return (distribution.减脂 || 0) + (distribution.增肌 || 0) + (distribution.保持体重 || 0)
})

// 获取用户目标百分比
const getGoalPercent = (type) => {
  const distribution = stats.value.goalDistribution || {}
  const count = distribution[type] || 0
  const total = goalTotal.value
  if (total === 0) return 0
  return (count / total) * 100
}

// 获取趋势图高度
const getTrendHeight = (count, type) => {
  const max = type === 'user'
      ? Math.max(...Object.values(stats.value.last7DaysNewUsers || {}), 10)
      : Math.max(...Object.values(stats.value.last7DaysAiCalls || {}), 10)
  if (max === 0) return 20
  return Math.max(20, (count / max) * 100)
}

// 格式化最后更新时间
const formatLastUpdateTime = () => {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
}

// 加载数据
const loadDashboard = async (isManual = false) => {
  if (isManual) {
    refreshing.value = true
  } else {
    loading.value = true
  }

  try {
    const res = await getDashboard()
    if (res.code === 200) {
      stats.value = res.data
      lastUpdateTime.value = formatLastUpdateTime()
      if (isManual) {
        ElMessage.success('数据已刷新')
      }
    } else {
      ElMessage.error('加载数据失败')
    }
  } catch (err) {
    console.error('加载数据看板失败', err)
    ElMessage.error('加载数据看板失败')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

// 手动刷新
const manualRefresh = () => {
  loadDashboard(true)
}

// 启动自动轮询
const startAutoRefresh = () => {
  if (timer) clearInterval(timer)
  // 每 30 秒自动刷新一次
  timer = setInterval(() => {
    // 只在页面可见时刷新
    if (!document.hidden) {
      loadDashboard(false)
    }
  }, 30000)
}

// 停止自动轮询
const stopAutoRefresh = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

// 页面可见性变化时处理
const handleVisibilityChange = () => {
  if (!document.hidden) {
    // 页面重新可见时立即刷新一次
    loadDashboard(false)
  }
}

onMounted(() => {
  loadDashboard()
  startAutoRefresh()
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  stopAutoRefresh()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 头部栏样式 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 15px;
}

.page-title {
  margin: 0;
  color: #303133;
}

.refresh-area {
  display: flex;
  align-items: center;
  gap: 15px;
}

.last-update {
  font-size: 13px;
  color: #909399;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

/* 卡片样式 */
.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 淡入动画 */
.fade-in {
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
  transform: translateY(20px);
}

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 骨架屏动画 */
@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

.skeleton-card {
  background: linear-gradient(135deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-icon {
  width: 48px;
  height: 48px;
  background: #d0d0d0;
  border-radius: 12px;
}

.skeleton-value {
  width: 80px;
  height: 32px;
  background: #d0d0d0;
  border-radius: 8px;
  margin-bottom: 8px;
}

.skeleton-label {
  width: 60px;
  height: 14px;
  background: #d0d0d0;
  border-radius: 4px;
}

.skeleton-name {
  width: 80px;
  height: 16px;
  background: #d0d0d0;
  border-radius: 4px;
}

.skeleton-bar {
  background: #d0d0d0 !important;
}

.skeleton-count {
  width: 40px;
  height: 16px;
  background: #d0d0d0;
  border-radius: 4px;
}

.skeleton-trend-bar {
  width: 40px;
  height: 80px;
  background: #d0d0d0;
  border-radius: 4px;
}

.skeleton-trend-label {
  width: 30px;
  height: 12px;
  background: #d0d0d0;
  border-radius: 4px;
  margin-top: 8px;
}

/* 图标样式 */
.stat-icon {
  font-size: 48px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-sub {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

/* 图表区域 */
.charts-row {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
  flex-wrap: wrap;
}

.chart-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  flex: 1;
  min-width: 280px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.chart-card.full-width {
  width: 100%;
}

.chart-card h3 {
  margin-bottom: 20px;
  font-size: 16px;
  color: #303133;
}

/* 目标分布样式 */
.goal-chart {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.goal-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.goal-name {
  width: 80px;
  font-size: 14px;
  color: #606266;
}

.goal-bar-container {
  flex: 1;
  height: 24px;
  background-color: #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
}

.goal-bar {
  height: 100%;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 12px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.goal-count {
  width: 50px;
  text-align: right;
  font-size: 14px;
  color: #606266;
}

.total-info {
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px solid #e4e7ed;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

/* 趋势图样式 */
.trend-chart {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  gap: 10px;
  height: 180px;
}

.trend-bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.trend-bar {
  width: 40px;
  background: linear-gradient(180deg, #67c23a, #85ce61);
  border-radius: 4px 4px 0 0;
  transition: height 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  min-height: 20px;
}

.trend-bar.ai-bar {
  background: linear-gradient(180deg, #667eea, #764ba2);
}

.trend-label {
  font-size: 12px;
  color: #909399;
}

.trend-value {
  font-size: 12px;
  color: #606266;
  font-weight: bold;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .trend-bar {
    width: 25px;
  }

  .skeleton-trend-bar {
    width: 25px;
  }
}
</style>