import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'medicines', name: 'MedicineList', component: () => import('../views/MedicineList.vue') },
      { path: 'medicines/:id', name: 'MedicineDetail', component: () => import('../views/MedicineDetail.vue') },
      { path: 'login', name: 'Login', component: () => import('../views/Login.vue') },
      { path: 'register/purchaser', name: 'RegisterPurchaser', component: () => import('../views/RegisterPurchaser.vue') },
      { path: 'register/merchant', name: 'RegisterMerchant', component: () => import('../views/RegisterMerchant.vue') }
    ]
  },
  {
    path: '/purchaser',
    component: () => import('../views/Layout.vue'),
    meta: { requiresAuth: true, role: 'PURCHASER' },
    children: [
      {
        path: '',
        component: () => import('../views/purchaser/UserCenter.vue'),
        children: [
          { path: 'orders', name: 'PurchaserOrders', component: () => import('../views/purchaser/Orders.vue') },
          { path: 'orders/:id', name: 'PurchaserOrderDetail', component: () => import('../views/purchaser/OrderDetail.vue') },
          { path: 'cart', name: 'Cart', component: () => import('../views/purchaser/Cart.vue') },
          { path: 'checkout', name: 'Checkout', component: () => import('../views/purchaser/Checkout.vue') },
          { path: 'collections', name: 'Collections', component: () => import('../views/purchaser/Collections.vue') },
          { path: 'addresses', name: 'Addresses', component: () => import('../views/purchaser/Addresses.vue') },
          { path: 'profile', name: 'PurchaserProfile', component: () => import('../views/purchaser/Profile.vue') },
          { path: 'recommendations', name: 'Recommendations', component: () => import('../views/purchaser/Recommendations.vue') },
          { path: 'feedback', name: 'Feedback', component: () => import('../views/purchaser/Feedback.vue') }
        ]
      }
    ]
  },
  {
    path: '/merchant',
    component: () => import('../views/Layout.vue'),
    meta: { requiresAuth: true, role: 'MERCHANT' },
    children: [
      {
        path: '',
        component: () => import('../views/merchant/MerchantCenter.vue'),
        children: [
          { path: 'dashboard', name: 'MerchantDashboard', component: () => import('../views/merchant/Dashboard.vue') },
          { path: 'medicines', name: 'MerchantMedicines', component: () => import('../views/merchant/Medicines.vue') },
          { path: 'orders', name: 'MerchantOrders', component: () => import('../views/merchant/Orders.vue') },
          { path: 'inventory', name: 'MerchantInventory', component: () => import('../views/merchant/Inventory.vue') },
          { path: 'profile', name: 'MerchantProfile', component: () => import('../views/merchant/Profile.vue') }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')

  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
    const requiredRole = to.matched.find(record => record.meta.role)?.meta.role
    if (requiredRole && userInfo?.role !== requiredRole) {
      next({ name: 'Home' })
      return
    }
  }

  if (to.name === 'Login' && token) {
    if (userInfo?.role === 'MERCHANT') {
      next({ name: 'MerchantDashboard' })
    } else {
      next({ name: 'Home' })
    }
    return
  }

  next()
})

export default router
