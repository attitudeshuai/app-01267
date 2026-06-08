<template>
  <div class="home">
    <section class="banner-section" v-loading="loadingBanners">
      <el-carousel height="380px" :interval="5000" arrow="hover">
        <el-carousel-item v-for="banner in banners" :key="banner.id">
          <div
            class="banner-item"
            :class="{ 'banner-clickable': banner.linkUrl }"
            :style="{ backgroundColor: banner.color || '#FF7A45' }"
            @click="banner.linkUrl && handleBannerClick(banner.linkUrl)"
          >
            <img v-if="banner.image" :src="banner.image" :alt="banner.title" class="banner-img" />
            <div v-else class="banner-placeholder">
              <el-icon :size="64" color="#fff"><FirstAidKit /></el-icon>
              <h2>{{ banner.title || '中药材商城' }}</h2>
              <p>{{ banner.description || '传承经典 · 道地药材' }}</p>
            </div>
          </div>
        </el-carousel-item>
        <el-carousel-item v-if="banners.length === 0">
          <div class="banner-item" style="background: linear-gradient(135deg, #FF7A45, #ff9566);">
            <div class="banner-placeholder">
              <el-icon :size="64" color="#fff"><FirstAidKit /></el-icon>
              <h2>中药材商城</h2>
              <p>传承经典 · 道地药材 · 品质保障</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <div class="home-container">
      <section v-if="announcements.length" class="announcement-bar">
        <div class="announcement-header">
          <el-icon color="#FF7A45"><Bell /></el-icon>
          <span class="announcement-title">系统公告</span>
        </div>
        <div class="announcement-list">
          <div
            v-for="(a, i) in announcements"
            :key="i"
            class="announcement-item"
            :title="a.title || a.content"
            @click="showAnnouncement(a)"
          >
            <span class="announcement-dot"></span>
            <span class="announcement-text">{{ a.title || a.content }}</span>
            <el-icon class="announcement-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </section>

      <el-dialog v-model="announcementVisible" :title="currentAnnouncement?.title || '公告详情'" width="560px" destroy-on-close>
        <div class="announcement-detail" v-if="currentAnnouncement">
          <p class="announcement-content">{{ currentAnnouncement.content || currentAnnouncement.title }}</p>
        </div>
      </el-dialog>

      <section class="category-section">
        <h3 class="section-title">药材分类</h3>
        <div class="category-grid">
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="category-card"
            @click="goCategory(cat.id)"
          >
            <div class="category-icon">
              <el-icon :size="28"><Grid /></el-icon>
            </div>
            <span>{{ cat.name }}</span>
          </div>
        </div>
      </section>

      <section class="hot-section">
        <h3 class="section-title">热门药材</h3>
        <div v-loading="loadingHot" class="medicine-grid">
          <div
            v-for="med in hotMedicines"
            :key="med.id"
            class="medicine-card"
            @click="goDetail(med.id)"
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
        </div>
        <div class="view-more">
          <el-button type="primary" plain round @click="$router.push('/medicines')">查看全部药材</el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBanners, getCategories, getAnnouncements, getHotMedicines } from '../api/common'

const router = useRouter()
const banners = ref([])
const categories = ref([])
const announcements = ref([])
const hotMedicines = ref([])
const loadingBanners = ref(false)
const loadingHot = ref(false)
const announcementVisible = ref(false)
const currentAnnouncement = ref(null)

onMounted(() => {
  fetchBanners()
  fetchCategories()
  fetchAnnouncements()
  fetchHotMedicines()
})

async function fetchBanners() {
  loadingBanners.value = true
  try {
    const res = await getBanners()
    banners.value = res.data || []
  } catch (_) { /* ignore */ }
  loadingBanners.value = false
}

async function fetchCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (_) { /* ignore */ }
}

async function fetchAnnouncements() {
  try {
    const res = await getAnnouncements()
    announcements.value = res.data || []
  } catch (_) { /* ignore */ }
}

async function fetchHotMedicines() {
  loadingHot.value = true
  try {
    const res = await getHotMedicines()
    hotMedicines.value = res.data || []
  } catch (_) { /* ignore */ }
  loadingHot.value = false
}

function goCategory(id) {
  router.push({ path: '/medicines', query: { categoryId: id } })
}

function goDetail(id) {
  router.push(`/medicines/${id}`)
}

function showAnnouncement(a) {
  currentAnnouncement.value = a
  announcementVisible.value = true
}

function handleBannerClick(linkUrl) {
  if (!linkUrl) return
  const url = linkUrl.trim()
  if (url.startsWith('http://') || url.startsWith('https://')) {
    window.open(url, '_blank')
  } else {
    router.push(url.startsWith('/') ? url : '/' + url)
  }
}
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}
.banner-section {
  margin-bottom: 24px;
}
.banner-item {
  height: 380px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.banner-item.banner-clickable {
  cursor: pointer;
}
.banner-item.banner-clickable:hover {
  opacity: 0.98;
}
.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.banner-placeholder {
  text-align: center;
  color: #fff;
}
.banner-placeholder h2 {
  font-size: 36px;
  margin: 16px 0 8px;
}
.banner-placeholder p {
  font-size: 18px;
  opacity: 0.9;
}
.announcement-bar {
  background: #fff5f0;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 32px;
  border: 1px solid #ffe7d9;
}
.announcement-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}
.announcement-title {
  flex-shrink: 0;
}
.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.announcement-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}
.announcement-item:hover {
  background: #fff;
  border-color: #FF7A45;
  box-shadow: 0 2px 8px rgba(255, 122, 69, 0.1);
}
.announcement-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #FF7A45;
  flex-shrink: 0;
}
.announcement-text {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}
.announcement-item:hover .announcement-text {
  color: #FF7A45;
}
.announcement-arrow {
  flex-shrink: 0;
  font-size: 14px;
  color: #8C8C8C;
}
.announcement-item:hover .announcement-arrow {
  color: #FF7A45;
}
.announcement-detail {
  padding: 8px 0;
}
.announcement-content {
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}
.section-title {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-left: 12px;
  border-left: 4px solid #FF7A45;
}
.category-section {
  margin-bottom: 40px;
}
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 16px;
}
.category-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #EEEEEE;
}
.category-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(255, 122, 69, 0.12);
  border-color: #FF7A45;
}
.category-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #fff5f0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 8px;
  color: #FF7A45;
}
.category-card span {
  font-size: 13px;
  color: #333;
}
.hot-section {
  margin-bottom: 40px;
}
.medicine-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 200px;
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
.view-more {
  text-align: center;
  margin-top: 24px;
}
</style>
