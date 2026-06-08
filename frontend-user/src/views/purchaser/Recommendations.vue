<template>
  <div class="recommend-page">
    <div class="page-header"><h2>为你推荐</h2></div>
    <div v-loading="loading" class="recommend-content">
      <div v-if="list.length" class="medicine-grid">
        <div v-for="med in list" :key="med.id" class="medicine-card" @click="goDetail(med.id)">
          <div class="medicine-image">
            <img v-if="med.image" :src="med.image" :alt="med.name" />
            <div v-else class="image-placeholder">
              <el-icon :size="36" color="#FF7A45"><FirstAidKit /></el-icon>
            </div>
          </div>
          <div class="medicine-info">
            <h4>{{ med.name }}</h4>
            <p class="medicine-spec">{{ med.specification || med.origin || '优质药材' }}</p>
            <div class="medicine-bottom">
              <span class="medicine-price">¥{{ med.price }}</span>
              <span class="medicine-sales">已售 {{ med.salesCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="暂无推荐" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRecommendMedicines, recordRecommendClick } from '../../api/purchaser'

const router = useRouter()
const list = ref([])
const loading = ref(false)

function goDetail(id) {
  recordRecommendClick(id).catch(() => {})
  router.push(`/medicines/${id}`)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getRecommendMedicines()
    list.value = res.data || []
  } catch (_) { /* ignore */ }
  loading.value = false
})
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #333; }
.recommend-content { min-height: 300px; }
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
  width: 100%; height: 180px; overflow: hidden; background: #f9f9f9;
}
.medicine-image img { width: 100%; height: 100%; object-fit: cover; }
.image-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #fff5f0, #ffe7d6);
}
.medicine-info { padding: 12px; }
.medicine-info h4 {
  font-size: 14px; font-weight: 600; color: #333; margin-bottom: 4px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.medicine-spec { font-size: 12px; color: #8C8C8C; margin-bottom: 8px; }
.medicine-bottom { display: flex; justify-content: space-between; align-items: center; }
.medicine-price { font-size: 16px; font-weight: 700; color: #FF7A45; }
.medicine-sales { font-size: 12px; color: #8C8C8C; }
</style>
