<template>
  <div class="layout">
    <header class="top-nav">
      <div class="nav-container">
        <div class="nav-left">
          <router-link to="/" class="logo">
            <el-icon :size="28" color="#FF7A45"><FirstAidKit /></el-icon>
            <span class="logo-text">中药材商城</span>
          </router-link>
          <nav class="nav-links">
            <router-link to="/" class="nav-link" active-class="active" :class="{ active: $route.path === '/' }">首页</router-link>
            <router-link to="/medicines" class="nav-link" active-class="active">药材中心</router-link>
          </nav>
        </div>
        <div class="nav-center">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索药材..."
            class="search-input"
            @keyup.enter="handleSearch"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="nav-right">
          <template v-if="!userStore.isLoggedIn">
            <router-link to="/login">
              <el-button type="primary" round>登录</el-button>
            </router-link>
          </template>
          <template v-else>
            <router-link v-if="userStore.isPurchaser" to="/purchaser/cart" class="cart-link">
              <el-badge :value="userStore.cartCount" :hidden="userStore.cartCount === 0" :max="99">
                <el-icon :size="22"><ShoppingCart /></el-icon>
              </el-badge>
            </router-link>
            <el-dropdown @command="handleCommand" trigger="click">
              <span class="user-avatar">
                <el-avatar :size="32" :style="{ backgroundColor: '#FF7A45' }">
                  {{ (userStore.userInfo?.nickname || userStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}
                </el-avatar>
                <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.companyName || userStore.userInfo?.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <template v-if="userStore.isPurchaser">
                    <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                    <el-dropdown-item command="collections">我的收藏</el-dropdown-item>
                    <el-dropdown-item command="addresses">收货地址</el-dropdown-item>
                    <el-dropdown-item command="purchaserProfile">个人信息</el-dropdown-item>
                    <el-dropdown-item command="feedback">意见反馈</el-dropdown-item>
                  </template>
                  <template v-if="userStore.isMerchant">
                    <el-dropdown-item command="dashboard">商家后台</el-dropdown-item>
                    <el-dropdown-item command="merchantMedicines">药材管理</el-dropdown-item>
                    <el-dropdown-item command="merchantOrders">订单管理</el-dropdown-item>
                    <el-dropdown-item command="merchantProfile">商家信息</el-dropdown-item>
                  </template>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
    </header>
    <main class="main-content">
      <router-view />
    </main>
    <footer class="site-footer">
      <div class="footer-container">
        <p>© 2026 中药材销售管理系统 · 传承中医药文化</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const searchKeyword = ref('')

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/medicines', query: { keyword: searchKeyword.value.trim() } })
  }
}

async function handleCommand(command) {
  const routeMap = {
    orders: '/purchaser/orders',
    collections: '/purchaser/collections',
    addresses: '/purchaser/addresses',
    purchaserProfile: '/purchaser/profile',
    feedback: '/purchaser/feedback',
    dashboard: '/merchant/dashboard',
    merchantMedicines: '/merchant/medicines',
    merchantOrders: '/merchant/orders',
    merchantProfile: '/merchant/profile'
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logout()
      router.push('/')
    } catch (_) { /* cancelled */ }
  } else if (routeMap[command]) {
    router.push(routeMap[command])
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.top-nav {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px;
}
.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 20px;
}
.nav-left {
  display: flex;
  align-items: center;
  gap: 32px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #FF7A45;
  white-space: nowrap;
}
.nav-links {
  display: flex;
  gap: 24px;
}
.nav-link {
  font-size: 15px;
  color: #333;
  padding: 4px 0;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}
.nav-link:hover, .nav-link.active {
  color: #FF7A45;
  border-bottom-color: #FF7A45;
}
.nav-center {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 0 40px;
}
.search-input {
  max-width: 400px;
  width: 100%;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.cart-link {
  display: flex;
  align-items: center;
  color: #333;
  cursor: pointer;
  transition: color 0.3s;
}
.cart-link:hover {
  color: #FF7A45;
}
.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.username {
  font-size: 14px;
  color: #333;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.main-content {
  flex: 1;
}
.site-footer {
  background: #fff;
  border-top: 1px solid #EEEEEE;
  padding: 24px 0;
  margin-top: 40px;
}
.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
  color: #8C8C8C;
  font-size: 14px;
}
</style>
