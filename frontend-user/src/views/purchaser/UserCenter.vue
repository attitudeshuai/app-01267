<template>
  <div class="user-center">
    <div class="center-container">
      <aside class="sidebar">
        <div class="user-brief">
          <el-avatar :size="56" :style="{ backgroundColor: '#FF7A45', fontSize: '22px' }">
            {{ (userStore.userInfo?.nickname || 'U').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="user-brief-info">
            <p class="user-brief-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</p>
            <p class="user-brief-role">采购商</p>
          </div>
        </div>
        <el-menu :default-active="activeMenu" router class="sidebar-menu">
          <el-menu-item index="/purchaser/orders">
            <el-icon><List /></el-icon><span>我的订单</span>
          </el-menu-item>
          <el-menu-item index="/purchaser/cart">
            <el-icon><ShoppingCart /></el-icon><span>购物车</span>
          </el-menu-item>
          <el-menu-item index="/purchaser/collections">
            <el-icon><Star /></el-icon><span>我的收藏</span>
          </el-menu-item>
          <el-menu-item index="/purchaser/recommendations">
            <el-icon><MagicStick /></el-icon><span>为你推荐</span>
          </el-menu-item>
          <el-menu-item index="/purchaser/addresses">
            <el-icon><Location /></el-icon><span>收货地址</span>
          </el-menu-item>
          <el-menu-item index="/purchaser/profile">
            <el-icon><User /></el-icon><span>个人信息</span>
          </el-menu-item>
          <el-menu-item index="/purchaser/feedback">
            <el-icon><ChatDotRound /></el-icon><span>意见反馈</span>
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
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/purchaser/orders')) return '/purchaser/orders'
  return path
})
</script>

<style scoped>
.user-center {
  padding: 24px 0;
}
.center-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  gap: 24px;
}
.sidebar {
  width: 240px;
  flex-shrink: 0;
}
.user-brief {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.user-brief-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.user-brief-role {
  font-size: 12px;
  color: #8C8C8C;
  margin-top: 4px;
}
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
.center-main {
  flex: 1;
  min-width: 0;
}
</style>
