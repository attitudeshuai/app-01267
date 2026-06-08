<template>
  <div class="addresses-page">
    <div class="page-header">
      <h2>收货地址</h2>
      <el-button type="primary" @click="openDialog()" round><el-icon><Plus /></el-icon> 新增地址</el-button>
    </div>
    <div v-loading="loading" class="address-list">
      <div v-for="addr in addresses" :key="addr.id" class="address-card">
        <div class="addr-main">
          <div class="addr-top">
            <span class="addr-name">{{ addr.receiverName }}</span>
            <span class="addr-phone">{{ addr.phone }}</span>
            <el-tag v-if="addr.isDefault" size="small" type="warning">默认</el-tag>
          </div>
          <p class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}</p>
        </div>
        <div class="addr-actions">
          <el-button text type="primary" @click="openDialog(addr)">编辑</el-button>
          <el-button v-if="!addr.isDefault" text type="primary" @click="handleSetDefault(addr.id)">设为默认</el-button>
          <el-button text type="danger" @click="handleDelete(addr.id)">删除</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && addresses.length === 0" description="暂无收货地址" />
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑地址' : '新增地址'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="form.province" placeholder="请输入省份" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" placeholder="请输入城市" />
        </el-form-item>
        <el-form-item label="区/县" prop="district">
          <el-input v-model="form.district" placeholder="请输入区/县" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="form.detailAddress" type="textarea" :rows="2" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '../../api/purchaser'
import { ElMessage, ElMessageBox } from 'element-plus'

const addresses = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  receiverName: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
})

const rules = {
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区/县', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

onMounted(() => fetchAddresses())

async function fetchAddresses() {
  loading.value = true
  try {
    const res = await getAddresses()
    addresses.value = res.data || []
  } catch (_) { /* error handled */ }
  loading.value = false
}

function openDialog(addr) {
  if (addr) {
    isEdit.value = true
    Object.assign(form, addr)
  } else {
    isEdit.value = false
    Object.assign(form, { id: null, receiverName: '', phone: '', province: '', city: '', district: '', detailAddress: '', isDefault: false })
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      receiverName: form.receiverName,
      phone: form.phone,
      province: form.province,
      city: form.city,
      district: form.district,
      detailAddress: form.detailAddress,
      isDefault: form.isDefault ? 1 : 0
    }
    if (isEdit.value) {
      payload.id = form.id
      await updateAddress(payload)
    } else {
      await addAddress(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchAddresses()
  } catch (_) { /* error handled */ }
  saving.value = false
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要删除该地址吗？', '提示', { type: 'warning' })
    await deleteAddress(id)
    ElMessage.success('已删除')
    fetchAddresses()
  } catch (_) { /* cancelled */ }
}

async function handleSetDefault(id) {
  try {
    await setDefaultAddress(id)
    ElMessage.success('已设为默认')
    fetchAddresses()
  } catch (_) { /* error handled */ }
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 { font-size: 20px; font-weight: 600; color: #333; }
.address-list { display: flex; flex-direction: column; gap: 12px; min-height: 200px; }
.address-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.addr-top { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.addr-name { font-weight: 600; font-size: 15px; color: #333; }
.addr-phone { font-size: 13px; color: #8C8C8C; }
.addr-detail { font-size: 14px; color: #666; }
.addr-actions { display: flex; gap: 4px; flex-shrink: 0; }
</style>
