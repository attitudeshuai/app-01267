<template>
  <div class="checkout-page">
    <div class="page-header">
      <h2>确认订单</h2>
    </div>
    <div v-loading="loading" class="checkout-content">
      <section class="address-section">
        <h3 class="section-title">收货地址</h3>
        <div v-if="addresses.length" class="address-list">
          <div
            v-for="addr in addresses"
            :key="addr.id"
            class="address-card"
            :class="{ active: selectedAddressId === addr.id }"
            @click="selectedAddressId = addr.id"
          >
            <div class="addr-info">
              <span class="addr-name">{{ addr.receiverName }}</span>
              <span class="addr-phone">{{ addr.phone }}</span>
              <el-tag v-if="addr.isDefault" size="small" type="warning">默认</el-tag>
            </div>
            <p class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}</p>
          </div>
        </div>
        <el-empty v-else description="暂无收货地址">
          <el-button type="primary" @click="$router.push('/purchaser/addresses')" round>添加地址</el-button>
        </el-empty>
      </section>

      <section class="items-section">
        <h3 class="section-title">商品清单</h3>
        <div class="order-items">
          <div v-for="item in checkoutItems" :key="item.id || item.medicineId" class="order-item">
            <div class="oi-image">
              <img v-if="item.image" :src="item.image" />
              <div v-else class="oi-placeholder">
                <el-icon :size="24" color="#FF7A45"><FirstAidKit /></el-icon>
              </div>
            </div>
            <div class="oi-info">
              <span class="oi-name">{{ item.medicineName || item.name }}</span>
              <span class="oi-spec">{{ item.specification || '' }}</span>
            </div>
            <span class="oi-price">¥{{ item.price }}</span>
            <span class="oi-qty">x{{ item.quantity }}</span>
            <span class="oi-subtotal">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
          </div>
        </div>
      </section>

      <section class="remark-section">
        <h3 class="section-title">订单备注</h3>
        <el-input v-model="remark" type="textarea" :rows="2" placeholder="选填，请输入订单备注" />
      </section>

      <div class="checkout-footer">
        <div class="total-row">
          <span>共 {{ totalQuantity }} 件商品，合计：</span>
          <span class="total-price">¥{{ totalPrice }}</span>
        </div>
        <el-button type="primary" size="large" :loading="submitting" :disabled="!selectedAddressId" @click="handleSubmit" round>
          提交订单
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAddresses, createOrder } from '../../api/purchaser'
import { ElMessage } from 'element-plus'

const router = useRouter()
const addresses = ref([])
const selectedAddressId = ref(null)
const checkoutItems = ref([])
const remark = ref('')
const loading = ref(false)
const submitting = ref(false)

const totalQuantity = computed(() => checkoutItems.value.reduce((s, i) => s + i.quantity, 0))
const totalPrice = computed(() => checkoutItems.value.reduce((s, i) => s + i.price * i.quantity, 0).toFixed(2))

onMounted(() => {
  const stored = sessionStorage.getItem('checkoutItems')
  if (stored) {
    checkoutItems.value = JSON.parse(stored)
  } else {
    ElMessage.warning('请先选择商品')
    router.push('/purchaser/cart')
    return
  }
  fetchAddresses()
})

async function fetchAddresses() {
  loading.value = true
  try {
    const res = await getAddresses()
    addresses.value = res.data || []
    const defaultAddr = addresses.value.find(a => a.isDefault)
    if (defaultAddr) selectedAddressId.value = defaultAddr.id
    else if (addresses.value.length) selectedAddressId.value = addresses.value[0].id
  } catch (_) { /* error handled */ }
  loading.value = false
}

async function handleSubmit() {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    const items = checkoutItems.value.map(i => ({
      medicineId: i.medicineId || i.id,
      quantity: i.quantity
    }))
    await createOrder({
      addressId: selectedAddressId.value,
      remark: remark.value,
      items
    })
    ElMessage.success('下单成功')
    sessionStorage.removeItem('checkoutItems')
    router.push('/purchaser/orders')
  } catch (_) { /* error handled */ }
  submitting.value = false
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
.checkout-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-left: 10px;
  border-left: 3px solid #FF7A45;
}
.address-section, .items-section, .remark-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.address-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.address-card {
  border: 2px solid #EEEEEE;
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
}
.address-card.active {
  border-color: #FF7A45;
  background: #fff8f5;
}
.addr-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.addr-name {
  font-weight: 600;
  color: #333;
}
.addr-phone {
  color: #8C8C8C;
  font-size: 13px;
}
.addr-detail {
  font-size: 13px;
  color: #666;
}
.order-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.order-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.oi-image {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}
.oi-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.oi-placeholder {
  width: 100%;
  height: 100%;
  background: #fff5f0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.oi-info {
  flex: 1;
}
.oi-name {
  display: block;
  font-size: 14px;
  color: #333;
}
.oi-spec {
  font-size: 12px;
  color: #8C8C8C;
}
.oi-price {
  color: #333;
  width: 80px;
}
.oi-qty {
  color: #8C8C8C;
  width: 50px;
}
.oi-subtotal {
  color: #FF7A45;
  font-weight: 600;
  width: 80px;
  text-align: right;
}
.checkout-footer {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.total-row {
  font-size: 14px;
  color: #666;
}
.total-price {
  font-size: 24px;
  font-weight: 700;
  color: #FF7A45;
}
</style>
