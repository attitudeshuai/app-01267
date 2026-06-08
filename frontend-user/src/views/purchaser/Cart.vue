<template>
  <div class="cart-page">
    <div class="page-header">
      <h2>购物车</h2>
    </div>
    <div v-loading="loading" class="cart-content">
      <template v-if="cartItems.length">
        <div class="cart-list">
          <div v-for="item in cartItems" :key="item.id" class="cart-item">
            <el-checkbox v-model="item.checked" @change="calcTotal" />
            <div class="item-image" @click="$router.push(`/medicines/${item.medicineId}`)">
              <img v-if="item.image" :src="item.image" :alt="item.medicineName" />
              <div v-else class="img-placeholder">
                <el-icon :size="28" color="#FF7A45"><FirstAidKit /></el-icon>
              </div>
            </div>
            <div class="item-info">
              <h4 @click="$router.push(`/medicines/${item.medicineId}`)">{{ item.medicineName || item.name }}</h4>
              <p class="item-spec">{{ item.specification || '' }}</p>
            </div>
            <div class="item-price">¥{{ item.price }}</div>
            <el-input-number
              v-model="item.quantity"
              :min="1"
              :max="Math.min(999, item.stockQuantity ?? 999)"
              size="large"
              controls-position="right"
              class="cart-quantity-input"
              @change="(val) => handleQuantityChange(item, val)"
            />
            <div class="item-subtotal">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
            <el-button type="danger" text @click="handleDelete(item.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="cart-footer">
          <div class="footer-left">
            <el-checkbox v-model="allChecked" @change="handleCheckAll">全选</el-checkbox>
            <el-button type="danger" text @click="handleClear">清空购物车</el-button>
          </div>
          <div class="footer-right">
            <span class="total-info">已选 <em>{{ checkedCount }}</em> 件商品，合计：</span>
            <span class="total-price">¥{{ totalPrice }}</span>
            <el-button type="primary" size="large" :disabled="checkedCount === 0" @click="handleCheckout" round>
              去结算
            </el-button>
          </div>
        </div>
      </template>
      <el-empty v-if="!loading && cartItems.length === 0" description="购物车空空如也">
        <el-button type="primary" @click="$router.push('/medicines')" round>去逛逛</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import { getCart, updateCartItem, deleteCartItem, clearCart } from '../../api/purchaser'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const cartItems = ref([])
const loading = ref(false)
const allChecked = ref(false)

const checkedCount = computed(() => cartItems.value.filter(i => i.checked).length)
const totalPrice = computed(() => {
  return cartItems.value
    .filter(i => i.checked)
    .reduce((sum, i) => sum + i.price * i.quantity, 0)
    .toFixed(2)
})

onMounted(() => fetchCart())

async function fetchCart() {
  loading.value = true
  try {
    const res = await getCart()
    cartItems.value = (res.data || []).map(i => ({ ...i, checked: false }))
  } catch (_) { /* error handled */ }
  loading.value = false
}

function calcTotal() {
  allChecked.value = cartItems.value.length > 0 && cartItems.value.every(i => i.checked)
}

function handleCheckAll(val) {
  cartItems.value.forEach(i => (i.checked = val))
}

async function handleQuantityChange(item, val) {
  try {
    await updateCartItem(item.id, val)
    userStore.fetchCartCount()
  } catch (_) {
    fetchCart()
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '提示', { type: 'warning' })
    await deleteCartItem(id)
    ElMessage.success('已删除')
    fetchCart()
    userStore.fetchCartCount()
  } catch (_) { /* cancelled */ }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定要清空购物车吗？', '提示', { type: 'warning' })
    await clearCart()
    cartItems.value = []
    ElMessage.success('已清空')
    userStore.fetchCartCount()
  } catch (_) { /* cancelled */ }
}

function handleCheckout() {
  const items = cartItems.value.filter(i => i.checked)
  if (items.length === 0) return
  sessionStorage.setItem('checkoutItems', JSON.stringify(items))
  router.push('/purchaser/checkout')
}
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}
.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}
.cart-content {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  min-height: 300px;
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #EEEEEE;
}
.cart-item:last-child {
  border-bottom: none;
}
.item-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
}
.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff5f0;
}
.item-info {
  flex: 1;
  min-width: 0;
}
.item-info h4 {
  font-size: 14px;
  color: #333;
  cursor: pointer;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-info h4:hover {
  color: #FF7A45;
}
.item-spec {
  font-size: 12px;
  color: #8C8C8C;
}
.item-price {
  font-size: 14px;
  color: #FF7A45;
  font-weight: 600;
  width: 80px;
  text-align: center;
}
.item-subtotal {
  font-size: 14px;
  color: #333;
  font-weight: 600;
  width: 80px;
  text-align: center;
}
.cart-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 0 0;
  border-top: 1px solid #EEEEEE;
  margin-top: 16px;
}
.footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.footer-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.total-info {
  font-size: 14px;
  color: #8C8C8C;
}
.total-info em {
  color: #FF7A45;
  font-style: normal;
  font-weight: 600;
}
.total-price {
  font-size: 24px;
  font-weight: 700;
  color: #FF7A45;
}

/* 购物车数量输入框 - 更大更易操作 */
.cart-quantity-input {
  width: 140px;
}
.cart-quantity-input :deep(.el-input-number__decrease),
.cart-quantity-input :deep(.el-input-number__increase) {
  width: 36px;
  height: 36px;
  font-size: 16px;
  line-height: 36px;
}
.cart-quantity-input :deep(.el-input__inner) {
  height: 36px;
  font-size: 15px;
  text-align: center;
}
</style>
