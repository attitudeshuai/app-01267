<template>
  <div class="merchant-orders-page">
    <div class="page-header"><h2>订单管理</h2></div>
    <div class="filter-bar">
      <el-input v-model="orderNo" placeholder="搜索订单号" clearable style="width:220px" @keyup.enter="handleSearch" @clear="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="orderStatus" placeholder="全部状态" clearable @change="handleSearch" style="width:140px">
        <el-option label="待付款" :value="0" />
        <el-option label="已付款" :value="1" />
        <el-option label="已发货" :value="2" />
        <el-option label="已收货" :value="3" />
        <el-option label="已完成" :value="4" />
        <el-option label="已取消" :value="5" />
      </el-select>
    </div>

    <el-table :data="orders" v-loading="loading" stripe style="width:100%">
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column label="买家" width="120">
        <template #default="{ row }">{{ row.purchaserName || row.username || '-' }}</template>
      </el-table-column>
      <el-table-column label="金额" width="100">
        <template #default="{ row }"><span style="color:#FF7A45;font-weight:600">¥{{ row.totalAmount }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.orderStatus)" size="small">{{ statusText(row.orderStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdTime" label="下单时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          <el-button v-if="row.orderStatus === 1" text type="success" size="small" @click="openShipDialog(row)">发货</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="total > 0" class="pagination-wrap">
      <el-pagination v-model:current-page="current" :page-size="size" :total="total" layout="total, prev, pager, next" background @current-change="fetchOrders" />
    </div>

    <el-dialog v-model="shipDialogVisible" title="确认发货" width="400px">
      <el-form label-width="80px">
        <el-form-item label="快递单号">
          <el-input v-model="trackingNo" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipping" @click="handleShip">确认发货</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <div v-if="detailOrder" v-loading="detailLoading">
        <div class="detail-info-grid">
          <p><span class="label">订单号：</span>{{ detailOrder.orderNo }}</p>
          <p><span class="label">下单时间：</span>{{ formatTime(detailOrder.createdTime) }}</p>
          <p><span class="label">状态：</span>{{ statusText(detailOrder.orderStatus) }}</p>
          <p><span class="label">买家：</span>{{ detailOrder.purchaserName || '-' }}</p>
          <p><span class="label">金额：</span><span style="color:#FF7A45">¥{{ detailOrder.totalAmount }}</span></p>
          <p><span class="label">收货人：</span>{{ detailOrder.receiverName || '-' }}</p>
          <p><span class="label">电话：</span>{{ detailOrder.receiverPhone || '-' }}</p>
          <p v-if="detailOrder.receiverAddress || detailOrder.address"><span class="label">地址：</span>{{ detailOrder.receiverAddress || detailOrder.address }}</p>
          <p v-if="detailOrder.trackingNo"><span class="label">快递单号：</span>{{ detailOrder.trackingNo }}</p>
          <p v-if="detailOrder.remark"><span class="label">备注：</span>{{ detailOrder.remark }}</p>
        </div>
        <el-divider />
        <h4 style="margin-bottom:12px">商品列表</h4>
        <div v-for="item in (detailOrder.details || detailOrder.items || detailOrder.orderDetails || [])" :key="item.id" style="display:flex;align-items:center;gap:12px;padding:8px 0;border-bottom:1px solid #f5f5f5">
          <span style="flex:1">{{ item.medicineName || item.name }}</span>
          <span style="color:#8C8C8C">x{{ item.quantity }}</span>
          <span style="color:#FF7A45;width:80px;text-align:right">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrders, getOrderDetail, shipOrder } from '../../api/merchant'
import { ElMessage } from 'element-plus'

const orders = ref([])
const orderNo = ref('')
const orderStatus = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const shipDialogVisible = ref(false)
const shipping = ref(false)
const trackingNo = ref('')
const currentOrder = ref(null)
const detailVisible = ref(false)
const detailOrder = ref(null)
const detailLoading = ref(false)

onMounted(() => fetchOrders())

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getOrders({ current: current.value, size: size.value, orderStatus: (orderStatus.value === '' || orderStatus.value == null) ? undefined : orderStatus.value, orderNo: orderNo.value || undefined })
    const data = res.data || {}
    orders.value = data.records || data || []
    total.value = data.total || 0
  } catch (_) { orders.value = [] }
  loading.value = false
}

function handleSearch() { current.value = 1; fetchOrders() }

function formatTime(t) {
  if (!t) return '-'
  const d = typeof t === 'string' ? new Date(t) : Array.isArray(t) ? new Date(t[0], t[1] - 1, t[2], t[3] || 0, t[4] || 0, t[5] || 0) : t
  if (isNaN(d.getTime())) return t
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
function statusText(s) {
  const m = { 0: '待付款', 1: '待发货', 2: '已发货', 3: '已收货', 4: '已完成', 5: '已取消' }
  return m[s] ?? s
}
function statusType(s) {
  const m = { 0: 'warning', 1: 'primary', 2: '', 3: 'info', 4: 'success', 5: 'info' }
  return m[s] ?? ''
}

function openShipDialog(row) {
  currentOrder.value = row
  trackingNo.value = ''
  shipDialogVisible.value = true
}

async function handleShip() {
  if (!trackingNo.value.trim()) {
    ElMessage.warning('请输入快递单号')
    return
  }
  shipping.value = true
  try {
    await shipOrder(currentOrder.value.id, trackingNo.value.trim())
    ElMessage.success('发货成功')
    shipDialogVisible.value = false
    fetchOrders()
  } catch (_) { /* error handled */ }
  shipping.value = false
}

async function viewDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getOrderDetail(row.id)
    detailOrder.value = res.data
  } catch (_) { /* error handled */ }
  detailLoading.value = false
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #333; }
.filter-bar {
  display: flex; gap: 12px; margin-bottom: 16px;
  background: #fff; border-radius: 8px; padding: 12px 16px;
}
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
.detail-info-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 10px;
}
.detail-info-grid .label { color: #8C8C8C; }
</style>
