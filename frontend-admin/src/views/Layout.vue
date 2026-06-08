<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="layout-aside">
      <div class="logo-area">
        <span class="logo-text">管理后台</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#FF7A45"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>首页概览</span>
        </el-menu-item>
        <el-sub-menu index="user-mgmt">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item index="/merchants">商家管理</el-menu-item>
          <el-menu-item index="/purchasers">采购商管理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="goods-mgmt">
          <template #title>
            <el-icon><Goods /></el-icon>
            <span>药材管理</span>
          </template>
          <el-menu-item index="/medicines">药材列表</el-menu-item>
          <el-menu-item index="/categories">分类管理</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/orders">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-sub-menu index="content-mgmt">
          <template #title>
            <el-icon><Picture /></el-icon>
            <span>内容管理</span>
          </template>
          <el-menu-item index="/banners">轮播图管理</el-menu-item>
          <el-menu-item index="/announcements">公告管理</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/config">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
        <el-menu-item index="/inventory">
          <el-icon><Warning /></el-icon>
          <span>库存预警</span>
        </el-menu-item>
        <el-menu-item index="/price-analysis">
          <el-icon><TrendCharts /></el-icon>
          <span>价格分析</span>
        </el-menu-item>
        <el-sub-menu index="system-mgmt">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/feedbacks">用户反馈</el-menu-item>
          <el-menu-item index="/operation-logs">操作日志</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="header-right">
          <span class="username">{{ userStore.username || '管理员' }}</span>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { DataAnalysis, UserFilled, Goods, List, Picture, Setting, Warning, TrendCharts, Document } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'

const route = useRoute()
const userStore = useUserStore()

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    await userStore.logout()
  } catch {
    // cancelled
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background: #001529;
  overflow-y: auto;
  overflow-x: hidden;
}

.layout-aside::-webkit-scrollbar {
  width: 0;
}

.logo-area {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
}

.layout-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #EEEEEE;
  height: 56px;
  padding: 0 24px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  color: #333;
  font-size: 14px;
}

.layout-main {
  background: #F5F7FA;
  padding: 0;
  overflow-y: auto;
}
</style>
