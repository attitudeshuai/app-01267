<template>
  <div class="page-container">
    <div class="page-header"><h2>订单管理</h2></div>
    <div class="card-wrapper">
      <div class="filter-bar">
        <el-input v-model="query.orderNo" placeholder="搜索订单号" clearable style="width: 220px" @clear="fetchData" @keyup.enter="fetchData" />
        <el-select v-model="query.orderStatus" placeholder="订单状态" clearable style="width: 150px" @change="fetchData">
          <el-option label="待支付" :value="0" />
          <el-option label="待发货" :value="1" />
          <el-option label="已发货" :value="2" />
          <el-option label="已收货" :value="3" />
          <el-option label="已完成" :value="4" />
          <el-option label="已取消" :value="5" />
        </el-select>
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="purchaserName" label="采购商" width="120" />
        <el-table-column prop="merchantName" label="商家" width="120" />
        <el-table-column prop="totalAmount" label="金额" width="110">
          <template #default="{ row }">¥{{ (row.totalAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.orderStatus).type">{{ orderStatusTag(row.orderStatus).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="下单时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="订单详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="orderStatusTag(detail.orderStatus).type">{{ orderStatusTag(detail.orderStatus).label }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="采购商">{{ detail.purchaserName }}</el-descriptions-item>
        <el-descriptions-item label="商家">{{ detail.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ (detail.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatTime(detail.createdTime) }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ detail.receiverAddress || '-' }}</el-descriptions-item>
      </el-descriptions>
      <h4 style="margin: 16px 0 8px;">订单商品</h4>
      <el-table :data="detail?.details || []" stripe size="small">
        <el-table-column prop="medicineName" label="药材名称" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="price" label="单价" width="100">
          <template #default="{ row }">¥{{ (row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="小计" width="110">
          <template #default="{ row }">¥{{ ((row.price || 0) * (row.quantity || 0)).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOrders, getOrderDetail } from '../api/admin'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, orderStatus: '', orderNo: '' })
const detailVisible = ref(false)
const detail = ref(null)

function orderStatusTag(s) {
  const map = {
    0: { label: '待支付', type: 'warning' },
    1: { label: '待发货', type: '' },
    2: { label: '已发货', type: 'primary' },
    3: { label: '已收货', type: 'success' },
    4: { label: '已完成', type: 'success' },
    5: { label: '已取消', type: 'info' }
  }
  return map[s] || { label: '未知', type: 'info' }
}

function formatTime(val) {
  if (!val) return '-'
  const s = String(val)
  if (s.length >= 19) return s.substring(0, 19).replace('T', ' ')
  return s
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getOrders(query)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function showDetail(row) {
  try {
    const res = await getOrderDetail(row.id)
    detail.value = res.data || row
    detailVisible.value = true
  } catch { /* handled */ }
}

onMounted(fetchData)
</script>
