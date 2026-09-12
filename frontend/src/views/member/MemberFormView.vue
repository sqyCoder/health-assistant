<template>
  <div>
    <div class="page-header">
      <div class="header-left">
        <el-button link :icon="ArrowLeft" @click="router.back()">返回</el-button>
        <h2 class="page-title">{{ isEdit ? '编辑成员' : '新增成员' }}</h2>
      </div>
    </div>

    <div class="page-card form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" maxlength="50" placeholder="成员姓名" />
            </el-form-item>
            <el-form-item label="关系" prop="relation">
              <el-select v-model="form.relation" placeholder="与账号主人的关系" style="width: 100%">
                <el-option v-for="(label, value) in RELATION_LABELS" :key="value" :label="label" :value="value" />
              </el-select>
            </el-form-item>
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="出生日期">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="身高">
              <el-input-number v-model="form.heightCm" :min="0" :max="300" :precision="1" :step="0.5" style="width: 100%" />
            </el-form-item>
            <el-form-item label="体重">
              <el-input-number v-model="form.weightKg" :min="0" :max="500" :precision="1" :step="0.5" style="width: 100%" />
            </el-form-item>
            <el-form-item label="血型">
              <el-select v-model="form.bloodType" clearable placeholder="选填" style="width: 100%">
                <el-option label="A 型" value="A" />
                <el-option label="B 型" value="B" />
                <el-option label="AB 型" value="AB" />
                <el-option label="O 型" value="O" />
                <el-option label="不详" value="UNKNOWN" />
              </el-select>
            </el-form-item>
            <el-form-item label="电话">
              <el-input v-model="form.phone" maxlength="30" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" placeholder="选填" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { memberApi } from '@/api/member'
import { useMemberStore } from '@/stores/member'
import { RELATION_LABELS } from '@/utils/dict'
import type { MemberPayload } from '@/types'

const route = useRoute()
const router = useRouter()
const memberStore = useMemberStore()
const formRef = ref<FormInstance>()
const saving = ref(false)

const memberId = computed(() => (typeof route.params.id === 'string' ? route.params.id : null))
const isEdit = computed(() => Boolean(memberId.value))

const form = reactive<MemberPayload>({
  name: '',
  relation: '',
  gender: 1,
  birthDate: undefined,
  heightCm: undefined,
  weightKg: undefined,
  bloodType: undefined,
  phone: undefined,
  remark: undefined,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  relation: [{ required: true, message: '请选择与账号主人的关系', trigger: 'change' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
}

onMounted(async () => {
  if (memberId.value) {
    const member = await memberApi.get(memberId.value)
    Object.assign(form, {
      name: member.name,
      relation: member.relation,
      gender: member.gender,
      birthDate: member.birthDate,
      heightCm: member.heightCm,
      weightKg: member.weightKg,
      bloodType: member.bloodType,
      phone: member.phone,
      remark: member.remark,
      sort: member.sort,
    })
  }
})

async function submit(): Promise<void> {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value && memberId.value) {
      await memberApi.update(memberId.value, { ...form })
      ElMessage.success('已保存')
    } else {
      await memberApi.create({ ...form })
      ElMessage.success('新增成功')
    }
    await memberStore.loadMembers()
    router.push('/members')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-card {
  max-width: 860px;
}
</style>
