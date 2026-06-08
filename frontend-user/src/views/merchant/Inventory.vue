<template>
  <div class="inventory-page">
    <div class="page-header">
      <h2>库存预警</h2>
    </div>
    <div v-loading="loading" class="inventory-content">
      <el-alert
        v-if="list.length"
        :title="`共 ${list.length} 种药材库存低于预警阈值`"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom:20px"
      />
      <el-table v-if="list.length" :data="list" stripe style="width:100%">
        <el-table-column label="药材" min-width="200">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px">
              <div style="width:40px;height:40px;border-radius:6px;overflow:hidden;flex-shrink:0;background:#fff5f0;display:flex;align-items:center;justify-content:center">
                <img v-if="row.image" :src="row.image" style="width:100%;height:100%;object-fit:cover" />
                <el-icon v-else :size="18" color="#FF7A45"><FirstAidKit /></el-icon>
              </div>
              <span style="font-weight:500">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="stockQuantity" label="当前库存" width="120">
          <template #default="{ row }">
            <span style="color:#F5222D;font-weight:600">{{ row.stockQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="warningThreshold" label="预警阈值" width="120" />
        <el-table-column prop="price" label="单价" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag type="danger" size="small" v-if="row.stockQuantity === 0">缺货</el-tag>
            <el-tag type="warning" size="small" v-else>低库存</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="$router.push('/merchant/medicines')">补货</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无库存预警，一切正常">
        <template #image>
          <el-icon :size="64" color="#52C41A"><CircleCheck /></el-icon>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getLowStock } from '../../api/merchant'

const list = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getLowStock()
    list.value = res.data || []
  } catch (_) { /* error handled */ }
  loading.value = false
})
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #333; }
.inventory-content { min-height: 300px; }
</style>
