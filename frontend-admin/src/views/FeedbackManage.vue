<template>
  <div class="page-container">
    <div class="page-header">
      <h2>用户反馈</h2>
    </div>
    <div class="card-wrapper">
      <div class="filter-bar">
        <el-select v-model="query.type" placeholder="反馈类型" clearable style="width: 140px" @change="fetchData">
          <el-option label="功能建议" :value="1" />
          <el-option label="问题反馈" :value="2" />
          <el-option label="投诉" :value="3" />
          <el-option label="其他" :value="4" />
        </el-select>
        <el-select v-model="query.status" placeholder="处理状态" clearable style="width: 140px" @change="fetchData">
          <el-option label="待处理" :value="0" />
          <el-option label="已处理" :value="1" />
          <el-option label="已关闭" :value="2" />
        </el-select>
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="purchaserId" label="用户ID" width="90" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">{{ feedbackTypeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contact" label="联系方式" width="120" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="feedbackStatusTag(row.status).type">{{ feedbackStatusTag(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="primary" size="small" @click="openReply(row)">回复</el-button>
            <el-button v-if="row.status === 0" type="warning" size="small" @click="handleClose(row)">关闭</el-button>
            <el-button type="info" size="small" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="replyVisible" title="回复反馈" width="500px" destroy-on-close>
      <el-input v-model="replyContent" type="textarea" :rows="4" placeholder="请输入回复内容" />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleReply">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="反馈详情" width="560px" destroy-on-close>
      <el-descriptions :column="1" border v-if="currentRow">
        <el-descriptions-item label="标题">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ feedbackTypeLabel(currentRow.type) }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ currentRow.content }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ currentRow.contact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ feedbackStatusTag(currentRow.status).label }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentRow.createdTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRow.reply" label="管理员回复">{{ currentRow.reply }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRow.replyTime" label="回复时间">{{ currentRow.replyTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFeedbacks, replyFeedback, closeFeedback } from '../api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, status: null, type: null })
const replyVisible = ref(false)
const detailVisible = ref(false)
const currentRow = ref(null)
const replyContent = ref('')

function feedbackTypeLabel(type) {
  const map = { 1: '功能建议', 2: '问题反馈', 3: '投诉', 4: '其他' }
  return map[type] || '未知'
}

function feedbackStatusTag(status) {
  const map = { 0: { label: '待处理', type: 'warning' }, 1: { label: '已处理', type: 'success' }, 2: { label: '已关闭', type: 'info' } }
  return map[status] ?? { label: '未知', type: 'info' }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getFeedbacks(query)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function openReply(row) {
  currentRow.value = row
  replyContent.value = ''
  replyVisible.value = true
}

function openDetail(row) {
  currentRow.value = row
  detailVisible.value = true
}

async function handleReply() {
  if (!replyContent.value?.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  submitLoading.value = true
  try {
    await replyFeedback(currentRow.value.id, replyContent.value)
    ElMessage.success('回复成功')
    replyVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

async function handleClose(row) {
  try {
    await ElMessageBox.confirm(`确定关闭反馈「${row.title}」吗？`, '提示', { type: 'warning' })
    await closeFeedback(row.id)
    ElMessage.success('已关闭')
    fetchData()
  } catch { /* cancelled */ }
}

onMounted(fetchData)
</script>
