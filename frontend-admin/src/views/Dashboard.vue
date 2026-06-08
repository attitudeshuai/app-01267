<template>
  <div class="page-container">
    <div class="page-header"><h2>首页概览</h2></div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="item in statCards" :key="item.label">
        <div class="stat-card" :style="{ borderTop: `3px solid ${item.color}` }">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <div class="card-wrapper">
          <h3 style="margin-bottom: 16px; font-size: 16px;">热销药材</h3>
          <el-table :data="dashboard.hotMedicines || []" stripe size="small" max-height="360">
            <el-table-column prop="medicineName" label="药材名称" />
            <el-table-column prop="salesCount" label="销量" width="100" />
          </el-table>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="card-wrapper">
          <h3 style="margin-bottom: 16px; font-size: 16px;">近期订单趋势</h3>
          <div class="trend-list" v-if="dashboard.orderTrends && dashboard.orderTrends.length">
            <div class="trend-item" v-for="item in dashboard.orderTrends" :key="item.date">
              <span class="trend-date">{{ item.date }}</span>
              <div class="trend-bar-wrap">
                <div class="trend-bar" :style="{ width: trendBarWidth(item.count) + '%' }"></div>
              </div>
              <span class="trend-count">{{ item.count }}单</span>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="80" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getDashboard } from '../api/admin'

const dashboard = reactive({
  totalOrders: 0,
  totalRevenue: 0,
  totalMedicines: 0,
  totalUsers: 0,
  pendingMerchants: 0,
  pendingMedicines: 0,
  lowStockCount: 0,
  hotMedicines: [],
  orderTrends: []
})

const statCards = computed(() => [
  { label: '总订单数', value: dashboard.totalOrders, color: '#FF7A45' },
  { label: '总营收(元)', value: `¥${(dashboard.totalRevenue || 0).toFixed(2)}`, color: '#52C41A' },
  { label: '药材总数', value: dashboard.totalMedicines, color: '#1890FF' },
  { label: '用户总数', value: dashboard.totalUsers, color: '#722ED1' },
  { label: '待审核商家', value: dashboard.pendingMerchants, color: '#FA8C16' },
  { label: '待审核药材', value: dashboard.pendingMedicines, color: '#EB2F96' },
  { label: '库存预警', value: dashboard.lowStockCount, color: '#F5222D' }
])

function trendBarWidth(count) {
  if (!dashboard.orderTrends || !dashboard.orderTrends.length) return 0
  const max = Math.max(...dashboard.orderTrends.map(i => i.count), 1)
  return (count / max) * 100
}

onMounted(async () => {
  try {
    const res = await getDashboard()
    Object.assign(dashboard, res.data || {})
  } catch {
    // handled by interceptor
  }
})
</script>

<style scoped>
.stat-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  border: 1px solid #EEEEEE;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #8C8C8C;
  margin-top: 4px;
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trend-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.trend-date {
  width: 90px;
  font-size: 13px;
  color: #8C8C8C;
  flex-shrink: 0;
}

.trend-bar-wrap {
  flex: 1;
  height: 18px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.trend-bar {
  height: 100%;
  background: linear-gradient(90deg, #FF7A45, #ff9a76);
  border-radius: 4px;
  transition: width 0.4s ease;
}

.trend-count {
  width: 50px;
  text-align: right;
  font-size: 13px;
  color: #333;
  flex-shrink: 0;
}
</style>
