import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '首页概览' } },
      { path: 'merchants', name: 'Merchants', component: () => import('../views/MerchantManage.vue'), meta: { title: '商家管理' } },
      { path: 'purchasers', name: 'Purchasers', component: () => import('../views/PurchaserManage.vue'), meta: { title: '采购商管理' } },
      { path: 'medicines', name: 'Medicines', component: () => import('../views/MedicineManage.vue'), meta: { title: '药材管理' } },
      { path: 'categories', name: 'Categories', component: () => import('../views/CategoryManage.vue'), meta: { title: '分类管理' } },
      { path: 'orders', name: 'Orders', component: () => import('../views/OrderManage.vue'), meta: { title: '订单管理' } },
      { path: 'banners', name: 'Banners', component: () => import('../views/BannerManage.vue'), meta: { title: '轮播图管理' } },
      { path: 'announcements', name: 'Announcements', component: () => import('../views/AnnouncementManage.vue'), meta: { title: '公告管理' } },
      { path: 'config', name: 'Config', component: () => import('../views/SystemConfig.vue'), meta: { title: '系统配置' } },
      { path: 'inventory', name: 'Inventory', component: () => import('../views/InventoryWarning.vue'), meta: { title: '库存预警' } },
      { path: 'price-analysis', name: 'PriceAnalysis', component: () => import('../views/PriceAnalysis.vue'), meta: { title: '价格分析' } },
      { path: 'feedbacks', name: 'Feedbacks', component: () => import('../views/FeedbackManage.vue'), meta: { title: '用户反馈' } },
      { path: 'operation-logs', name: 'OperationLogs', component: () => import('../views/OperationLogManage.vue'), meta: { title: '操作日志' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin_token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
