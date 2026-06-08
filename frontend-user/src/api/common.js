import request from './request'

export function getBanners() {
  return request.get('/common/banners')
}

export function getCategories() {
  return request.get('/common/categories')
}

export function getAnnouncements() {
  return request.get('/common/announcements')
}

export function getMedicines(params) {
  return request.get('/common/medicines', { params })
}

export function getMedicineDetail(id) {
  return request.get(`/common/medicines/${id}`)
}

export function getHotMedicines() {
  return request.get('/common/medicines/hot')
}

export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/common/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
