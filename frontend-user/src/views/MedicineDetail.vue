<template>
  <div class="detail-page">
    <div class="detail-container" v-loading="loading">
      <div v-if="medicine" class="detail-content">
        <div class="detail-top">
          <div class="detail-image">
            <img v-if="medicine.image" :src="medicine.image" :alt="medicine.name" />
            <div v-else class="image-placeholder">
              <el-icon :size="80" color="#FF7A45"><FirstAidKit /></el-icon>
            </div>
            <div v-if="medicine.images && medicine.images.length" class="image-thumbs">
              <img v-for="(img, i) in medicine.images" :key="i" :src="img" class="thumb" @click="medicine.image = img" />
            </div>
          </div>
          <div class="detail-info">
            <h1 class="medicine-name">{{ medicine.name }}</h1>
            <p class="medicine-desc-short">{{ medicine.specification || '' }}</p>
            <div class="price-row">
              <span class="price-label">价格</span>
              <span class="price-value">¥{{ medicine.price }}</span>
              <span class="unit">/ {{ medicine.unit || '份' }}</span>
            </div>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">产地</span>
                <span class="info-value">{{ medicine.origin || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">品质等级</span>
                <span class="info-value">{{ medicine.qualityGrade || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">规格</span>
                <span class="info-value">{{ medicine.specification || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">库存</span>
                <span class="info-value">{{ medicine.stockQuantity ?? '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">销量</span>
                <span class="info-value">{{ medicine.salesCount || 0 }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">商家</span>
                <span class="info-value">{{ medicine.merchantName || medicine.companyName || '-' }}</span>
              </div>
            </div>
            <div class="action-row">
              <el-input-number v-model="quantity" :min="1" :max="Math.min(999, medicine.stockQuantity || 999)" size="large" />
              <el-button type="primary" size="large" :loading="adding" @click="handleAddCart" round>
                <el-icon><ShoppingCart /></el-icon> 加入购物车
              </el-button>
              <el-button size="large" @click="handleToggleCollect" round :type="collected ? 'warning' : 'default'">
                <el-icon><Star /></el-icon> {{ collected ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="药材详情" name="desc">
            <div class="tab-content desc-content">
              <p v-if="medicine.description">{{ medicine.description }}</p>
              <el-empty v-else description="暂无详情" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="溯源信息" name="trace">
            <div class="tab-content">
              <p v-if="medicine.traceInfo">{{ medicine.traceInfo }}</p>
              <el-empty v-else description="暂无溯源信息" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="用户评价" name="reviews">
            <div class="tab-content">
              <div v-if="reviews.length" class="review-list">
                <div v-for="r in reviews" :key="r.id" class="review-item">
                  <div class="review-header">
                    <el-avatar :size="32" :style="{ backgroundColor: '#FF7A45' }">{{ (r.username || 'U').charAt(0) }}</el-avatar>
                    <span class="review-user">{{ r.username || '匿名用户' }}</span>
                    <el-rate v-model="r.rating" disabled :colors="['#FF7A45', '#FF7A45', '#FF7A45']" />
                    <span class="review-time">{{ r.createTime }}</span>
                  </div>
                  <p class="review-content">{{ r.review || r.content }}</p>
                </div>
              </div>
              <el-empty v-else description="暂无评价" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { getMedicineDetail } from '../api/common'
import { addToCart, toggleCollection } from '../api/purchaser'
import { ElMessage } from 'element-plus'

const route = useRoute()
const userStore = useUserStore()
const medicine = ref(null)
const reviews = ref([])
const quantity = ref(1)
const collected = ref(false)
const adding = ref(false)
const loading = ref(false)
const activeTab = ref('desc')

onMounted(() => {
  fetchDetail()
})

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getMedicineDetail(route.params.id)
    medicine.value = res.data || {}
    reviews.value = res.data?.reviews || []
    collected.value = res.data?.collected || false
  } catch (_) { /* error handled */ }
  loading.value = false
}

async function handleAddCart() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!userStore.isPurchaser) {
    ElMessage.warning('仅采购商可购买')
    return
  }
  adding.value = true
  try {
    await addToCart(medicine.value.id, quantity.value)
    ElMessage.success('已加入购物车')
    userStore.fetchCartCount()
  } catch (_) { /* error handled */ }
  adding.value = false
}

async function handleToggleCollect() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!userStore.isPurchaser) {
    ElMessage.warning('仅采购商可收藏')
    return
  }
  try {
    await toggleCollection(medicine.value.id)
    collected.value = !collected.value
    ElMessage.success(collected.value ? '已收藏' : '已取消收藏')
  } catch (_) { /* error handled */ }
}
</script>

<style scoped>
.detail-page {
  padding: 24px 0;
}
.detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  min-height: 400px;
}
.detail-top {
  display: flex;
  gap: 40px;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.detail-image {
  width: 420px;
  flex-shrink: 0;
}
.detail-image > img {
  width: 420px;
  height: 420px;
  object-fit: cover;
  border-radius: 12px;
  background: #f9f9f9;
}
.image-placeholder {
  width: 420px;
  height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff5f0, #ffe7d6);
  border-radius: 12px;
}
.image-thumbs {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.thumb {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.3s;
}
.thumb:hover {
  border-color: #FF7A45;
}
.detail-info {
  flex: 1;
}
.medicine-name {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 8px;
}
.medicine-desc-short {
  color: #8C8C8C;
  font-size: 14px;
  margin-bottom: 20px;
}
.price-row {
  background: #fff8f5;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 24px;
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.price-label {
  font-size: 14px;
  color: #8C8C8C;
}
.price-value {
  font-size: 32px;
  font-weight: 700;
  color: #FF7A45;
}
.unit {
  font-size: 14px;
  color: #8C8C8C;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.info-label {
  font-size: 12px;
  color: #8C8C8C;
}
.info-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.action-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.detail-tabs {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.tab-content {
  min-height: 200px;
  padding: 16px 0;
}
.desc-content p {
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
}
.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.review-item {
  border-bottom: 1px solid #EEEEEE;
  padding-bottom: 16px;
}
.review-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.review-user {
  font-weight: 500;
  color: #333;
}
.review-time {
  font-size: 12px;
  color: #8C8C8C;
  margin-left: auto;
}
.review-content {
  color: #666;
  line-height: 1.6;
  padding-left: 44px;
}
</style>
