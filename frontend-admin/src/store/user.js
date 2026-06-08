import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi } from '../api/auth'
import router from '../router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const username = ref(localStorage.getItem('admin_username') || '')

  async function login(form) {
    const res = await loginApi(form)
    token.value = res.data.token || res.data
    username.value = form.username
    localStorage.setItem('admin_token', token.value)
    localStorage.setItem('admin_username', username.value)
    return res
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      username.value = ''
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_username')
      router.push('/login')
    }
  }

  return { token, username, login, logout }
})
