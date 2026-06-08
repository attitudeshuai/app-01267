import request from './request'

export function getDashboard() {
  return request.get('/admin/dashboard')
}

export function getMerchants(params) {
  return request.get('/admin/merchants', { params })
}

export function createMerchant(data) {
  return request.post('/admin/merchants', data)
}

export function updateMerchant(data) {
  return request.put('/admin/merchants', data)
}

export function auditMerchant(id, status) {
  return request.put(`/admin/merchants/${id}/audit`, null, { params: { status } })
}

export function toggleMerchantStatus(id, status) {
  return request.put(`/admin/merchants/${id}/status`, null, { params: { status } })
}

export function getPurchasers(params) {
  return request.get('/admin/purchasers', { params })
}

export function createPurchaser(data) {
  return request.post('/admin/purchasers', data)
}

export function updatePurchaser(data) {
  return request.put('/admin/purchasers', data)
}

export function togglePurchaserStatus(id, status) {
  return request.put(`/admin/purchasers/${id}/status`, null, { params: { status } })
}

export function getMedicines(params) {
  return request.get('/admin/medicines', { params })
}

export function getMedicineDetail(id) {
  return request.get(`/admin/medicines/${id}`)
}

export function createMedicine(data) {
  return request.post('/admin/medicines', data)
}

export function updateMedicine(data) {
  return request.put('/admin/medicines', data)
}

export function deleteMedicine(id) {
  return request.delete(`/admin/medicines/${id}`)
}

export function auditMedicine(id, status) {
  return request.put(`/admin/medicines/${id}/status`, null, { params: { status } })
}

export function getCategories(params) {
  return request.get('/admin/categories', { params })
}

export function getAllCategories() {
  return request.get('/admin/categories/all')
}

export function createCategory(data) {
  return request.post('/admin/categories', data)
}

export function updateCategory(data) {
  return request.put('/admin/categories', data)
}

export function deleteCategory(id) {
  return request.delete(`/admin/categories/${id}`)
}

export function getOrders(params) {
  return request.get('/admin/orders', { params })
}

export function getOrderDetail(id) {
  return request.get(`/admin/orders/${id}`)
}

export function getBanners() {
  return request.get('/admin/banners')
}

export function createBanner(data) {
  return request.post('/admin/banners', data)
}

export function updateBanner(data) {
  return request.put('/admin/banners', data)
}

export function deleteBanner(id) {
  return request.delete(`/admin/banners/${id}`)
}

export function getAnnouncements(params) {
  return request.get('/admin/announcements', { params })
}

export function createAnnouncement(data) {
  return request.post('/admin/announcements', data)
}

export function updateAnnouncement(data) {
  return request.put('/admin/announcements', data)
}

export function deleteAnnouncement(id) {
  return request.delete(`/admin/announcements/${id}`)
}

export function getConfigs() {
  return request.get('/admin/configs')
}

export function updateConfig(data) {
  return request.put('/admin/configs', data)
}

export function getLowStock() {
  return request.get('/admin/inventory/low-stock')
}

export function getPriceHistory(medicineId) {
  return request.get(`/admin/price-history/${medicineId}`)
}

export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/common/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getFeedbacks(params) {
  return request.get('/admin/feedbacks', { params })
}

export function replyFeedback(id, reply) {
  return request.put(`/admin/feedbacks/${id}/reply`, { reply })
}

export function closeFeedback(id) {
  return request.put(`/admin/feedbacks/${id}/close`)
}

export function getOperationLogs(params) {
  return request.get('/admin/operation-logs', { params })
}
