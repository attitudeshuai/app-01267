<template>
  <div class="feedback-page">
    <div class="page-header">
      <h2>意见反馈</h2>
      <p class="subtitle">如有问题或建议，欢迎向我们反馈</p>
    </div>
    <el-card class="feedback-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="反馈类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
            <el-option label="功能建议" :value="1" />
            <el-option label="问题反馈" :value="2" />
            <el-option label="投诉" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入反馈标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="详细描述" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请详细描述您的问题或建议" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.contact" placeholder="选填，便于我们联系您" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交反馈</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { submitFeedback } from '../../api/purchaser'

const formRef = ref(null)
const submitting = ref(false)
const form = reactive({ type: 1, title: '', content: '', contact: '' })
const rules = {
  title: [{ required: true, message: '请输入反馈标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入详细描述', trigger: 'blur' }]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await submitFeedback({ ...form })
    ElMessage.success('反馈提交成功，感谢您的宝贵意见！')
    resetForm()
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.type = 1
  form.title = ''
  form.content = ''
  form.contact = ''
  formRef.value?.resetFields()
}
</script>

<style scoped>
.feedback-page {
  padding: 0 24px;
}
.page-header {
  margin-bottom: 24px;
}
.page-header h2 {
  font-size: 20px;
  color: #333;
  margin: 0 0 8px 0;
}
.subtitle {
  font-size: 14px;
  color: #8C8C8C;
  margin: 0;
}
.feedback-card {
  max-width: 600px;
}
</style>
