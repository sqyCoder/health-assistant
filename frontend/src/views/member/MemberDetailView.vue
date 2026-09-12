<template>
  <div v-loading="loading">
    <div class="page-header">
      <div class="header-left">
        <el-button link :icon="ArrowLeft" @click="router.back()">返回</el-button>
        <h2 class="page-title">成员档案</h2>
      </div>
      <el-button v-if="memberId" type="primary" plain @click="router.push(`/members/${memberId}/edit`)">
        编辑基础信息
      </el-button>
    </div>

    <div v-if="profile" class="hero-card ac-glass">
      <div class="hero-avatar">{{ profile.member.name.charAt(0) }}</div>
      <div class="hero-info">
        <div class="hero-name-line">
          <span class="hero-name">{{ profile.member.name }}</span>
          <span class="hero-relation">{{ labelOf(RELATION_LABELS, profile.member.relation) }}</span>
        </div>
        <div class="hero-chips">
          <span class="hero-chip">{{ GENDER_LABELS[profile.member.gender] ?? '-' }}</span>
          <span class="hero-chip">{{ profile.member.age ?? '-' }} 岁</span>
          <span class="hero-chip">BMI <b class="ac-num">{{ profile.member.bmi ?? '-' }}</b></span>
          <span v-if="profile.member.heightCm" class="hero-chip">身高 <b class="ac-num">{{ profile.member.heightCm }}</b> cm</span>
          <span v-if="profile.member.weightKg" class="hero-chip">体重 <b class="ac-num">{{ profile.member.weightKg }}</b> kg</span>
          <span v-if="profile.member.bloodType" class="hero-chip">{{ profile.member.bloodType }} 型血</span>
          <span v-if="profile.member.birthDate" class="hero-chip">{{ profile.member.birthDate }}</span>
          <span v-if="profile.member.phone" class="hero-chip">{{ profile.member.phone }}</span>
        </div>
        <div v-if="profile.member.remark" class="hero-remark">{{ profile.member.remark }}</div>
      </div>
      <div class="hero-decor" />
    </div>

    <div class="page-card mt-16">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="既往病史" name="medical">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="openMedical(null)">新增病史</el-button>
          </div>
          <el-table :data="profile?.medicalHistories ?? []" empty-text="暂无病史记录">
            <el-table-column prop="diseaseName" label="疾病名称" min-width="130" />
            <el-table-column prop="diagnosedDate" label="确诊日期" width="110" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.diseaseStatus === 'TREATING' ? 'warning' : row.diseaseStatus === 'MANAGING' ? 'danger' : 'success'">
                  {{ labelOf(DISEASE_STATUS_LABELS, row.diseaseStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="treatmentDesc" label="治疗情况" min-width="150" show-overflow-tooltip />
            <el-table-column prop="hospital" label="医院" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openMedical(row as MedicalHistory)">编辑</el-button>
                <el-button link type="danger" @click="removeMedical(row as MedicalHistory)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="过敏史" name="allergy">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="openAllergy(null)">新增过敏</el-button>
          </div>
          <el-table :data="profile?.allergies ?? []" empty-text="暂无过敏记录">
            <el-table-column prop="allergen" label="过敏原" min-width="130" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ labelOf(ALLERGY_TYPE_LABELS, row.allergyType) }}</template>
            </el-table-column>
            <el-table-column prop="reaction" label="过敏反应" min-width="150" show-overflow-tooltip />
            <el-table-column label="严重程度" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.severity === 'SEVERE' ? 'danger' : row.severity === 'MODERATE' ? 'warning' : 'info'">
                  {{ labelOf(SEVERITY_LABELS, row.severity) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAllergy(row as Allergy)">编辑</el-button>
                <el-button link type="danger" @click="removeAllergy(row as Allergy)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="长期用药" name="medication">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="openMedication(null)">新增用药</el-button>
          </div>
          <el-table :data="profile?.medications ?? []" empty-text="暂无用药记录">
            <el-table-column prop="drugName" label="药品名称" min-width="130" />
            <el-table-column prop="dose" label="剂量" width="90" />
            <el-table-column prop="frequency" label="频次" width="100" />
            <el-table-column label="途径" width="80">
              <template #default="{ row }">{{ labelOf(MEDICATION_ROUTE_LABELS, row.route) }}</template>
            </el-table-column>
            <el-table-column prop="startDate" label="开始日期" width="110" />
            <el-table-column prop="purpose" label="用途" min-width="110" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'TAKING' ? 'success' : 'info'">
                  {{ labelOf(MEDICATION_STATUS_LABELS, row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openMedication(row as Medication)">编辑</el-button>
                <el-button link type="danger" @click="removeMedication(row as Medication)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="家族史" name="family">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="openFamily(null)">新增家族史</el-button>
          </div>
          <el-table :data="profile?.familyHistories ?? []" empty-text="暂无家族史记录">
            <el-table-column label="亲属关系" width="130">
              <template #default="{ row }">{{ labelOf(FAMILY_RELATION_LABELS, row.relation) }}</template>
            </el-table-column>
            <el-table-column prop="diseaseName" label="疾病名称" min-width="140" />
            <el-table-column prop="onsetAge" label="发病年龄" width="100" />
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openFamily(row as FamilyHistory)">编辑</el-button>
                <el-button link type="danger" @click="removeFamily(row as FamilyHistory)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="生活习惯" name="lifestyle">
          <el-form :model="lifestyleForm" label-width="110px" class="lifestyle-form">
            <el-row :gutter="16">
              <el-col :xs="24" :sm="12">
                <el-form-item label="吸烟情况">
                  <el-select v-model="lifestyleForm.smoking" clearable placeholder="选填" style="width: 100%">
                    <el-option v-for="(label, value) in SMOKING_LABELS" :key="value" :label="label" :value="value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="吸烟量">
                  <el-input v-model="lifestyleForm.smokingAmount" maxlength="50" placeholder="如：每日 10 支" />
                </el-form-item>
                <el-form-item label="饮酒情况">
                  <el-select v-model="lifestyleForm.drinking" clearable placeholder="选填" style="width: 100%">
                    <el-option v-for="(label, value) in DRINKING_LABELS" :key="value" :label="label" :value="value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="饮酒量">
                  <el-input v-model="lifestyleForm.drinkingAmount" maxlength="50" placeholder="如：每周 3 次啤酒" />
                </el-form-item>
                <el-form-item label="平均睡眠(h)">
                  <el-input-number v-model="lifestyleForm.sleepHours" :min="0" :max="24" :precision="1" :step="0.5" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="睡眠质量">
                  <el-select v-model="lifestyleForm.sleepQuality" clearable placeholder="选填" style="width: 100%">
                    <el-option v-for="(label, value) in SLEEP_QUALITY_LABELS" :key="value" :label="label" :value="value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="运动频率">
                  <el-input v-model="lifestyleForm.exerciseFrequency" maxlength="50" placeholder="如：每周 3 次" />
                </el-form-item>
                <el-form-item label="日均久坐(h)">
                  <el-input-number v-model="lifestyleForm.sedentaryHours" :min="0" :max="24" :precision="1" :step="0.5" style="width: 100%" />
                </el-form-item>
                <el-form-item label="压力水平">
                  <el-select v-model="lifestyleForm.stressLevel" clearable placeholder="选填" style="width: 100%">
                    <el-option v-for="(label, value) in STRESS_LEVEL_LABELS" :key="value" :label="label" :value="value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="饮食习惯">
                  <el-input v-model="lifestyleForm.dietHabit" maxlength="255" placeholder="如：偏咸、爱吃肉" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" :loading="savingLifestyle" @click="saveLifestyle">保存生活习惯</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="medicalDialog.visible" :title="medicalDialog.editingId ? '编辑病史' : '新增病史'" width="520px">
      <el-form ref="medicalFormRef" :model="medicalDialog.form" :rules="medicalRules" label-width="90px">
        <el-form-item label="疾病名称" prop="diseaseName">
          <el-input v-model="medicalDialog.form.diseaseName" maxlength="100" />
        </el-form-item>
        <el-form-item label="确诊日期">
          <el-date-picker v-model="medicalDialog.form.diagnosedDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="疾病状态" prop="diseaseStatus">
          <el-select v-model="medicalDialog.form.diseaseStatus" style="width: 100%">
            <el-option v-for="(label, value) in DISEASE_STATUS_LABELS" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗情况">
          <el-input v-model="medicalDialog.form.treatmentDesc" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="医院">
          <el-input v-model="medicalDialog.form.hospital" maxlength="100" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="medicalDialog.form.remark" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="medicalDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="savingDialog" @click="saveMedical">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="allergyDialog.visible" :title="allergyDialog.editingId ? '编辑过敏史' : '新增过敏史'" width="520px">
      <el-form ref="allergyFormRef" :model="allergyDialog.form" :rules="allergyRules" label-width="90px">
        <el-form-item label="过敏原" prop="allergen">
          <el-input v-model="allergyDialog.form.allergen" maxlength="100" placeholder="如：青霉素、海鲜" />
        </el-form-item>
        <el-form-item label="类型" prop="allergyType">
          <el-select v-model="allergyDialog.form.allergyType" style="width: 100%">
            <el-option v-for="(label, value) in ALLERGY_TYPE_LABELS" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="过敏反应">
          <el-input v-model="allergyDialog.form.reaction" maxlength="200" placeholder="如：皮疹、呼吸困难" />
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="allergyDialog.form.severity" clearable style="width: 100%">
            <el-option v-for="(label, value) in SEVERITY_LABELS" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="allergyDialog.form.remark" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="allergyDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="savingDialog" @click="saveAllergy">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="medicationDialog.visible" :title="medicationDialog.editingId ? '编辑用药' : '新增用药'" width="560px">
      <el-form ref="medicationFormRef" :model="medicationDialog.form" :rules="medicationRules" label-width="90px">
        <el-form-item label="药品名称" prop="drugName">
          <el-input v-model="medicationDialog.form.drugName" maxlength="100" />
        </el-form-item>
        <el-form-item label="剂量">
          <el-input v-model="medicationDialog.form.dose" maxlength="50" placeholder="如：5mg" />
        </el-form-item>
        <el-form-item label="频次">
          <el-input v-model="medicationDialog.form.frequency" maxlength="50" placeholder="如：每日 1 次" />
        </el-form-item>
        <el-form-item label="用药途径">
          <el-select v-model="medicationDialog.form.route" clearable style="width: 100%">
            <el-option v-for="(label, value) in MEDICATION_ROUTE_LABELS" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="medicationDialog.form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="medicationDialog.form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="用途">
          <el-input v-model="medicationDialog.form.purpose" maxlength="100" placeholder="如：降压" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="medicationDialog.form.status" style="width: 100%">
            <el-option v-for="(label, value) in MEDICATION_STATUS_LABELS" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="医嘱">
          <el-input v-model="medicationDialog.form.doctorAdvice" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="medicationDialog.form.remark" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="medicationDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="savingDialog" @click="saveMedication">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="familyDialog.visible" :title="familyDialog.editingId ? '编辑家族史' : '新增家族史'" width="480px">
      <el-form ref="familyFormRef" :model="familyDialog.form" :rules="familyRules" label-width="90px">
        <el-form-item label="亲属关系" prop="relation">
          <el-select v-model="familyDialog.form.relation" style="width: 100%">
            <el-option v-for="(label, value) in FAMILY_RELATION_LABELS" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="疾病名称" prop="diseaseName">
          <el-input v-model="familyDialog.form.diseaseName" maxlength="100" />
        </el-form-item>
        <el-form-item label="发病年龄">
          <el-input-number v-model="familyDialog.form.onsetAge" :min="0" :max="150" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="familyDialog.form.remark" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="familyDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="savingDialog" @click="saveFamily">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { memberApi } from '@/api/member'
import {
  ALLERGY_TYPE_LABELS,
  DISEASE_STATUS_LABELS,
  DRINKING_LABELS,
  FAMILY_RELATION_LABELS,
  GENDER_LABELS,
  MEDICATION_ROUTE_LABELS,
  MEDICATION_STATUS_LABELS,
  RELATION_LABELS,
  SEVERITY_LABELS,
  SLEEP_QUALITY_LABELS,
  SMOKING_LABELS,
  STRESS_LEVEL_LABELS,
  labelOf,
} from '@/utils/dict'
import type {
  Allergy,
  AllergyPayload,
  FamilyHistory,
  FamilyHistoryPayload,
  Lifestyle,
  MedicalHistory,
  MedicalHistoryPayload,
  Medication,
  MedicationPayload,
  MemberProfile,
} from '@/types'

const route = useRoute()
const router = useRouter()
const memberId = computed(() => (typeof route.params.id === 'string' ? route.params.id : null))

const loading = ref(false)
const savingDialog = ref(false)
const savingLifestyle = ref(false)
const activeTab = ref('medical')
const profile = ref<MemberProfile | null>(null)

const medicalFormRef = ref<FormInstance>()
const allergyFormRef = ref<FormInstance>()
const medicationFormRef = ref<FormInstance>()
const familyFormRef = ref<FormInstance>()

const medicalDialog = reactive({
  visible: false,
  editingId: null as string | null,
  form: { diseaseName: '', diagnosedDate: undefined as string | undefined, diseaseStatus: 'MANAGING', treatmentDesc: '', hospital: '', remark: '' } as MedicalHistoryPayload,
})
const allergyDialog = reactive({
  visible: false,
  editingId: null as string | null,
  form: { allergen: '', allergyType: 'DRUG', reaction: '', severity: '', remark: '' } as AllergyPayload,
})
const medicationDialog = reactive({
  visible: false,
  editingId: null as string | null,
  form: { drugName: '', dose: '', frequency: '', route: '', startDate: undefined as string | undefined, endDate: undefined as string | undefined, purpose: '', status: 'TAKING', doctorAdvice: '', remark: '' } as MedicationPayload,
})
const familyDialog = reactive({
  visible: false,
  editingId: null as string | null,
  form: { relation: 'FATHER', diseaseName: '', onsetAge: undefined as number | undefined, remark: '' } as FamilyHistoryPayload,
})

const lifestyleForm = reactive<Lifestyle>({})

const medicalRules: FormRules = {
  diseaseName: [{ required: true, message: '请输入疾病名称', trigger: 'blur' }],
  diseaseStatus: [{ required: true, message: '请选择疾病状态', trigger: 'change' }],
}
const allergyRules: FormRules = {
  allergen: [{ required: true, message: '请输入过敏原', trigger: 'blur' }],
  allergyType: [{ required: true, message: '请选择过敏类型', trigger: 'change' }],
}
const medicationRules: FormRules = {
  drugName: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
}
const familyRules: FormRules = {
  relation: [{ required: true, message: '请选择亲属关系', trigger: 'change' }],
  diseaseName: [{ required: true, message: '请输入疾病名称', trigger: 'blur' }],
}

onMounted(loadProfile)

async function loadProfile(): Promise<void> {
  if (!memberId.value) {
    return
  }
  loading.value = true
  try {
    profile.value = await memberApi.profile(memberId.value)
    Object.assign(lifestyleForm, profile.value.lifestyle ?? {})
  } finally {
    loading.value = false
  }
}

function openMedical(record: MedicalHistory | null): void {
  medicalDialog.editingId = record?.id ?? null
  medicalDialog.form = record
    ? { diseaseName: record.diseaseName, diagnosedDate: record.diagnosedDate, diseaseStatus: record.diseaseStatus, treatmentDesc: record.treatmentDesc, hospital: record.hospital, remark: record.remark }
    : { diseaseName: '', diagnosedDate: undefined, diseaseStatus: 'MANAGING', treatmentDesc: '', hospital: '', remark: '' }
  medicalDialog.visible = true
}

async function saveMedical(): Promise<void> {
  await medicalFormRef.value?.validate()
  if (!memberId.value) {
    return
  }
  savingDialog.value = true
  try {
    if (medicalDialog.editingId) {
      await memberApi.updateMedicalHistory(memberId.value, medicalDialog.editingId, medicalDialog.form)
    } else {
      await memberApi.addMedicalHistory(memberId.value, medicalDialog.form)
    }
    ElMessage.success('已保存')
    medicalDialog.visible = false
    await loadProfile()
  } finally {
    savingDialog.value = false
  }
}

async function removeMedical(record: MedicalHistory): Promise<void> {
  await ElMessageBox.confirm(`确定删除「${record.diseaseName}」吗？`, '提示', { type: 'warning' })
  if (memberId.value) {
    await memberApi.removeMedicalHistory(memberId.value, record.id)
    ElMessage.success('已删除')
    await loadProfile()
  }
}

function openAllergy(record: Allergy | null): void {
  allergyDialog.editingId = record?.id ?? null
  allergyDialog.form = record
    ? { allergen: record.allergen, allergyType: record.allergyType, reaction: record.reaction, severity: record.severity, remark: record.remark }
    : { allergen: '', allergyType: 'DRUG', reaction: '', severity: '', remark: '' }
  allergyDialog.visible = true
}

async function saveAllergy(): Promise<void> {
  await allergyFormRef.value?.validate()
  if (!memberId.value) {
    return
  }
  savingDialog.value = true
  try {
    if (allergyDialog.editingId) {
      await memberApi.updateAllergy(memberId.value, allergyDialog.editingId, allergyDialog.form)
    } else {
      await memberApi.addAllergy(memberId.value, allergyDialog.form)
    }
    ElMessage.success('已保存')
    allergyDialog.visible = false
    await loadProfile()
  } finally {
    savingDialog.value = false
  }
}

async function removeAllergy(record: Allergy): Promise<void> {
  await ElMessageBox.confirm(`确定删除「${record.allergen}」过敏记录吗？`, '提示', { type: 'warning' })
  if (memberId.value) {
    await memberApi.removeAllergy(memberId.value, record.id)
    ElMessage.success('已删除')
    await loadProfile()
  }
}

function openMedication(record: Medication | null): void {
  medicationDialog.editingId = record?.id ?? null
  medicationDialog.form = record
    ? { drugName: record.drugName, dose: record.dose, frequency: record.frequency, route: record.route, startDate: record.startDate, endDate: record.endDate, purpose: record.purpose, status: record.status ?? 'TAKING', doctorAdvice: record.doctorAdvice, remark: record.remark }
    : { drugName: '', dose: '', frequency: '', route: '', startDate: undefined, endDate: undefined, purpose: '', status: 'TAKING', doctorAdvice: '', remark: '' }
  medicationDialog.visible = true
}

async function saveMedication(): Promise<void> {
  await medicationFormRef.value?.validate()
  if (!memberId.value) {
    return
  }
  savingDialog.value = true
  try {
    if (medicationDialog.editingId) {
      await memberApi.updateMedication(memberId.value, medicationDialog.editingId, medicationDialog.form)
    } else {
      await memberApi.addMedication(memberId.value, medicationDialog.form)
    }
    ElMessage.success('已保存')
    medicationDialog.visible = false
    await loadProfile()
  } finally {
    savingDialog.value = false
  }
}

async function removeMedication(record: Medication): Promise<void> {
  await ElMessageBox.confirm(`确定删除「${record.drugName}」用药记录吗？`, '提示', { type: 'warning' })
  if (memberId.value) {
    await memberApi.removeMedication(memberId.value, record.id)
    ElMessage.success('已删除')
    await loadProfile()
  }
}

function openFamily(record: FamilyHistory | null): void {
  familyDialog.editingId = record?.id ?? null
  familyDialog.form = record
    ? { relation: record.relation, diseaseName: record.diseaseName, onsetAge: record.onsetAge, remark: record.remark }
    : { relation: 'FATHER', diseaseName: '', onsetAge: undefined, remark: '' }
  familyDialog.visible = true
}

async function saveFamily(): Promise<void> {
  await familyFormRef.value?.validate()
  if (!memberId.value) {
    return
  }
  savingDialog.value = true
  try {
    if (familyDialog.editingId) {
      await memberApi.updateFamilyHistory(memberId.value, familyDialog.editingId, familyDialog.form)
    } else {
      await memberApi.addFamilyHistory(memberId.value, familyDialog.form)
    }
    ElMessage.success('已保存')
    familyDialog.visible = false
    await loadProfile()
  } finally {
    savingDialog.value = false
  }
}

async function removeFamily(record: FamilyHistory): Promise<void> {
  await ElMessageBox.confirm(`确定删除「${record.diseaseName}」家族史记录吗？`, '提示', { type: 'warning' })
  if (memberId.value) {
    await memberApi.removeFamilyHistory(memberId.value, record.id)
    ElMessage.success('已删除')
    await loadProfile()
  }
}

async function saveLifestyle(): Promise<void> {
  if (!memberId.value) {
    return
  }
  savingLifestyle.value = true
  try {
    const saved = await memberApi.saveLifestyle(memberId.value, lifestyleForm)
    Object.assign(lifestyleForm, saved)
    ElMessage.success('生活习惯已保存')
  } finally {
    savingLifestyle.value = false
  }
}
</script>

<style scoped>
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px 24px;
  border-radius: 22px;
  overflow: hidden;
}

.hero-avatar {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 72px;
  height: 72px;
  flex: none;
  border-radius: 22px;
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  background-image: var(--ac-gradient-primary);
  box-shadow: var(--ac-shadow-glow);
}

.hero-info {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.hero-name-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--ac-text-1);
}

.hero-relation {
  padding: 3px 11px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
}

.hero-chips {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 11px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--ac-text-2);
  background: color-mix(in srgb, var(--ac-text-1) 6%, transparent);
  border: 1px solid var(--ac-border);
}

.hero-chip b {
  color: var(--ac-text-1);
}

.hero-remark {
  margin-top: 10px;
  font-size: 12.5px;
  color: var(--ac-text-3);
}

.hero-decor {
  position: absolute;
  right: -60px;
  top: -80px;
  width: 220px;
  height: 220px;
  border-radius: 999px;
  background: conic-gradient(
    from 0deg,
    color-mix(in srgb, var(--ac-primary) 24%, transparent),
    color-mix(in srgb, var(--ac-violet) 18%, transparent),
    color-mix(in srgb, var(--ac-cyan) 20%, transparent),
    color-mix(in srgb, var(--ac-primary) 24%, transparent)
  );
  filter: blur(42px);
  opacity: 0.6;
  animation: hero-decor-spin 30s linear infinite;
}

@keyframes hero-decor-spin {
  to {
    transform: rotate(360deg);
  }
}

body.ac-lite .hero-decor {
  animation: none;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-name {
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile-sub {
  margin-top: 6px;
  font-size: 13px;
}

.tab-toolbar {
  margin-bottom: 12px;
}

.lifestyle-form {
  max-width: 860px;
}
</style>
