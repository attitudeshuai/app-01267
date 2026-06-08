<template>
  <div class="collections-page">
    <div class="page-header"><h2>我的收藏</h2></div>
    <div v-loading="loading" class="collections-content">
      <div v-if="list.length" class="medicine-grid">
        <div v-for="med in list" :key="med.id" class="medicine-card" @click="$router.push(`/medicines/${med.medicineId || med.id}`)">
          <div class="medicine-image">
            <img v-if="med.image" :src="med.image" :alt="med.medicineName || med.name" />
            <div v-else class="image-placeholder">
              <el-icon :size="36" color="#FF7A45"><FirstAidKit /></el-icon>
            </div>
            <el-button
              class="uncollect-btn"
              type="danger"
              circle
              size="small"
              @click.stop="handleUncollect(med)"
            >
              <el-icon><Star /></el-icon>
            </el-button>
          </div>
          <div class="medicine-info">
            <h4>{{ med.medicineName || med.name }}</h4>
            <div class="medicine-bottom">
              <span class="medicine-price">¥{{ med.price }}</span>
              <span class="medicine-sales">已售 {{ med.salesCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="暂无收藏">
        <el-button type="primary" @click="$router.push('/medicines')" round>去逛逛</el-button>
      </el-empty>
      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="current"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="fetchCollections"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCollections, toggleCollection } from '../../api/purchaser'
import { ElMessage } from 'element-plus'

const list = ref([])
const current = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

onMounted(() => fetchCollections())

async function fetchCollections() {
  loading.value = true
  try {
    const res = await getCollections({ current: current.value, size: size.value })
    const data = res.data || {}
    list.value = data.records || data || []
    total.value = data.total || 0
  } catch (_) { list.value = [] }
  loading.value = false
}

async function handleUncollect(med) {
  try {
    await toggleCollection(med.medicineId || med.id)
    ElMessage.success('已取消收藏')
    fetchCollections()
  } catch (_) { /* error handled */ }
}
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #333; }
.collections-content { min-height: 300px; }
.medicine-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.medicine-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #EEEEEE;
}
.medicine-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}
.medicine-image {
  position: relative;
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: #f9f9f9;
}
.medicine-image img { width: 100%; height: 100%; object-fit: cover; }
.image-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #fff5f0, #ffe7d6);
}
.uncollect-btn {
  position: absolute;
  top: 8px;
  right: 8px;
}
.medicine-info { padding: 12px; }
.medicine-info h4 {
  font-size: 14px; font-weight: 600; color: #333; margin-bottom: 8px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.medicine-bottom { display: flex; justify-content: space-between; align-items: center; }
.medicine-price { font-size: 16px; font-weight: 700; color: #FF7A45; }
.medicine-sales { font-size: 12px; color: #8C8C8C; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 24px; }
</style>
