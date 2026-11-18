<template>
  <!-- 问答库 -->
  <el-card shadow="never" class="section">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          placeholder="搜索问题关键词"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" plain @click="handleSearch">搜索</el-button>
      </div>
      <el-button type="primary" @click="openCreate">新建问答</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="answer" label="答案" min-width="220" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="110" />
      <el-table-column label="同义问法" width="150">
        <template #default="{ row }">
          {{ joinList(row.synonyms) }}
        </template>
      </el-table-column>
      <el-table-column label="标签" width="130">
        <template #default="{ row }">
          {{ joinList(row.tags) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="hitCount" label="命中次数" width="100" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pagination"
      @current-change="loadQuestions"
    />
  </el-card>

  <!-- 未解答问题 -->
  <el-card shadow="never" class="section">
    <template #header>
      <span>未解答问题</span>
    </template>
    <el-table :data="unanswered" v-loading="unansweredLoading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="question" label="问题" min-width="300" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag type="warning" size="small">{{ row.status === 1 ? '待处理' : '已处理' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提问时间" width="180" />
    </el-table>

    <el-pagination
      v-model:current-page="unansweredPage"
      :page-size="size"
      :total="unansweredTotal"
      layout="total, prev, pager, next"
      class="pagination"
      @current-change="loadUnanswered"
    />
  </el-card>

  <!-- 新建/编辑对话框 -->
  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑问答' : '新建问答'" width="560px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="问题">
        <el-input v-model="form.question" placeholder="标准问题" />
      </el-form-item>
      <el-form-item label="答案">
        <el-input v-model="form.answer" type="textarea" :rows="4" placeholder="标准答案" />
      </el-form-item>
      <el-form-item label="分类">
        <el-input v-model="form.category" placeholder="如 报到/宿舍/选课" />
      </el-form-item>
      <el-form-item label="同义问法">
        <el-input v-model="form.synonyms" placeholder="多个用英文逗号分隔" />
      </el-form-item>
      <el-form-item label="标签">
        <el-input v-model="form.tags" placeholder="多个用英文逗号分隔" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
// 问答库：搜索/新建/行内编辑/删除 + 未解答问题分页
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createQuestion,
  deleteQuestion,
  listQuestions,
  listUnanswered,
  updateQuestion,
} from '@/api/qa'

const size = 10
const keyword = ref('')
const page = ref(1)
const total = ref(0)
const list = ref([])
const loading = ref(false)

const unansweredPage = ref(1)
const unansweredTotal = ref(0)
const unanswered = ref([])
const unansweredLoading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = reactive({
  question: '',
  answer: '',
  category: '',
  synonyms: '',
  tags: '',
  status: 1,
})

// 同义词/标签为数组时逗号拼接展示
function joinList(v) {
  if (Array.isArray(v)) return v.join('、')
  return v || '-'
}

async function loadQuestions() {
  loading.value = true
  try {
    const res = await listQuestions({ page: page.value, size, keyword: keyword.value })
    list.value = res.data.list || []
    total.value = res.data.total || 0
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function loadUnanswered() {
  unansweredLoading.value = true
  try {
    const res = await listUnanswered({ page: unansweredPage.value, size })
    unanswered.value = res.data.list || []
    unansweredTotal.value = res.data.total || 0
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    unansweredLoading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadQuestions()
}

function resetForm() {
  form.question = ''
  form.answer = ''
  form.category = ''
  form.synonyms = ''
  form.tags = ''
  form.status = 1
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.question = row.question
  form.answer = row.answer
  form.category = row.category
  form.synonyms = Array.isArray(row.synonyms) ? row.synonyms.join(',') : row.synonyms || ''
  form.tags = Array.isArray(row.tags) ? row.tags.join(',') : row.tags || ''
  form.status = row.status
  dialogVisible.value = true
}

async function save() {
  if (!form.question || !form.answer) {
    ElMessage.warning('问题和答案不能为空')
    return
  }
  // 同义词/标签按逗号拆为数组
  const split = (v) =>
    v
      .split(/[,，]/)
      .map((s) => s.trim())
      .filter(Boolean)
  const payload = {
    question: form.question,
    answer: form.answer,
    category: form.category,
    synonyms: split(form.synonyms),
    tags: split(form.tags),
    status: form.status,
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateQuestion(editingId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await createQuestion(payload)
      ElMessage.success('新建成功')
    }
    dialogVisible.value = false
    loadQuestions()
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除问答「${row.question}」？`, '删除确认', { type: 'warning' })
  await deleteQuestion(row.id)
  ElMessage.success('已删除')
  loadQuestions()
}

onMounted(() => {
  loadQuestions()
  loadUnanswered()
})
</script>

<style scoped>
.section {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.toolbar-left {
  display: flex;
  gap: 8px;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
