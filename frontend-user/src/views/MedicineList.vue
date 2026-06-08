<template>
  <div class="medicine-list-page">
    <div class="list-container">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="keyword"
            placeholder="搜索药材名称"
            clearable
            style="width: 260px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="categoryId" placeholder="全部分类" clearable @change="handleSearch" style="width: 160px">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </div>
        <span class="result-count">共 {{ total }} 种药材</span>
      </div>

      <div v-loading="loading" class="medicine-grid">
        <div
          v-for="med in medicines"
          :key="med.id"
          class="medicine-card"
          @click="$router.push(`/medicines/${med.id}`)"
        >
          <div class="medicine-image">
            <img v-if="med.image" :src="med.image" :alt="med.name" />
            <div v-else class="image-placeholder">
              <el-icon :size="40" color="#FF7A45"><FirstAidKit /></el-icon>
            </div>
          </div>
          <div class="medicine-info">
            <h4 class="medicine-name">{{ med.name }}</h4>
            <p class="medicine-spec">{{ med.specification || med.origin || '优质药材' }}</p>
            <div class="medicine-bottom">
              <span class="medicine-price">¥{{ med.price }}</span>
              <span class="medicine-sales">已售 {{ med.salesCount || 0 }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && medicines.length === 0" description="暂无相关药材" />
      </div>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[12, 24, 36]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="fetchMedicines"
          @size-change="fetchMedicines"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getMedicines, getCategories } from '../api/common'

const route = useRoute()
const medicines = ref([])
const categories = ref([])
const keyword = ref('')
const categoryId = ref('')
const current = ref(1)
const size = ref(12)
const total = ref(0)
const loading = ref(false)

onMounted(() => {
  keyword.value = route.query.keyword || ''
  categoryId.value = route.query.categoryId ? Number(route.query.categoryId) : ''
  fetchCategories()
  fetchMedicines()
})

watch(() => route.query, (q) => {
  if (q.keyword !== undefined) keyword.value = q.keyword
  if (q.categoryId !== undefined) categoryId.value = q.categoryId ? Number(q.categoryId) : ''
  current.value = 1
  fetchMedicines()
})

async function fetchCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (_) { /* ignore */ }
}

async function fetchMedicines() {
  loading.value = true
  try {
    const res = await getMedicines({
      current: current.value,
      size: size.value,
      keyword: keyword.value,
      categoryId: categoryId.value || undefined
    })
    const data = res.data || {}
    medicines.value = data.records || data || []
    total.value = data.total || 0
  } catch (_) {
    medicines.value = []
  }
  loading.value = false
}

function handleSearch() {
  current.value = 1
  fetchMedicines()
}
</script>

<style scoped>
.medicine-list-page {
  padding: 24px 0;
}
.list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 12px;
  padding: 16px 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.filter-left {
  display: flex;
  gap: 12px;
}
.result-count {
  font-size: 14px;
  color: #8C8C8C;
}
.medicine-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 300px;
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
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: #f9f9f9;
}
.medicine-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.medicine-card:hover .medicine-image img {
  transform: scale(1.05);
}
.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff5f0, #ffe7d6);
}
.medicine-info {
  padding: 14px;
}
.medicine-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.medicine-spec {
  font-size: 12px;
  color: #8C8C8C;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.medicine-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.medicine-price {
  font-size: 18px;
  font-weight: 700;
  color: #FF7A45;
}
.medicine-sales {
  font-size: 12px;
  color: #8C8C8C;
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
