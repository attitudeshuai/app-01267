import request from './request'

export function login(data) {
  return request.post('/auth/login', { ...data, role: 'ADMIN' })
}

export function logout() {
  return request.post('/auth/logout')
}
