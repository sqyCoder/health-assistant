<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">管理后台</h2>
    </div>

    <div class="page-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="用户管理" name="users">
          <div class="toolbar">
            <el-input v-model="userKeyword" placeholder="用户名/昵称" style="width: 220px" clearable @keyup.enter="loadUsers" />
            <el-button type="primary" @click="loadUsers">查询</el-button>
          </div>
          <el-table v-loading="userLoading" :data="users" empty-text="暂无用户">
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="nickname" label="昵称" min-width="110" />
            <el-table-column label="角色" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ row.role }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastLoginAt" label="最近登录" width="170" />
            <el-table-column prop="createdAt" label="注册时间" width="170" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleUserStatus(row as UserInfo)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button link type="warning" @click="resetPassword(row as UserInfo)">重置密码</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="userTotal > pageSize"
            class="mt-16"
            layout="total, prev, pager, next"
            :total="userTotal"
            :page-size="pageSize"
            :current-page="userPage"
            @current-change="(page: number) => { userPage = page; loadUsers() }"
          />
        </el-tab-pane>

        <el-tab-pane label="指标字典" name="metrics">
          <div class="toolbar">
            <el-input v-model="metricKeyword" placeholder="指标名称/编码" style="width: 220px" clearable @keyup.enter="loadMetrics" />
            <el-button type="primary" @click="loadMetrics">查询</el-button>
            <el-button type="success" @click="openMetricDialog(null)">新增指标</el-button>
          </div>
          <el-table v-loading="metricLoading" :data="metrics" empty-text="暂无指标">
            <el-table-column prop="name" label="指标名称" min-width="140" />
            <el-table-column prop="code" label="编码" width="100" />
            <el-table-column prop="categoryName" label="分类" width="110" />
            <el-table-column prop="unit" label="单位" width="90" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openMetricDialog(row as MetricItem)">编辑</el-button>
                <el-button link type="primary" @click="openRangesDialog(row as MetricItem)">参考范围</el-button>
                <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleMetricStatus(row as MetricItem)">
                  {{ row.status === 1 ? '停用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="metricTotal > pageSize"
            class="mt-16"
            layout="total, prev, pager, next"
            :total="metricTotal"
            :page-size="pageSize"
            :current-page="metricPage"
            @current-change="(page: number) => { metricPage = page; loadMetrics() }"
          />
        </el-tab-pane>

        <el-tab-pane label="Prompt 模板" name="prompts">
          <div class="toolbar">
            <el-button type="primary" @click="openPromptDialog">新建版本</el-button>
          </div>
          <el-table v-loading="promptLoading" :data="prompts" empty-text="暂无模板">
            <el-table-column type="expand">
              <template #default="{ row }">
                <pre class="prompt-content">{{ row.content }}</pre>
              </template>
            </el-table-column>
            <el-table-column prop="code" label="场景码" width="150" />
            <el-table-column prop="name" label="名称" min-width="150" />
            <el-table-column prop="version" label="版本" width="70" />
            <el-table-column prop="temperature" label="温度" width="70" />
            <el-table-column prop="maxTokens" label="maxTokens" width="100" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'ENABLED' ? 'success' : 'info'">
                  {{ row.status === 'ENABLED' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link :type="row.status === 'ENABLED' ? 'danger' : 'success'" @click="togglePromptStatus(row as PromptTemplate)">
                  {{ row.status === 'ENABLED' ? '停用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="AI 日志" name="logs">
          <div class="summary-row">
            <div class="summary-card">
              <div class="summary-value">{{ logSummaryTotals.count }}</div>
              <div class="summary-label">近 30 天调用次数</div>
            </div>
            <div class="summary-card">
              <div class="summary-value">{{ logSummaryTotals.tokens }}</div>
              <div class="summary-label">总 Token</div>
            </div>
            <div class="summary-card">
              <div class="summary-value">¥{{ logSummaryTotals.cost.toFixed(4) }}</div>
              <div class="summary-label">预估费用</div>
            </div>
          </div>
          <div class="toolbar">
            <el-select v-model="logScene" placeholder="场景" clearable style="width: 160px" @change="loadLogs">
              <el-option label="报告解析" value="REPORT_PARSE" />
              <el-option label="解读" value="INTERPRET" />
              <el-option label="建议" value="SUGGESTION" />
              <el-option label="对话" value="CHAT" />
            </el-select>
            <el-button @click="loadLogs">刷新</el-button>
          </div>
          <el-table v-loading="logLoading" :data="logs" empty-text="暂无日志">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="scene" label="场景" width="130" />
            <el-table-column prop="model" label="模型" width="130" />
            <el-table-column prop="promptTokens" label="输入" width="80" />
            <el-table-column prop="completionTokens" label="输出" width="80" />
            <el-table-column prop="totalTokens" label="总Token" width="90" />
            <el-table-column label="费用" width="100">
              <template #default="{ row }">¥{{ Number(row.costEstimate ?? 0).toFixed(6) }}</template>
            </el-table-column>
            <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
            <el-table-column label="流式" width="70">
              <template #default="{ row }">{{ row.isStream === 1 ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'SUCCESS' ? 'success' : 'danger'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="logTotal > pageSize"
            class="mt-16"
            layout="total, prev, pager, next"
            :total="logTotal"
            :page-size="pageSize"
            :current-page="logPage"
            @current-change="(page: number) => { logPage = page; loadLogs() }"
          />
        </el-tab-pane>

        <el-tab-pane label="任务队列" name="tasks">
          <div class="toolbar">
            <el-select v-model="taskStatus" placeholder="状态" clearable style="width: 140px" @change="loadTasks">
              <el-option label="等待中" value="PENDING" />
              <el-option label="执行中" value="RUNNING" />
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAILED" />
            </el-select>
            <el-select v-model="taskType" placeholder="类型" clearable style="width: 180px" @change="loadTasks">
              <el-option label="报告解析" value="REPORT_PARSE" />
              <el-option label="AI 解读" value="AI_INTERPRET" />
              <el-option label="AI 建议" value="AI_SUGGESTION" />
              <el-option label="PDF 导出" value="PDF_EXPORT" />
            </el-select>
            <el-button @click="loadTasks">刷新</el-button>
          </div>
          <el-table v-loading="taskLoading" :data="tasks" empty-text="暂无任务">
            <el-table-column prop="id" label="任务ID" width="180" show-overflow-tooltip />
            <el-table-column prop="taskType" label="类型" width="140" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="taskTagType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="progress" label="进度" width="80">
              <template #default="{ row }">{{ row.progress }}%</template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="170" />
            <el-table-column prop="errorMsg" label="错误信息" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="warning" :disabled="row.status === 'RUNNING' || row.status === 'PENDING'" @click="retryTask(row as TaskInfo)">
                  重试
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="taskTotal > pageSize"
            class="mt-16"
            layout="total, prev, pager, next"
            :total="taskTotal"
            :page-size="pageSize"
            :current-page="taskPage"
            @current-change="(page: number) => { taskPage = page; loadTasks() }"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="metricDialogVisible" :title="metricForm.id ? '编辑指标' : '新增指标'" width="620px">
      <el-form :model="metricForm" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="分类" required>
              <el-select v-model="metricForm.categoryId" style="width: 100%">
                <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="编码" required>
              <el-input v-model="metricForm.code" :disabled="Boolean(metricForm.id)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" required>
              <el-input v-model="metricForm.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="英文/缩写">
              <el-input v-model="metricForm.nameEn" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位">
              <el-input v-model="metricForm.unit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型">
              <el-select v-model="metricForm.valueType" style="width: 100%">
                <el-option label="数值型" value="NUMERIC" />
                <el-option label="定性型" value="TEXT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="别名">
              <el-input v-model="metricForm.alias" placeholder="逗号分隔，用于解析匹配" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="说明">
              <el-input v-model="metricForm.description" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="metricDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMetric">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rangeDialogVisible" title="参考范围（保存后全量替换）" width="720px">
      <div class="toolbar">
        <el-button type="primary" size="small" @click="addRangeRow">添加范围</el-button>
      </div>
      <div v-for="(range, index) in rangeRows" :key="index" class="range-row">
        <el-select v-model="range.gender" style="width: 90px">
          <el-option label="通用" value="ALL" />
          <el-option label="男" value="M" />
          <el-option label="女" value="F" />
        </el-select>
        <el-input-number v-model="range.ageMin" :min="0" :max="150" placeholder="年龄起" controls-position="right" style="width: 110px" />
        <el-input-number v-model="range.ageMax" :min="0" :max="150" placeholder="年龄止" controls-position="right" style="width: 110px" />
        <el-input-number v-model="range.refMin" :precision="3" placeholder="下限" controls-position="right" style="width: 130px" />
        <el-input-number v-model="range.refMax" :precision="3" placeholder="上限" controls-position="right" style="width: 130px" />
        <el-input v-model="range.refText" placeholder="文本如 9-50" style="width: 120px" />
        <el-button link type="danger" @click="rangeRows.splice(index, 1)">删除</el-button>
      </div>
      <template #footer>
        <el-button @click="rangeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRanges">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="promptDialogVisible" title="新建 Prompt 版本" width="680px">
      <el-form :model="promptForm" label-width="90px">
        <el-form-item label="场景码" required>
          <el-select v-model="promptForm.code" style="width: 100%">
            <el-option label="AI_INTERPRET" value="AI_INTERPRET" />
            <el-option label="AI_SUGGESTION" value="AI_SUGGESTION" />
            <el-option label="REPORT_PARSE" value="REPORT_PARSE" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="promptForm.name" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="promptForm.content" type="textarea" :rows="12" />
        </el-form-item>
        <el-form-item label="温度 / maxTokens">
          <el-input-number v-model="promptForm.temperature" :min="0" :max="1" :step="0.1" style="width: 140px" />
          <el-input-number v-model="promptForm.maxTokens" :min="256" :max="16384" :step="512" style="width: 160px; margin-left: 12px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="promptDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePrompt">创建并启用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import { metricApi } from '@/api/metric'
import type { AiLogRecord, MetricCategory, MetricItem, PromptTemplate, TaskInfo, UserInfo } from '@/types'

const activeTab = ref('users')
const pageSize = 10
const saving = ref(false)

interface MetricFormState {
  id?: string
  categoryId?: string
  code: string
  name: string
  nameEn: string
  alias: string
  unit: string
  valueType: string
  description: string
  status: number
}

interface RangeFormState {
  gender: string
  ageMin?: number
  ageMax?: number
  refMin?: number
  refMax?: number
  refText: string
}

const users = ref<UserInfo[]>([])
const userLoading = ref(false)
const userKeyword = ref('')
const userPage = ref(1)
const userTotal = ref(0)

const metrics = ref<MetricItem[]>([])
const metricLoading = ref(false)
const metricKeyword = ref('')
const metricPage = ref(1)
const metricTotal = ref(0)
const categories = ref<MetricCategory[]>([])
const metricDialogVisible = ref(false)
const metricForm = reactive<MetricFormState>({
  categoryId: undefined,
  code: '',
  name: '',
  nameEn: '',
  alias: '',
  unit: '',
  valueType: 'NUMERIC',
  description: '',
  status: 1,
})
const rangeDialogVisible = ref(false)
const rangeMetricId = ref<string>()
const rangeRows = reactive<RangeFormState[]>([])

const prompts = ref<PromptTemplate[]>([])
const promptLoading = ref(false)
const promptDialogVisible = ref(false)
const promptForm = reactive({
  code: 'AI_INTERPRET',
  name: '',
  content: '',
  temperature: 0.3,
  maxTokens: 8192,
})

const logs = ref<AiLogRecord[]>([])
const logLoading = ref(false)
const logScene = ref<string>()
const logPage = ref(1)
const logTotal = ref(0)
const logSummary = ref<{ date: string; count: number; tokens: number; cost: number }[]>([])

const tasks = ref<TaskInfo[]>([])
const taskLoading = ref(false)
const taskStatus = ref<string>()
const taskType = ref<string>()
const taskPage = ref(1)
const taskTotal = ref(0)

const logSummaryTotals = computed(() => {
  let count = 0
  let tokens = 0
  let cost = 0
  logSummary.value.forEach((item) => {
    count += Number(item.count ?? 0)
    tokens += Number(item.tokens ?? 0)
    cost += Number(item.cost ?? 0)
  })
  return { count, tokens, cost }
})

onMounted(async () => {
  categories.value = await metricApi.categories()
  await loadUsers()
})

function onTabChange(tab: string | number): void {
  const name = String(tab)
  if (name === 'users') loadUsers()
  else if (name === 'metrics') loadMetrics()
  else if (name === 'prompts') loadPrompts()
  else if (name === 'logs') loadLogs()
  else if (name === 'tasks') loadTasks()
}

async function loadUsers(): Promise<void> {
  userLoading.value = true
  try {
    const page = await adminApi.users({ keyword: userKeyword.value || undefined, pageNum: userPage.value, pageSize })
    users.value = page.list
    userTotal.value = page.total
  } finally {
    userLoading.value = false
  }
}

async function toggleUserStatus(user: UserInfo): Promise<void> {
  const nextStatus = user.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确定${nextStatus === 0 ? '禁用' : '启用'}用户「${user.username}」吗？`, '提示', { type: 'warning' })
  await adminApi.updateUserStatus(user.id, nextStatus)
  ElMessage.success('已更新')
  await loadUsers()
}

async function resetPassword(user: UserInfo): Promise<void> {
  await ElMessageBox.confirm(`重置「${user.username}」的密码？其所有登录将失效。`, '提示', { type: 'warning' })
  const result = await adminApi.resetPassword(user.id)
  await ElMessageBox.alert(`临时密码：${result.tempPassword}`, '重置成功（请立即告知用户并建议修改）', { confirmButtonText: '知道了' })
}

async function loadMetrics(): Promise<void> {
  metricLoading.value = true
  try {
    const page = await adminApi.metrics({ keyword: metricKeyword.value || undefined, pageNum: metricPage.value, pageSize })
    metrics.value = page.list
    metricTotal.value = page.total
  } finally {
    metricLoading.value = false
  }
}

function openMetricDialog(metric: MetricItem | null): void {
  metricForm.id = metric?.id
  metricForm.categoryId = categories.value[0]?.id
  metricForm.code = metric?.code ?? ''
  metricForm.name = metric?.name ?? ''
  metricForm.nameEn = metric?.nameEn ?? ''
  metricForm.alias = metric?.alias ?? ''
  metricForm.unit = metric?.unit ?? ''
  metricForm.valueType = metric?.valueType ?? 'NUMERIC'
  metricForm.description = metric?.description ?? ''
  metricForm.status = 1
  metricDialogVisible.value = true
}

async function saveMetric(): Promise<void> {
  saving.value = true
  try {
    if (metricForm.id) {
      await adminApi.updateMetric(metricForm.id, { ...metricForm })
    } else {
      await adminApi.createMetric({ ...metricForm })
    }
    ElMessage.success('已保存')
    metricDialogVisible.value = false
    await loadMetrics()
  } finally {
    saving.value = false
  }
}

async function toggleMetricStatus(metric: MetricItem): Promise<void> {
  const nextStatus = metric.status === 1 ? 0 : 1
  await adminApi.updateMetricStatus(metric.id, nextStatus)
  ElMessage.success('已更新')
  await loadMetrics()
}

async function openRangesDialog(metric: MetricItem): Promise<void> {
  rangeMetricId.value = metric.id
  const detail = await metricApi.detail(metric.id)
  rangeRows.splice(0, rangeRows.length)
  if (detail.ranges && detail.ranges.length > 0) {
    detail.ranges.forEach((range) => {
      rangeRows.push({
        gender: range.gender,
        ageMin: range.ageMin,
        ageMax: range.ageMax,
        refMin: range.refMin,
        refMax: range.refMax,
        refText: range.refText ?? '',
      })
    })
  } else {
    addRangeRow()
  }
  rangeDialogVisible.value = true
}

function addRangeRow(): void {
  rangeRows.push({ gender: 'ALL', ageMin: 18, ageMax: 150, refMin: undefined, refMax: undefined, refText: '' })
}

async function saveRanges(): Promise<void> {
  if (!rangeMetricId.value) {
    return
  }
  saving.value = true
  try {
    await adminApi.replaceRanges(rangeMetricId.value, rangeRows.map((row) => ({ ...row })))
    ElMessage.success('参考范围已保存')
    rangeDialogVisible.value = false
    await loadMetrics()
  } finally {
    saving.value = false
  }
}

async function loadPrompts(): Promise<void> {
  promptLoading.value = true
  try {
    prompts.value = await adminApi.prompts()
  } finally {
    promptLoading.value = false
  }
}

function openPromptDialog(): void {
  promptForm.code = 'AI_INTERPRET'
  promptForm.name = ''
  promptForm.content = ''
  promptForm.temperature = 0.3
  promptForm.maxTokens = 8192
  promptDialogVisible.value = true
}

async function savePrompt(): Promise<void> {
  if (!promptForm.name || !promptForm.content) {
    ElMessage.warning('请填写名称与内容')
    return
  }
  saving.value = true
  try {
    await adminApi.createPrompt({ ...promptForm })
    ElMessage.success('新版本已创建并启用')
    promptDialogVisible.value = false
    await loadPrompts()
  } finally {
    saving.value = false
  }
}

async function togglePromptStatus(prompt: PromptTemplate): Promise<void> {
  const nextStatus = prompt.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  await adminApi.updatePromptStatus(prompt.id, nextStatus)
  ElMessage.success('已更新')
  await loadPrompts()
}

async function loadLogs(): Promise<void> {
  logLoading.value = true
  try {
    const [page, summary] = await Promise.all([
      adminApi.aiLogs({ scene: logScene.value, pageNum: logPage.value, pageSize }),
      adminApi.aiLogSummary(30),
    ])
    logs.value = page.list
    logTotal.value = page.total
    logSummary.value = summary
  } finally {
    logLoading.value = false
  }
}

async function loadTasks(): Promise<void> {
  taskLoading.value = true
  try {
    const page = await adminApi.tasks({
      status: taskStatus.value,
      taskType: taskType.value,
      pageNum: taskPage.value,
      pageSize,
    })
    tasks.value = page.list
    taskTotal.value = page.total
  } finally {
    taskLoading.value = false
  }
}

async function retryTask(task: TaskInfo): Promise<void> {
  await adminApi.retryTask(task.id)
  ElMessage.success('已提交重试')
  await loadTasks()
}

function taskTagType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'warning'
  return 'info'
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.summary-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.summary-card {
  flex: 1;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 14px;
  text-align: center;
}

.summary-value {
  font-size: 22px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.summary-label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.prompt-content {
  white-space: pre-wrap;
  background: var(--el-fill-color-light);
  padding: 10px;
  border-radius: 6px;
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
}

.range-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>
