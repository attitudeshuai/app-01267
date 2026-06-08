<template>
  <div class="dashboard-page">
    <div class="page-header"><h2>数据概览</h2></div>
    <div v-loading="loading" class="dashboard-content">
      <div class="stats-grid">
        <div class="stat-card revenue">
          <div class="stat-icon"><el-icon :size="32"><Money /></el-icon></div>
          <div class="stat-info">
            <p class="stat-value">¥{{ dashboard.totalRevenue || '0.00' }}</p>
            <p class="stat-label">总收入</p>
          </div>
        </div>
        <div class="stat-card orders">
          <div class="stat-icon"><el-icon :size="32"><List /></el-icon></div>
          <div class="stat-info">
            <p class="stat-value">{{ dashboard.totalOrders || 0 }}</p>
            <p class="stat-label">总订单数</p>
          </div>
        </div>
        <div class="stat-card medicines">
          <div class="stat-icon"><el-icon :size="32"><FirstAidKit /></el-icon></div>
          <div class="stat-info">
            <p class="stat-value">{{ dashboard.totalMedicines || 0 }}</p>
            <p class="stat-label">药材品种</p>
          </div>
        </div>
        <div class="stat-card lowstock">
          <div class="stat-icon"><el-icon :size="32"><Warning /></el-icon></div>
          <div class="stat-info">
            <p class="stat-value">{{ dashboard.lowStockCount || 0 }}</p>
            <p class="stat-label">低库存预警</p>
          </div>
        </div>
      </div>

      <div class="detail-cards">
        <div class="detail-card">
          <h3>今日统计</h3>
          <div class="today-stats">
            <div class="today-item">
              <span class="today-value">{{ dashboard.todayOrders || 0 }}</span>
              <span class="today-label">今日订单</span>
            </div>
            <div class="today-item">
              <span class="today-value">¥{{ dashboard.todayRevenue || '0.00' }}</span>
              <span class="today-label">今日收入</span>
            </div>
            <div class="today-item">
              <span class="today-value">{{ dashboard.pendingShipment || 0 }}</span>
              <span class="today-label">待发货</span>
            </div>
          </div>
        </div>
        <div class="detail-card">
          <h3>快捷入口</h3>
          <div class="quick-links">
            <el-button type="primary" plain @click="$router.push('/merchant/medicines')">药材管理</el-button>
            <el-button type="primary" plain @click="$router.push('/merchant/orders')">订单管理</el-button>
            <el-button type="warning" plain @click="$router.push('/merchant/inventory')">库存预警</el-button>
          </div>
        </div>
        <div v-if="(dashboard.unreadNotificationCount || 0) > 0" class="detail-card notification-card">
          <h3>
            <el-icon color="#fa8c16"><Bell /></el-icon>
            系统通知 ({{ dashboard.unreadNotificationCount }} 条未读)
          </h3>
          <div class="notification-tip">
            <span>您有新的库存预警或价格异常通知，请前往</span>
            <el-button type="primary" link @click="$router.push('/merchant/inventory')">库存预警</el-button>
            <span>查看详情</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { getDashboard } from '../../api/merchant'

const dashboard = ref({})
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getDashboard()
    dashboard.value = res.data || {}
  } catch (_) { /* error handled */ }
  loading.value = false
})
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #333; }
.dashboard-content { min-height: 300px; }
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.stat-icon {
  width: 60px; height: 60px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
}
.revenue .stat-icon { background: #fff5f0; color: #FF7A45; }
.orders .stat-icon { background: #e6f7ff; color: #1890ff; }
.medicines .stat-icon { background: #f6ffed; color: #52C41A; }
.lowstock .stat-icon { background: #fff2e8; color: #fa8c16; }
.stat-value { font-size: 24px; font-weight: 700; color: #333; }
.stat-label { font-size: 13px; color: #8C8C8C; margin-top: 4px; }
.detail-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.detail-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.detail-card h3 { font-size: 16px; font-weight: 600; color: #333; margin-bottom: 20px; }
.today-stats { display: flex; justify-content: space-around; text-align: center; }
.today-value { display: block; font-size: 22px; font-weight: 700; color: #FF7A45; }
.today-label { font-size: 13px; color: #8C8C8C; margin-top: 4px; display: block; }
.quick-links { display: flex; gap: 12px; flex-wrap: wrap; }
.notification-card { border-left: 4px solid #fa8c16; }
.notification-tip {
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
