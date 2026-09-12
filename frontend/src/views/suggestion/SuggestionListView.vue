<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">锻炼与康复建议</h2>
      <el-button type="primary" :icon="Plus" @click="openDialog">生成新建议</el-button>
    </div>

    <div class="page-card">
      <el-table v-loading="loading" :data="list" empty-text="暂无建议，点击右上角生成">
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ labelOf(SUGGESTION_TYPE_LABELS, row.suggestionType) }}</template>
        </el-table-column>
        <el-table-column label="目标" width="120">
          <template #default="{ row }">{{ labelOf(GOAL_LABELS, row.goal) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'DONE' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'">
              {{ row.status === 'DONE' ? '已生成' : row.status === 'FAILED' ? '失败' : '生成中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="生成时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/suggestions/${row.id}`)">查看</el-button>
            <el-button link type="danger" @click="remove(row as Suggestion)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > pageSize"
        class="mt-16"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="(page: number) => { pageNum = page; load() }"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="生成新建议" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="建议类型">
          <el-select v-model="form.suggestionType" style="width: 100%">
            <el-option v-for="(label, value) in SUGGESTION_TYPE_LABELS" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标">
          <el-select v-model="form.goal" clearable placeholder="选填" style="width: 100%">
            <el-option v-for="(label, value) in GOAL_LABELS" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据来源">
          <el-radio-group v-model="form.sourceType">
            <el-radio value="MANUAL">当前档案 + 近期运动</el-radio>
            <el-radio value="REPORT" :disabled="!reportId">最近体检报告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert
          title="AI 将结合成员档案、体检异常、既往病史与近 30 天运动数据生成个性化周计划，约 10-30 秒。"
          type="info"
          :closable="false"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="generate">开始生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { reportApi } from '@/api/report'
import { suggestionApi } from '@/api/suggestion'
import { taskApi } from '@/api/task'
import { useMemberStore } from '@/stores/member'
import { GOAL_LABELS, SUGGESTION_TYPE_LABELS, labelOf } from '@/utils/dict'
import type { Suggestion } from '@/types'

const route = useRoute()
const router = useRouter()
const memberStore = useMemberStore()

const loading = ref(false)
const generating = ref(false)
const dialogVisible = ref(false)
const list = ref<Suggestion[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const reportId = ref<string>()

const form = reactive({
  suggestionType: 'COMPREHENSIVE',
  goal: undefined as string | undefined,
  sourceType: 'MANUAL',
})

onMounted(init)
watch(() => memberStore.currentMemberId, init)

async function init(): Promise<void> {
  await Promise.all([load(), loadLatestReport()])
  const sourceReport = route.query.sourceReport
  if (typeof sourceReport === 'string' && sourceReport) {
    reportId.value = sourceReport
    form.sourceType = 'REPORT'
    dialogVisible.value = true
  }
}

async function load(): Promise<void> {
  if (!memberStore.currentMemberId) {
    list.value = []
    return
  }
  loading.value = true
  try {
    const page = await suggestionApi.list(memberStore.currentMemberId, pageNum.value, pageSize)
    list.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function loadLatestReport(): Promise<void> {
  if (!memberStore.currentMemberId) {
    return
  }
  const page = await reportApi.list({ memberId: memberStore.currentMemberId, pageNum: 1, pageSize: 1 })
  reportId.value = page.list[0]?.id
}

function openDialog(): void {
  if (!memberStore.currentMemberId) {
    ElMessage.warning('请先选择成员')
    return
  }
  if (form.sourceType === 'REPORT' && !reportId.value) {
    form.sourceType = 'MANUAL'
  }
  dialogVisible.value = true
}

async function generate(): Promise<void> {
  if (!memberStore.currentMemberId) {
    return
  }
  generating.value = true
  try {
    const { suggestionId, taskId } = await suggestionApi.generate(memberStore.currentMemberId, {
      suggestionType: form.suggestionType,
      sourceType: form.sourceType,
      sourceId: form.sourceType === 'REPORT' ? reportId.value : undefined,
      goal: form.goal,
    })
    for (let i = 0; i < 60; i++) {
      await new Promise((resolve) => setTimeout(resolve, 3000))
      const task = await taskApi.get(taskId)
      if (task.status === 'SUCCESS' || task.status === 'FAILED') {
        if (task.status === 'FAILED') {
          ElMessage.error('生成失败：' + (task.errorMsg || '请重试'))
          return
        }
        break
      }
    }
    dialogVisible.value = false
    ElMessage.success('建议已生成')
    router.push(`/suggestions/${suggestionId}`)
  } finally {
    generating.value = false
  }
}

async function remove(item: Suggestion): Promise<void> {
  await ElMessageBox.confirm('确定删除该建议吗？', '提示', { type: 'warning' })
  await suggestionApi.remove(item.id)
  ElMessage.success('已删除')
  await load()
}
</script>
