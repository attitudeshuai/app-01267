import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi } from '../api/auth'
import { getCart } from '../api/purchaser'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const cartCount = ref(0)

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')
  const isPurchaser = computed(() => role.value === 'PURCHASER')
  const isMerchant = computed(() => role.value === 'MERCHANT')

  async function login(form) {
    const res = await loginApi(form)
    token.value = res.data.token
    userInfo.value = res.data
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data))
    if (res.data.role === 'PURCHASER') {
      await fetchCartCount()
    }
    return res
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (_) { /* ignore */ }
    token.value = ''
    userInfo.value = null
    cartCount.value = 0
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  async function fetchCartCount() {
    if (!isPurchaser.value) return
    try {
      const res = await getCart()
      const items = res.data || []
      cartCount.value = Array.isArray(items) ? items.length : 0
    } catch (_) {
      cartCount.value = 0
    }
  }

  function setUserInfo(info) {
    userInfo.value = { ...userInfo.value, ...info }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  return {
    token, userInfo, cartCount,
    isLoggedIn, role, isPurchaser, isMerchant,
    login, logout, fetchCartCount, setUserInfo
  }
})
