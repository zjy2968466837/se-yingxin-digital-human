<template>
  <el-row :gutter="16">
    <!-- 左侧：报到步骤模板（展开行显示材料清单） -->
    <el-col :span="16">
      <el-card shadow="never" header="报到步骤模板">
        <el-table
          :data="steps"
          v-loading="loading"
          row-key="stepCode"
          stripe
          @expand-change="handleExpand"
        >
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="material-box">
                <div v-if="materialMap[row.stepCode]?.loading" v-loading="true" class="loading-box" />
                <template v-else>
                  <el-empty
                    v-if="!(materialMap[row.stepCode]?.list || []).length"
                    description="该步骤无材料清单"
                    :image-size="60"
                  />
                  <el-table v-else :data="materialMap[row.stepCode].list" size="small">
                    <el-table-column prop="name" label="材料名称" width="180" />
                    <el-table-column label="是否必需" width="100">
                      <template #default="{ row: m }">
                        <el-tag :type="m.required ? 'danger' : 'info'" size="small">
                          {{ m.required ? '必需' : '可选' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="remark" label="说明" min-width="200" />
                  </el-table>
                </template>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="seq" label="顺序" width="70" align="center" />
          <el-table-column prop="stepName" label="步骤名称" min-width="140" />
          <el-table-column prop="location" label="办理地点" min-width="140" />
          <el-table-column prop="officeHours" label="办公时间" min-width="130" />
          <el-table-column prop="contact" label="联系方式" min-width="130" />
        </el-table>
      </el-card>
    </el-col>

    <!-- 右侧：各学院报到进度 -->
    <el-col :span="8">
      <el-card shadow="never" header="各学院报到进度">
        <el-empty v-if="!(colleges || []).length" description="暂无数据" />
        <div v-else class="college-list">
          <div v-for="c in colleges" :key="c.college" class="college-item">
            <div class="college-line">
              <span class="college-name">{{ c.college }}</span>
              <span class="college-num">{{ c.done }}/{{ c.total }}</span>
            </div>
            <el-progress :percentage="pct(c.rate)" :stroke-width="10" />
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
// 报到管理：步骤模板（可展开材料清单）+ 各学院报到进度
import { computed, onMounted, reactive, ref } from 'vue'
import { getOverview } from '@/api/dashboard'
import { listMaterials, listSteps } from '@/api/checkin'

const loading = ref(false)
const steps = ref([])
// 已加载的材料清单缓存：stepCode -> { list }
const materialMap = reactive({})
const colleges = ref(null)

function pct(rate) {
  if (rate === undefined || rate === null) return 0
  return Math.round(rate * 100)
}

async function loadSteps() {
  loading.value = true
  try {
    const res = await listSteps()
    steps.value = res.data
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    loading.value = false
  }
}

// 展开行时按需加载该步骤的材料清单
function handleExpand(row, expandedRows) {
  if (expandedRows.includes(row)) {
    loadMaterials(row.stepCode)
  }
}

async function loadMaterials(stepCode) {
  if (materialMap[stepCode]) return
  materialMap[stepCode] = { loading: true, list: [] }
  try {
    const res = await listMaterials(stepCode)
    materialMap[stepCode] = { loading: false, list: res.data }
  } catch {
    materialMap[stepCode] = { loading: false, list: [] }
  }
}

async function loadColleges() {
  try {
    const res = await getOverview()
    colleges.value = res.data?.colleges || []
  } catch {
    // 错误提示由拦截器统一处理
  }
}

onMounted(() => {
  loadSteps()
  loadColleges()
})
</script>

<style scoped>
.material-box {
  padding: 8px 16px;
}
.loading-box {
  height: 60px;
}
.college-list {
  max-height: 480px;
  overflow-y: auto;
}
.college-item {
  margin-bottom: 16px;
}
.college-line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}
.college-name {
  font-size: 14px;
  color: #303133;
}
.college-num {
  font-size: 13px;
  color: #909399;
}
</style>
