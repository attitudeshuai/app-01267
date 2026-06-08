<template>
  <div class="page-container">
    <div class="page-header"><h2>价格分析</h2></div>
    <div class="card-wrapper">
      <!-- 价格异常波动预警阈值设置 -->
      <div class="price-warning-card">
        <div class="price-warning-header">
          <h4>价格异常波动预警阈值</h4>
          <el-tag type="info" size="small">当药材价格变动超过该比例时，系统自动向商户发送预警通知</el-tag>
        </div>
        <div class="price-warning-body">
          <template v-if="editingThreshold">
            <el-input-number v-model="thresholdValue" :min="0.01" :max="1" :step="0.05" :precision="2" size="default" />
            <span class="threshold-hint">（0.01=1%，0.3=30%）</span>
            <el-button type="primary" size="default" :loading="saveThresholdLoading" @click="saveThreshold">保存</el-button>
            <el-button @click="editingThreshold = false">取消</el-button>
          </template>
          <template v-else>
            <span class="threshold-display">{{ (priceAbnormalRate * 100).toFixed(0) }}%</span>
            <el-button type="primary" size="small" @click="startEditThreshold">修改</el-button>
          </template>
        </div>
      </div>

      <div class="filter-bar">
        <el-select
          v-model="selectedMedicineId"
          filterable
          placeholder="选择或搜索药材"
          style="width: 320px"
          @change="fetchHistory"
        >
          <el-option v-for="m in medicineOptions" :key="m.id" :label="`${m.name} - ¥${(m.price || 0).toFixed(2)}/${m.unit || '克'}`" :value="m.id" />
        </el-select>
        <el-button @click="loadMedicines" :loading="searchLoading">刷新列表</el-button>
      </div>

      <template v-if="selectedMedicineId && selectedMedicine">
        <div class="stats-cards">
          <div class="stat-card">
            <div class="stat-label">当前价格</div>
            <div class="stat-value primary">¥{{ (selectedMedicine.price || 0).toFixed(2) }}</div>
            <div class="stat-unit">每{{ selectedMedicine.unit || '克' }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">历史最高</div>
            <div class="stat-value danger">¥{{ maxPrice.toFixed(2) }}</div>
            <div class="stat-unit">{{ maxPriceDate }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">历史最低</div>
            <div class="stat-value success">¥{{ minPrice.toFixed(2) }}</div>
            <div class="stat-unit">{{ minPriceDate }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">价格变化</div>
            <div class="stat-value" :class="priceChange >= 0 ? 'danger' : 'success'">
              {{ priceChange >= 0 ? '+' : '' }}{{ priceChange.toFixed(2) }}%
            </div>
            <div class="stat-unit">相比首次记录</div>
          </div>
        </div>

        <div v-if="historyData.length" class="price-chart">
          <h4 style="margin-bottom: 16px; color: #333;">价格走势图</h4>
          <div class="chart-container">
            <div class="y-axis">
              <span>¥{{ maxPrice.toFixed(2) }}</span>
              <span>¥{{ ((maxPrice + minPrice) / 2).toFixed(2) }}</span>
              <span>¥{{ minPrice.toFixed(2) }}</span>
            </div>
            <div class="chart-area">
              <div class="chart-grid">
                <div class="grid-line"></div>
                <div class="grid-line"></div>
                <div class="grid-line"></div>
              </div>
              <div class="bar-chart">
                <div class="bar-item" v-for="(item, idx) in historyData" :key="idx">
                  <div class="bar-tooltip">¥{{ (item.price || 0).toFixed(2) }}</div>
                  <div class="bar-fill" :style="{ height: barHeight(item.price) + '%' }"></div>
                  <div class="bar-label">{{ formatDate(item.recordTime) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <h4 style="margin: 24px 0 12px; color: #333;">价格变更记录</h4>
        <el-table :data="historyData" v-loading="historyLoading" stripe>
          <el-table-column prop="price" label="价格" width="140">
            <template #default="{ row }">
              <span style="font-weight: 600; color: #FF7A45;">¥{{ (row.price || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="recordTime" label="记录时间" min-width="180" />
          <el-table-column prop="isAbnormal" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.isAbnormal ? 'danger' : 'success'" size="small">
                {{ row.isAbnormal ? '异常' : '正常' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!historyLoading && historyData.length === 0" description="暂无价格记录" />
      </template>
      <div v-else class="empty-state">
        <el-icon :size="64" color="#ddd"><TrendCharts /></el-icon>
        <p>请选择药材查看价格分析</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'
import { getMedicines, getPriceHistory, getConfigs, updateConfig } from '../api/admin'
import { ElMessage } from 'element-plus'

const selectedMedicineId = ref(null)
const priceAbnormalRate = ref(0.3)
const editingThreshold = ref(false)
const thresholdValue = ref(0.3)
const saveThresholdLoading = ref(false)
const medicineOptions = ref([])
const searchLoading = ref(false)
const historyLoading = ref(false)
const historyData = ref([])

const selectedMedicine = computed(() => {
  return medicineOptions.value.find(m => m.id === selectedMedicineId.value)
})

const maxPrice = computed(() => {
  if (!historyData.value.length) return 0
  return Math.max(...historyData.value.map(i => i.price || 0))
})

const minPrice = computed(() => {
  if (!historyData.value.length) return 0
  return Math.min(...historyData.value.map(i => i.price || 0))
})

const maxPriceDate = computed(() => {
  if (!historyData.value.length) return '-'
  const item = historyData.value.find(i => i.price === maxPrice.value)
  return item ? formatDate(item.recordTime) : '-'
})

const minPriceDate = computed(() => {
  if (!historyData.value.length) return '-'
  const item = historyData.value.find(i => i.price === minPrice.value)
  return item ? formatDate(item.recordTime) : '-'
})

const priceChange = computed(() => {
  if (historyData.value.length < 2) return 0
  const first = historyData.value[0].price || 1
  const last = historyData.value[historyData.value.length - 1].price || 0
  return ((last - first) / first) * 100
})

async function loadPriceAbnormalRate() {
  try {
    const res = await getConfigs()
    const configs = res.data || []
    const cfg = configs.find(c => c.configKey === 'price_abnormal_rate')
    if (cfg && cfg.configValue) {
      const v = parseFloat(cfg.configValue)
      if (!isNaN(v)) priceAbnormalRate.value = v
    }
  } catch { /* use default 0.3 */ }
}

function startEditThreshold() {
  thresholdValue.value = priceAbnormalRate.value
  editingThreshold.value = true
}

async function saveThreshold() {
  const cfg = (await getConfigs()).data?.find(c => c.configKey === 'price_abnormal_rate')
  if (!cfg) {
    ElMessage.warning('未找到 price_abnormal_rate 配置项，请先在系统配置中添加')
    return
  }
  saveThresholdLoading.value = true
  try {
    await updateConfig({ ...cfg, configValue: String(thresholdValue.value) })
    priceAbnormalRate.value = thresholdValue.value
    editingThreshold.value = false
    ElMessage.success('保存成功')
  } finally {
    saveThresholdLoading.value = false
  }
}

async function loadMedicines() {
  searchLoading.value = true
  try {
    const res = await getMedicines({ current: 1, size: 100, status: 1 })
    medicineOptions.value = res.data?.records || []
  } finally {
    searchLoading.value = false
  }
}

async function fetchHistory() {
  if (!selectedMedicineId.value) return
  historyLoading.value = true
  try {
    const res = await getPriceHistory(selectedMedicineId.value)
    historyData.value = res.data || []
  } finally {
    historyLoading.value = false
  }
}

function barHeight(price) {
  if (!historyData.value.length || maxPrice.value === minPrice.value) return 50
  const range = maxPrice.value - minPrice.value
  if (range === 0) return 100
  return ((price - minPrice.value) / range) * 80 + 20
}

function formatDate(dt) {
  if (!dt) return ''
  if (dt.length >= 10) return dt.substring(0, 10)
  return dt
}

onMounted(() => {
  loadPriceAbnormalRate()
  loadMedicines()
})
</script>

<style scoped>
.price-warning-card {
  background: linear-gradient(135deg, #fff9f6, #fff5f0);
  border: 1px solid #ffe4d9;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}
.price-warning-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.price-warning-header h4 {
  margin: 0;
  font-size: 16px;
  color: #333;
}
.price-warning-body {
  display: flex;
  align-items: center;
  gap: 12px;
}
.threshold-display {
  font-size: 24px;
  font-weight: 700;
  color: #FF7A45;
}
.threshold-hint {
  font-size: 12px;
  color: #8C8C8C;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin: 20px 0;
}

.stat-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #8C8C8C;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.stat-value.primary { color: #FF7A45; }
.stat-value.success { color: #52C41A; }
.stat-value.danger { color: #F5222D; }

.stat-unit {
  font-size: 12px;
  color: #bbb;
  margin-top: 4px;
}

.price-chart {
  margin-top: 24px;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.chart-container {
  display: flex;
  gap: 12px;
}

.y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  font-size: 11px;
  color: #8C8C8C;
  width: 60px;
  text-align: right;
  padding: 0 8px;
}

.chart-area {
  flex: 1;
  position: relative;
  height: 200px;
}

.chart-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.grid-line {
  border-top: 1px dashed #e5e5e5;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: calc(100% - 24px);
  padding: 0 8px;
  position: relative;
  z-index: 1;
}

.bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
  min-width: 32px;
  max-width: 60px;
  position: relative;
}

.bar-tooltip {
  position: absolute;
  top: -24px;
  font-size: 11px;
  color: #FF7A45;
  font-weight: 600;
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.2s;
}

.bar-item:hover .bar-tooltip {
  opacity: 1;
}

.bar-fill {
  width: 100%;
  max-width: 40px;
  background: linear-gradient(180deg, #FF7A45, #ff9a76);
  border-radius: 4px 4px 0 0;
  transition: height 0.4s ease, transform 0.2s;
  min-height: 4px;
  cursor: pointer;
}

.bar-item:hover .bar-fill {
  transform: scaleX(1.1);
}

.bar-label {
  font-size: 10px;
  color: #8C8C8C;
  margin-top: 6px;
  white-space: nowrap;
  height: 18px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #bbb;
}

.empty-state p {
  margin-top: 16px;
  font-size: 14px;
}
</style>
