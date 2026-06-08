import request from './request'

export function getProfile() {
  return request.get('/merchant/profile')
}

export function updateProfile(data) {
  return request.put('/merchant/profile', data)
}

export function getDashboard() {
  return request.get('/merchant/dashboard')
}

export function getMedicines(params) {
  return request.get('/merchant/medicines', { params })
}

export function getMedicineDetail(id) {
  return request.get(`/merchant/medicines/${id}`)
}

export function addMedicine(data) {
  return request.post('/merchant/medicines', data)
}

export function updateMedicine(data) {
  return request.put('/merchant/medicines', data)
}

export function onShelfMedicine(id) {
  return request.put(`/merchant/medicines/${id}/on-shelf`)
}

export function offShelfMedicine(id) {
  return request.put(`/merchant/medicines/${id}/off-shelf`)
}

export function deleteMedicine(id) {
  return request.delete(`/merchant/medicines/${id}`)
}

export function getOrders(params) {
  return request.get('/merchant/orders', { params })
}

export function getOrderDetail(id) {
  return request.get(`/merchant/orders/${id}`)
}

export function shipOrder(id, trackingNo) {
  return request.put(`/merchant/orders/${id}/ship`, null, { params: { trackingNo } })
}

export function getLowStock() {
  return request.get('/merchant/inventory/low-stock')
}

export function getPriceHistory(medicineId) {
  return request.get(`/merchant/price-history/${medicineId}`)
}

export function getNotifications(params) {
  return request.get('/merchant/notifications', { params })
}

export function markNotificationRead(id) {
  return request.put(`/merchant/notifications/${id}/read`)
}
