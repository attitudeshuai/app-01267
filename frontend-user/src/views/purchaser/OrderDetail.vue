<template>
  <div class="order-detail-page">
    <div class="page-header">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      <h2>订单详情</h2>
    </div>
    <div v-loading="loading" class="detail-content">
      <template v-if="order">
        <div class="status-bar" :class="statusClass">
          <div class="status-info">
            <h3 class="status-text">{{ statusText(order.orderStatus) }}</h3>
            <p v-if="order.orderStatus === 0">请尽快完成支付</p>
            <p v-if="order.orderStatus === 2">您的包裹正在路上</p>
          </div>
          <div class="status-actions">
            <el-button v-if="order.orderStatus === 0" type="primary" @click="handlePay" round>立即支付</el-button>
            <el-button v-if="order.orderStatus === 2" type="primary" @click="handleReceive" round>确认收货</el-button>
            <el-button v-if="order.orderStatus === 0" @click="handleCancel" round>取消订单</el-button>
          </div>
        </div>

        <div class="info-card">
          <h3 class="card-title">订单信息</h3>
          <div class="info-grid">
            <div class="info-row"><span class="label">订单号</span><span>{{ order.orderNo }}</span></div>
            <div class="info-row"><span class="label">下单时间</span><span>{{ formatTime(order.createdTime) }}</span></div>
            <div class="info-row"><span class="label">收货人</span><span>{{ order.receiverName || '-' }}</span></div>
            <div class="info-row"><span class="label">联系电话</span><span>{{ order.receiverPhone || '-' }}</span></div>
            <div class="info-row"><span class="label">收货地址</span><span>{{ order.receiverAddress || order.address || '-' }}</span></div>
            <div class="info-row" v-if="order.trackingNo"><span class="label">快递单号</span><span>{{ order.trackingNo }}</span></div>
            <div class="info-row" v-if="order.remark"><span class="label">备注</span><span>{{ order.remark }}</span></div>
          </div>
        </div>

        <div class="info-card">
          <h3 class="card-title">商品列表</h3>
          <div class="items-list" v-if="(order.details || order.items || order.orderDetails || []).length">
            <div v-for="(item, idx) in (order.details || order.items || order.orderDetails || [])" :key="item.id || idx" class="detail-item">
              <div class="di-image">
                <img v-if="item.medicineImage || item.image" :src="item.medicineImage || item.image" />
                <div v-else class="di-placeholder">
                  <el-icon :size="24" color="#FF7A45"><FirstAidKit /></el-icon>
                </div>
              </div>
              <div class="di-info">
                <span class="di-name">{{ item.medicineName || item.name }}</span>
                <span class="di-spec">{{ item.specification || '' }}</span>
              </div>
              <span class="di-price">¥{{ item.price }}</span>
              <span class="di-qty">x{{ item.quantity }}</span>
              <span class="di-subtotal">¥{{ (Number(item.subtotal) || (item.price * item.quantity) || 0).toFixed(2) }}</span>
              <el-button
                v-if="(order.orderStatus === 4 || order.orderStatus === 3) && !item.review"
                size="small"
                type="primary"
                plain
                @click="openReview(item)"
                round
              >评价</el-button>
            </div>
          </div>
          <div v-else class="items-empty">
            <el-empty description="暂无商品明细" :image-size="60" />
          </div>
          <div class="total-bar">
            <span>订单总额：</span>
            <span class="total-amount">¥{{ order.totalAmount }}</span>
          </div>
        </div>
      </template>
    </div>

    <el-dialog v-model="reviewVisible" title="评价商品" width="460px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" :colors="['#FF7A45', '#FF7A45', '#FF7A45']" />
        </el-form-item>
        <el-form-item label="评价">
          <el-input v-model="reviewForm.review" type="textarea" :rows="3" placeholder="请输入评价内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, payOrder, receiveOrder, cancelOrder, reviewOrder } from '../../api/purchaser'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(false)
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewForm = reactive({ orderDetailId: null, rating: 5, review: '' })

const statusClass = computed(() => {
  const map = { 0: 'warning', 1: 'primary', 2: 'info', 3: 'success', 4: 'success', 5: 'grey' }
  return map[order.value?.orderStatus] ?? ''
})

function formatTime(t) {
  if (!t) return '-'
  const d = typeof t === 'string' ? new Date(t) : Array.isArray(t) ? new Date(t[0], t[1] - 1, t[2], t[3] || 0, t[4] || 0, t[5] || 0) : t
  if (isNaN(d.getTime())) return t
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => fetchDetail())

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.id)
    order.value = res.data
  } catch (_) { /* error handled */ }
  loading.value = false
}

function statusText(s) {
  const map = { 0: '待付款', 1: '待发货', 2: '已发货', 3: '已收货', 4: '已完成', 5: '已取消' }
  return map[s] ?? s
}

async function handlePay() {
  try {
    await ElMessageBox.confirm('确认支付该订单？', '支付确认', { type: 'info' })
    await payOrder(order.value.id)
    ElMessage.success('支付成功')
    fetchDetail()
  } catch (_) { /* cancelled */ }
}

async function handleReceive() {
  try {
    await ElMessageBox.confirm('确认已收到货物？', '收货确认', { type: 'info' })
    await receiveOrder(order.value.id)
    ElMessage.success('已确认收货')
    fetchDetail()
  } catch (_) { /* cancelled */ }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '取消订单', { type: 'warning' })
    await cancelOrder(order.value.id)
    ElMessage.success('订单已取消')
    fetchDetail()
  } catch (_) { /* cancelled */ }
}

function openReview(item) {
  reviewForm.orderDetailId = item.id
  reviewForm.rating = 5
  reviewForm.review = ''
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewForm.review.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }
  reviewLoading.value = true
  try {
    await reviewOrder(reviewForm)
    ElMessage.success('评价成功')
    reviewVisible.value = false
    fetchDetail()
  } catch (_) { /* error handled */ }
  reviewLoading.value = false
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 300px;
}
.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-radius: 12px;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}
.status-bar.warning { background: linear-gradient(135deg, #FF7A45, #ff9566); }
.status-bar.primary { background: linear-gradient(135deg, #409eff, #66b1ff); }
.status-bar.info { background: linear-gradient(135deg, #909399, #b1b3b8); }
.status-bar.success { background: linear-gradient(135deg, #389e0d, #52c41a); }
.status-bar.grey { background: linear-gradient(135deg, #bfbfbf, #d9d9d9); }
.status-info h3.status-text {
  font-size: 20px;
  margin-bottom: 4px;
  color: #fff;
  font-weight: 600;
}
.status-info p { font-size: 14px; opacity: 0.95; color: #fff; }
.status-actions :deep(.el-button) { color: #fff; border-color: #fff; }
.status-actions :deep(.el-button:hover) { background: rgba(255,255,255,0.2); color: #fff; border-color: #fff; }
.status-actions { display: flex; gap: 8px; }
.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-left: 10px;
  border-left: 3px solid #FF7A45;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.info-row {
  display: flex;
  gap: 12px;
  font-size: 14px;
}
.info-row .label {
  color: #8C8C8C;
  min-width: 70px;
}
.detail-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.di-image {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}
.di-image img { width: 100%; height: 100%; object-fit: cover; }
.di-placeholder { width: 100%; height: 100%; background: #fff5f0; display: flex; align-items: center; justify-content: center; }
.di-info { flex: 1; }
.di-name { display: block; font-size: 14px; color: #333; }
.di-spec { font-size: 12px; color: #8C8C8C; }
.di-price { width: 70px; color: #333; }
.di-qty { width: 50px; color: #8C8C8C; }
.di-subtotal { width: 80px; text-align: right; color: #FF7A45; font-weight: 600; }
.items-empty { padding: 24px 0; }
.total-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  padding-top: 16px;
  font-size: 14px;
  color: #666;
}
.total-amount { font-size: 22px; font-weight: 700; color: #FF7A45; }
</style>
