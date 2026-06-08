<template>
  <div class="orders-page">
    <div class="page-header">
      <h2>我的订单</h2>
    </div>
    <el-tabs v-model="activeStatus" @tab-change="handleTabChange" class="order-tabs">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待付款" :name="0" />
      <el-tab-pane label="待发货" :name="1" />
      <el-tab-pane label="已发货" :name="2" />
      <el-tab-pane label="已完成" :name="4" />
      <el-tab-pane label="已取消" :name="5" />
    </el-tabs>
    <div v-loading="loading" class="orders-content">
      <template v-if="orders.length">
        <div v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="order-time">{{ formatTime(order.createdTime) }}</span>
            <el-tag :type="statusType(order.orderStatus)" size="small">{{ statusText(order.orderStatus) }}</el-tag>
          </div>
          <div class="order-items" @click="$router.push(`/purchaser/orders/${order.id}`)">
            <div v-for="item in (order.details || order.items || order.orderDetails || []).slice(0, 5)" :key="item.id" class="order-item-brief">
              <div class="oib-image">
                <img v-if="item.medicineImage || item.image" :src="item.medicineImage || item.image" />
                <div v-else class="oib-placeholder">
                  <el-icon :size="20" color="#FF7A45"><FirstAidKit /></el-icon>
                </div>
              </div>
              <span class="oib-name">{{ item.medicineName || item.name }}</span>
              <span class="oib-qty">x{{ item.quantity }}</span>
            </div>
          </div>
          <div class="order-footer">
            <span class="order-total">合计：<em>¥{{ order.totalAmount }}</em></span>
            <div class="order-actions">
              <el-button v-if="order.orderStatus === 0" type="primary" size="small" @click="handlePay(order)" round>立即支付</el-button>
              <el-button v-if="order.orderStatus === 2" type="primary" size="small" @click="handleReceive(order)" round>确认收货</el-button>
              <el-button v-if="order.orderStatus === 0" size="small" @click="handleCancel(order)" round>取消订单</el-button>
              <el-button size="small" @click="$router.push(`/purchaser/orders/${order.id}`)" round>查看详情</el-button>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="current"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="fetchOrders"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrders, payOrder, receiveOrder, cancelOrder } from '../../api/purchaser'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref([])
const activeStatus = ref('all')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

onMounted(() => fetchOrders())

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getOrders({
      current: current.value,
      size: size.value,
      orderStatus: (activeStatus.value === 'all' || activeStatus.value === '' || activeStatus.value == null) ? undefined : activeStatus.value
    })
    const data = res.data || {}
    orders.value = data.records || data || []
    total.value = data.total || 0
  } catch (_) { orders.value = [] }
  loading.value = false
}

function handleTabChange() {
  current.value = 1
  fetchOrders()
}

function formatTime(t) {
  if (!t) return '-'
  const d = typeof t === 'string' ? new Date(t) : Array.isArray(t) ? new Date(t[0], t[1] - 1, t[2], t[3] || 0, t[4] || 0, t[5] || 0) : t
  if (isNaN(d.getTime())) return t
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function statusText(s) {
  const map = { 0: '待付款', 1: '待发货', 2: '已发货', 3: '已收货', 4: '已完成', 5: '已取消' }
  return map[s] ?? s
}

function statusType(s) {
  const map = { 0: 'warning', 1: 'primary', 2: '', 3: 'info', 4: 'success', 5: 'info' }
  return map[s] ?? ''
}

async function handlePay(order) {
  try {
    await ElMessageBox.confirm('确认支付该订单？', '支付确认', { type: 'info' })
    await payOrder(order.id)
    ElMessage.success('支付成功')
    fetchOrders()
  } catch (_) { /* cancelled */ }
}

async function handleReceive(order) {
  try {
    await ElMessageBox.confirm('确认已收到货物？', '收货确认', { type: 'info' })
    await receiveOrder(order.id)
    ElMessage.success('已确认收货')
    fetchOrders()
  } catch (_) { /* cancelled */ }
}

async function handleCancel(order) {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '取消订单', { type: 'warning' })
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (_) { /* cancelled */ }
}
</script>

<style scoped>
.page-header {
  margin-bottom: 12px;
}
.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}
.order-tabs {
  margin-bottom: 16px;
}
.orders-content {
  min-height: 300px;
}
.order-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.order-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f5f5;
}
.order-no {
  font-size: 13px;
  color: #8C8C8C;
}
.order-time {
  font-size: 13px;
  color: #8C8C8C;
  margin-left: auto;
}
.order-items {
  cursor: pointer;
  padding: 8px 0;
}
.order-item-brief {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
}
.oib-image {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}
.oib-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.oib-placeholder {
  width: 100%;
  height: 100%;
  background: #fff5f0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.oib-name {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.oib-qty {
  font-size: 13px;
  color: #8C8C8C;
}
.order-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;
}
.order-total {
  font-size: 14px;
  color: #666;
}
.order-total em {
  font-style: normal;
  font-size: 18px;
  font-weight: 700;
  color: #FF7A45;
}
.order-actions {
  display: flex;
  gap: 8px;
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
