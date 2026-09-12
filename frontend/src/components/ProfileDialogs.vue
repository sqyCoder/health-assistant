<template>
  <el-dialog
    :model-value="profileVisible"
    title="个人资料"
    width="420px"
    @update:model-value="emit('update:profileVisible', $event)"
  >
    <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="80px">
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="profileForm.nickname" maxlength="50" />
      </el-form-item>
      <el-form-item label="头像地址" prop="avatarUrl">
        <el-input v-model="profileForm.avatarUrl" maxlength="500" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:profileVisible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog
    :model-value="passwordVisible"
    title="修改密码"
    width="420px"
    @update:model-value="emit('update:passwordVisible', $event)"
  >
    <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:passwordVisible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="savePassword">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  profileVisible: boolean
  passwordVisible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:profileVisible', value: boolean): void
  (e: 'update:passwordVisible', value: boolean): void
}>()

const userStore = useUserStore()
const saving = ref(false)
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

const profileForm = reactive({
  nickname: '',
  avatarUrl: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const profileRules: FormRules = {
  nickname: [{ required: true, message: '昵称不能为空', trigger: 'blur' }],
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度需为 8-32 位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码需同时包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

watch(
  () => props.profileVisible,
  (visible) => {
    if (visible) {
      profileForm.nickname = userStore.user?.nickname ?? ''
      profileForm.avatarUrl = userStore.user?.avatarUrl ?? ''
    }
  },
)

watch(
  () => props.passwordVisible,
  (visible) => {
    if (visible) {
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    }
  },
)

async function saveProfile(): Promise<void> {
  await profileFormRef.value?.validate()
  saving.value = true
  try {
    userStore.user = await authApi.updateProfile({
      nickname: profileForm.nickname,
      avatarUrl: profileForm.avatarUrl || undefined,
    })
    ElMessage.success('资料已更新')
    emit('update:profileVisible', false)
  } finally {
    saving.value = false
  }
}

async function savePassword(): Promise<void> {
  await passwordFormRef.value?.validate()
  saving.value = true
  try {
    await authApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码已修改，请重新登录')
    emit('update:passwordVisible', false)
    await userStore.logout()
    window.location.href = '/login'
  } finally {
    saving.value = false
  }
}
</script>
