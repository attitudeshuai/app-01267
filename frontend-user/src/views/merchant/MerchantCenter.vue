<template>
  <div class="merchant-center">
    <div class="center-container">
      <aside class="sidebar">
        <div class="merchant-brief">
          <el-avatar :size="56" :style="{ backgroundColor: '#FF7A45', fontSize: '22px' }">
            {{ (userStore.userInfo?.companyName || 'M').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="brief-info">
            <p class="brief-name">{{ userStore.userInfo?.companyName || userStore.userInfo?.username }}</p>
            <p class="brief-role">商家中心</p>
          </div>
        </div>
        <el-menu :default-active="activeMenu" router class="sidebar-menu">
          <el-menu-item index="/merchant/dashboard">
            <el-icon><DataAnalysis /></el-icon><span>数据概览</span>
          </el-menu-item>
          <el-menu-item index="/merchant/medicines">
            <el-icon><FirstAidKit /></el-icon><span>药材管理</span>
          </el-menu-item>
          <el-menu-item index="/merchant/orders">
            <el-icon><List /></el-icon><span>订单管理</span>
          </el-menu-item>
          <el-menu-item index="/merchant/inventory">
            <el-icon><Box /></el-icon><span>库存预警</span>
          </el-menu-item>
          <el-menu-item index="/merchant/profile">
            <el-icon><Setting /></el-icon><span>商家信息</span>
          </el-menu-item>
        </el-menu>
      </aside>
      <main class="center-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../../store/user'

const route = useRoute()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.merchant-center { padding: 24px 0; }
.center-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  gap: 24px;
}
.sidebar { width: 240px; flex-shrink: 0; }
.merchant-brief {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.brief-name { font-size: 16px; font-weight: 600; color: #333; }
.brief-role { font-size: 12px; color: #8C8C8C; margin-top: 4px; }
.sidebar-menu {
  border-radius: 12px;
  border-right: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}
.sidebar-menu .el-menu-item.is-active {
  color: #FF7A45;
  background: #fff5f0;
}
.center-main { flex: 1; min-width: 0; }
</style>
