import request from './request'

export function getProfile() {
  return request.get('/purchaser/profile')
}

export function updateProfile(data) {
  return request.put('/purchaser/profile', data)
}

export function getMedicines(params) {
  return request.get('/purchaser/medicines', { params })
}

export function getMedicineDetail(id) {
  return request.get(`/purchaser/medicines/${id}`)
}

export function getHotMedicines() {
  return request.get('/purchaser/medicines/hot')
}

export function getRecommendMedicines() {
  return request.get('/purchaser/medicines/recommend')
}

export function recordRecommendClick(medicineId) {
  return request.post('/purchaser/recommend/click', null, { params: { medicineId } })
}

export function getCart() {
  return request.get('/purchaser/cart')
}

export function addToCart(medicineId, quantity = 1) {
  return request.post('/purchaser/cart', null, { params: { medicineId, quantity } })
}

export function updateCartItem(id, quantity) {
  return request.put(`/purchaser/cart/${id}`, null, { params: { quantity } })
}

export function deleteCartItem(id) {
  return request.delete(`/purchaser/cart/${id}`)
}

export function clearCart() {
  return request.delete('/purchaser/cart')
}

export function createOrder(data) {
  return request.post('/purchaser/orders', data)
}

export function getOrders(params) {
  return request.get('/purchaser/orders', { params })
}

export function getOrderDetail(id) {
  return request.get(`/purchaser/orders/${id}`)
}

export function payOrder(id) {
  return request.put(`/purchaser/orders/${id}/pay`)
}

export function receiveOrder(id) {
  return request.put(`/purchaser/orders/${id}/receive`)
}

export function cancelOrder(id) {
  return request.put(`/purchaser/orders/${id}/cancel`)
}

export function reviewOrder(data) {
  return request.post('/purchaser/orders/review', data)
}

export function getAddresses() {
  return request.get('/purchaser/addresses')
}

export function addAddress(data) {
  return request.post('/purchaser/addresses', data)
}

export function updateAddress(data) {
  return request.put('/purchaser/addresses', data)
}

export function deleteAddress(id) {
  return request.delete(`/purchaser/addresses/${id}`)
}

export function setDefaultAddress(id) {
  return request.put(`/purchaser/addresses/${id}/default`)
}

export function getCollections(params) {
  return request.get('/purchaser/collections', { params })
}

export function toggleCollection(medicineId) {
  return request.post(`/purchaser/collections/${medicineId}`)
}

export function submitFeedback(data) {
  return request.post('/purchaser/feedback', data)
}
