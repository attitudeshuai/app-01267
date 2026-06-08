import request from './request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function registerPurchaser(data) {
  return request.post('/auth/register/purchaser', data)
}

export function registerMerchant(data) {
  return request.post('/auth/register/merchant', data)
}

export function logout() {
  return request.post('/auth/logout')
}
