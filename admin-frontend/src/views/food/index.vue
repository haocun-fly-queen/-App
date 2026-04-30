<template>
  <div class="food-container">
    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="标准食物库" name="food" />
      <el-tab-pane label="AI识别校准" name="calibrate" />
    </el-tabs>

    <!-- ==================== 标准食物库内容 ==================== -->
    <div v-show="activeTab === 'food'">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
            v-model="keyword"
            placeholder="搜索食物名称"
            style="width: 200px"
            clearable
            @clear="loadData"
            @keyup.enter="loadData"
        />
        <el-select v-model="category" placeholder="选择分类" style="width: 150px" clearable @change="loadData">
          <el-option v-for="cat in categoryList" :key="cat" :label="cat" :value="cat" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
        <el-button type="success" @click="openAddDialog">+ 新增食物</el-button>
      </div>

      <!-- 食物表格 -->
      <el-table :data="foodList" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="食物名称" width="150" />
        <el-table-column prop="alias" label="别名" width="150" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="caloriePer100g" label="热量(kcal/100g)" width="130" />
        <el-table-column prop="carbsPer100g" label="碳水(g/100g)" width="120" />
        <el-table-column prop="proteinPer100g" label="蛋白质(g/100g)" width="120" />
        <el-table-column prop="fatPer100g" label="脂肪(g/100g)" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button
                v-if="row.status === 1"
                type="warning"
                size="small"
                @click="handleStatus(row.id, 0)"
            >停用</el-button>
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
            @current-change="loadData"
            @size-change="loadData"
        />
      </div>
    </div>

    <!-- ==================== AI识别校准内容 ==================== -->
    <div v-show="activeTab === 'calibrate'">
      <div class="search-bar">
        <el-input
            v-model="aiKeyword"
            placeholder="搜索识别结果"
            style="width: 250px"
            clearable
            @clear="loadAiLogs"
            @keyup.enter="loadAiLogs"
        />
        <el-button type="primary" @click="loadAiLogs">搜索</el-button>
      </div>

      <el-table :data="aiLogList" border stripe v-loading="aiLoading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column label="识别结果" min-width="250">
          <template #default="{ row }">
            <div v-if="row.rawResult">
              <div v-for="(food, idx) in parseRawResult(row.rawResult)" :key="idx" class="food-tag">
                {{ food.name }} ({{ food.weight }}g)
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="当前匹配" width="180">
          <template #default="{ row }">
            <el-tag
                v-for="id in ((row.matchedFoodIds || '').split(',').filter(id => id))"
                :key="id"
                size="small"
                style="margin: 2px"
            >
              {{ getFoodNameById(id) || id }}
            </el-tag>
            <span v-if="!row.matchedFoodIds">未匹配</span>
          </template>
        </el-table-column>
        <el-table-column prop="isCorrected" label="已校准" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isCorrected === 1 ? 'success' : 'info'">
              {{ row.isCorrected === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="识别时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openCalibrateDialog(row)">校准</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
            v-model:current-page="aiPage"
            v-model:page-size="aiSize"
            :total="aiTotal"
            :page-sizes="[10, 15, 30, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadAiLogs"
            @size-change="loadAiLogs"
        />
      </div>
    </div>

    <!-- 新增/编辑食物弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="食物名称" required>
          <el-input v-model="form.name" placeholder="请输入食物名称" />
        </el-form-item>
        <el-form-item label="别名">
          <el-input v-model="form.alias" placeholder="多个别名用逗号分隔" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categoryList" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="热量(kcal/100g)" required>
          <el-input-number v-model="form.caloriePer100g" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="碳水(g/100g)">
          <el-input-number v-model="form.carbsPer100g" :min="0" :step="0.1" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="蛋白质(g/100g)">
          <el-input-number v-model="form.proteinPer100g" :min="0" :step="0.1" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="脂肪(g/100g)">
          <el-input-number v-model="form.fatPer100g" :min="0" :step="0.1" :precision="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFood" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 校准弹窗 -->
    <el-dialog v-model="calibrateDialogVisible" title="校准AI识别匹配" width="650px">
      <div v-if="currentAiLog">
        <div class="calibrate-section">
          <div v-for="(food, idx) in parseRawResult(currentAiLog.rawResult)" :key="idx" class="calibrate-food-item">
            <div class="food-info">
              <span class="food-name">{{ food.name }}</span>
              <span class="food-weight">{{ food.weight }}g</span>
            </div>
            <el-select
                v-model="calibrateSelections[idx]"
                placeholder="选择匹配食物"
                filterable
                clearable
                style="width: 250px"
            >
              <el-option
                  v-for="item in foodListAll"
                  :key="item.id"
                  :label="`${item.name} (${item.category})`"
                  :value="item.id"
              />
            </el-select>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="calibrateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCalibration" :loading="calibrateLoading">保存校准</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFoodList, getCategories, addFood, updateFood, deleteFood, updateFoodStatus, getAiLogs, calibrateAiLog } from '@/api/food'

// ==================== 标准食物库变量 ====================
const loading = ref(false)
const submitLoading = ref(false)
const foodList = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(15)
const keyword = ref('')
const category = ref('')
const categoryList = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增食物')
const isEdit = ref(false)
const editId = ref(null)
const form = ref({
  name: '',
  alias: '',
  category: '',
  caloriePer100g: 0,
  carbsPer100g: 0,
  proteinPer100g: 0,
  fatPer100g: 0
})

// ==================== AI校准变量 ====================
const activeTab = ref('food')
const aiLoading = ref(false)
const aiLogList = ref([])
const aiTotal = ref(0)
const aiPage = ref(1)
const aiSize = ref(15)
const aiKeyword = ref('')
const calibrateDialogVisible = ref(false)
const currentAiLog = ref(null)
const calibrateSelections = ref([])
const calibrateLoading = ref(false)
const foodListAll = ref([])

// ==================== 标准食物库方法 ====================
const loadData = async () => {
  loading.value = true
  try {
    const res = await getFoodList({
      page: page.value,
      size: size.value,
      keyword: keyword.value,
      category: category.value
    })
    if (res.code === 200) {
      foodList.value = res.data.list
      total.value = res.data.total
    }
  } catch (err) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategories()
    if (res.code === 200) {
      categoryList.value = res.data
    }
  } catch (err) {
    console.error('加载分类失败', err)
  }
}

const openAddDialog = () => {
  isEdit.value = false
  editId.value = null
  dialogTitle.value = '新增食物'
  form.value = {
    name: '',
    alias: '',
    category: '',
    caloriePer100g: 0,
    carbsPer100g: 0,
    proteinPer100g: 0,
    fatPer100g: 0
  }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑食物'
  form.value = {
    name: row.name,
    alias: row.alias || '',
    category: row.category,
    caloriePer100g: row.caloriePer100g,
    carbsPer100g: row.carbsPer100g || 0,
    proteinPer100g: row.proteinPer100g || 0,
    fatPer100g: row.fatPer100g || 0
  }
  dialogVisible.value = true
}

const saveFood = async () => {
  if (!form.value.name) {
    ElMessage.warning('请输入食物名称')
    return
  }
  if (!form.value.category) {
    ElMessage.warning('请选择分类')
    return
  }

  submitLoading.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateFood(editId.value, form.value)
    } else {
      res = await addFood(form.value)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
      dialogVisible.value = false
      loadData()
    }
  } catch (err) {
    ElMessage.error('保存失败')
  } finally {
    submitLoading.value = false
  }
}

const handleStatus = async (id, status) => {
  const action = status === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定要${action}该食物吗？`, '提示', { type: 'warning' })
    const res = await updateFoodStatus(id, status)
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

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该食物吗？', '提示', { type: 'warning' })
    const res = await deleteFood(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// ==================== AI校准方法 ====================
const parseRawResult = (rawResult) => {
  if (!rawResult) return []
  try {
    const data = typeof rawResult === 'string' ? JSON.parse(rawResult) : rawResult
    return data.foods || []
  } catch (e) {
    return []
  }
}

const getFoodNameById = (id) => {
  const food = foodListAll.value.find(f => f.id === parseInt(id))
  return food ? food.name : null
}

const loadAllFoods = async () => {
  try {
    const res = await getFoodList({ page: 1, size: 999, status: 1 })
    if (res.code === 200) {
      foodListAll.value = res.data.list || []
    }
  } catch (err) {
    console.error('加载食物列表失败', err)
  }
}

const loadAiLogs = async () => {
  aiLoading.value = true
  try {
    const res = await getAiLogs({
      page: aiPage.value,
      size: aiSize.value,
      keyword: aiKeyword.value
    })
    if (res.code === 200) {
      aiLogList.value = res.data.list
      aiTotal.value = res.data.total
    }
  } catch (err) {
    ElMessage.error('加载AI日志失败')
  } finally {
    aiLoading.value = false
  }
}

const openCalibrateDialog = (log) => {
  currentAiLog.value = log
  const foods = parseRawResult(log.rawResult)
  const originalIds = (log.matchedFoodIds || '').split(',').filter(id => id)
  calibrateSelections.value = foods.map((_, idx) => {
    return originalIds[idx] ? parseInt(originalIds[idx]) : null
  })
  calibrateDialogVisible.value = true
}

const saveCalibration = async () => {
  const foodIds = calibrateSelections.value.filter(id => id !== null && id !== '').join(',')
  calibrateLoading.value = true
  try {
    const res = await calibrateAiLog(currentAiLog.value.id, foodIds)
    if (res.code === 200) {
      ElMessage.success('校准成功')
      calibrateDialogVisible.value = false
      loadAiLogs()
    }
  } catch (err) {
    ElMessage.error('校准失败')
  } finally {
    calibrateLoading.value = false
  }
}

const handleTabClick = () => {
  if (activeTab.value === 'calibrate') {
    loadAiLogs()
    loadAllFoods()
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadData()
  loadCategories()
  loadAllFoods()
  loadAiLogs()
})
</script>

<style scoped>
.food-container {
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
.food-tag {
  display: inline-block;
  background: #f5f7fa;
  padding: 2px 8px;
  margin: 2px;
  border-radius: 4px;
  font-size: 12px;
}
.calibrate-section {
  margin-bottom: 20px;
}
.calibrate-food-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}
.food-info {
  display: flex;
  gap: 15px;
}
.food-name {
  font-weight: bold;
  font-size: 14px;
}
.food-weight {
  color: #666;
  font-size: 12px;
}
</style>