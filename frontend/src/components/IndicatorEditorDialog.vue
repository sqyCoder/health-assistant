<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="760px"
    top="6vh"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form v-if="showMeta" :model="meta" label-width="80px" class="meta-form">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="报告名称">
            <el-input v-model="meta.reportName" placeholder="选填" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="体检日期" required>
            <el-date-picker v-model="meta.reportDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="体检机构">
            <el-input v-model="meta.institution" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="报告类型">
            <el-select v-model="meta.reportType" style="width: 100%">
              <el-option v-for="(label, value) in REPORT_TYPE_LABELS" :key="value" :label="label" :value="value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="indicator-header">
      <span>指标明细（共 {{ rows.length }} 项）</span>
      <el-button type="primary" size="small" :icon="Plus" @click="addRow">添加一行</el-button>
    </div>

    <div v-for="(row, index) in rows" :key="index" class="indicator-row">
      <el-select
        v-model="row.metricName"
        filterable
        remote
        clearable
        allow-create
        default-first-option
        :remote-method="searchMetrics"
        :loading="metricLoading"
        placeholder="搜索指标名称或直接输入"
        class="metric-select"
        @change="(value: string) => onMetricChange(row, value)"
      >
        <el-option v-for="metric in metricOptions" :key="metric.id" :label="metric.name" :value="metric.name">
          <span>{{ metric.name }}</span>
          <span class="option-hint">{{ metric.unit || '' }} {{ metric.categoryName || '' }}</span>
        </el-option>
      </el-select>
      <el-input v-model="row.valueText" placeholder="检验值" class="value-input" />
      <el-input v-model="row.unit" placeholder="单位" class="unit-input" />
      <el-input v-model="row.refRangeText" placeholder="参考范围，如 9-50" class="ref-input" />
      <el-button link type="danger" :icon="Delete" @click="rows.splice(index, 1)" />
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import { metricApi } from '@/api/metric'
import { REPORT_TYPE_LABELS } from '@/utils/dict'
import type { IndicatorItem, MetricItem } from '@/types'

interface MetaForm {
  reportName: string
  reportDate: string
  institution: string
  reportType: string
}

const props = defineProps<{
  modelValue: boolean
  title: string
  showMeta: boolean
  loading: boolean
  initialIndicators?: IndicatorItem[]
  initialMeta?: Partial<MetaForm>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: { meta: MetaForm | null; indicators: IndicatorItem[] }): void
}>()

const rows = reactive<IndicatorItem[]>([])
const meta = reactive<MetaForm>({
  reportName: '',
  reportDate: new Date().toISOString().slice(0, 10),
  institution: '',
  reportType: 'ANNUAL',
})

const metricOptions = ref<MetricItem[]>([])
const metricLoading = ref(false)

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      return
    }
    rows.splice(0, rows.length)
    const initial = props.initialIndicators ?? []
    if (initial.length === 0) {
      rows.push({ metricName: '', valueText: '', unit: '', refRangeText: '' })
    } else {
      initial.forEach((item) => rows.push({ ...item }))
    }
    meta.reportName = props.initialMeta?.reportName ?? ''
    meta.reportDate = props.initialMeta?.reportDate ?? new Date().toISOString().slice(0, 10)
    meta.institution = props.initialMeta?.institution ?? ''
    meta.reportType = props.initialMeta?.reportType ?? 'ANNUAL'
    metricOptions.value = []
  },
)

async function searchMetrics(keyword: string): Promise<void> {
  if (!keyword) {
    return
  }
  metricLoading.value = true
  try {
    const page = await metricApi.search(keyword, undefined, 20)
    metricOptions.value = page.list
  } finally {
    metricLoading.value = false
  }
}

function onMetricChange(row: IndicatorItem, value: string): void {
  const matched = metricOptions.value.find((metric) => metric.name === value)
  if (matched) {
    row.metricId = matched.id
    if (!row.unit) {
      row.unit = matched.unit
    }
  } else {
    row.metricId = undefined
  }
}

function addRow(): void {
  rows.push({ metricName: '', valueText: '', unit: '', refRangeText: '' })
}

function submit(): void {
  const valid = rows.filter((row) => row.metricName && row.metricName.trim())
  if (valid.length === 0) {
    ElMessage.warning('至少填写一项指标')
    return
  }
  if (props.showMeta && !meta.reportDate) {
    ElMessage.warning('请选择体检日期')
    return
  }
  emit('submit', {
    meta: props.showMeta ? { ...meta } : null,
    indicators: valid.map((row, index) => ({ ...row, metricName: row.metricName.trim(), sort: index + 1 })),
  })
}
</script>

<style scoped>
.meta-form {
  margin-bottom: 8px;
}

.indicator-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}

.indicator-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.metric-select {
  flex: 2;
}

.value-input {
  flex: 1;
}

.unit-input {
  width: 110px;
}

.ref-input {
  width: 140px;
}

.option-hint {
  float: right;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
