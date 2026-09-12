<template>
  <div class="auth-page">
    <div class="auth-brand">
      <div class="brand-particles" aria-hidden="true">
        <span v-for="(particle, index) in particles" :key="index" class="particle" :style="particle" />
      </div>
      <div class="brand-inner">
        <div class="brand-logo">
          <HeartPulse :size="22" />
        </div>
        <h1 class="brand-title">
          注册账号，<br />
          <span class="ac-gradient-text">全家健康一张图</span>
        </h1>
        <ul class="brand-points">
          <li v-for="point in points" :key="point">
            <Check :size="14" />
            {{ point }}
          </li>
        </ul>
      </div>
    </div>

    <div class="auth-form-side">
      <div class="auth-card ac-glass">
        <div class="card-head">
          <h2 class="card-title">创建账号</h2>
          <p class="card-sub">一个账号即可管理全家的健康档案</p>
        </div>

        <form class="auth-form" @submit.prevent="submit">
          <label class="float-field" :class="{ active: focused === 'username' || form.username }">
            <span class="float-label">用户名（4-20 位字母、数字或下划线）</span>
            <input v-model.trim="form.username" type="text" autocomplete="username" @focus="focused = 'username'" @blur="focused = ''" />
          </label>
          <label class="float-field" :class="{ active: focused === 'nickname' || form.nickname }">
            <span class="float-label">昵称</span>
            <input v-model.trim="form.nickname" type="text" @focus="focused = 'nickname'" @blur="focused = ''" />
          </label>
          <label class="float-field" :class="{ active: focused === 'password' || form.password }">
            <span class="float-label">密码（8-32 位，含字母和数字）</span>
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              @focus="focused = 'password'"
              @blur="focused = ''"
            />
            <button type="button" class="eye-btn" tabindex="-1" @click="showPassword = !showPassword">
              <EyeOff v-if="showPassword" :size="15" />
              <Eye v-else :size="15" />
            </button>
          </label>
          <div class="strength">
            <span v-for="index in 4" :key="index" class="strength-bar" :class="index <= strength.score ? `level-${strength.score}` : ''" />
            <span class="strength-text">{{ strength.label }}</span>
          </div>
          <label class="float-field" :class="{ active: focused === 'confirm' || form.confirmPassword }">
            <span class="float-label">确认密码</span>
            <input
              v-model="form.confirmPassword"
              type="password"
              autocomplete="new-password"
              @focus="focused = 'confirm'"
              @blur="focused = ''"
            />
          </label>

          <Transition name="shake">
            <div v-if="errorMessage" class="form-error">
              <TriangleAlert :size="13" />
              {{ errorMessage }}
            </div>
          </Transition>

          <button type="submit" class="submit-btn" :class="buttonState" :disabled="buttonState === 'loading'">
            <template v-if="buttonState === 'idle'">注 册</template>
            <template v-else-if="buttonState === 'loading'">
              <Loader2 :size="16" class="spin" /> 正在创建
            </template>
            <template v-else>
              <Check :size="16" /> 创建成功
            </template>
          </button>
        </form>

        <div class="card-footer">
          已有账号？
          <router-link to="/login" class="link">直接登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Check, Eye, EyeOff, HeartPulse, Loader2, TriangleAlert } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})
const focused = ref('')
const showPassword = ref(false)
const errorMessage = ref('')
const buttonState = ref<'idle' | 'loading' | 'success'>('idle')

const points = [
  '完整健康档案（病史/过敏/用药/家族史）',
  '体检报告 AI 自动解析与解读',
  '个性化锻炼与康复建议',
  '基于家人历史数据的健康问答',
]

const particles = Array.from({ length: 14 }, () => {
  const size = 3 + Math.random() * 5
  return {
    left: `${Math.random() * 100}%`,
    top: `${Math.random() * 100}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${Math.random() * 8}s`,
    animationDuration: `${7 + Math.random() * 9}s`,
    opacity: String(0.25 + Math.random() * 0.4),
  }
})

const strength = computed(() => {
  const password = form.password
  let score = 0
  if (password.length >= 8) score += 1
  if (/[A-Za-z]/.test(password) && /\d/.test(password)) score += 1
  if (password.length >= 12) score += 1
  if (/[^A-Za-z0-9]/.test(password)) score += 1
  const labels = ['太短', '较弱', '一般', '较强', '很强']
  return { score, label: password ? labels[score] : '密码强度' }
})

async function submit(): Promise<void> {
  errorMessage.value = ''
  if (!/^[a-zA-Z0-9_]{4,20}$/.test(form.username)) {
    errorMessage.value = '用户名需为 4-20 位字母、数字或下划线'
    return
  }
  if (!form.nickname) {
    errorMessage.value = '请输入昵称'
    return
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,32}$/.test(form.password)) {
    errorMessage.value = '密码需 8-32 位且同时包含字母和数字'
    return
  }
  if (form.password !== form.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }
  buttonState.value = 'loading'
  try {
    await userStore.register(form.username, form.password, form.nickname)
    buttonState.value = 'success'
    setTimeout(() => router.push('/'), 420)
  } catch (error) {
    buttonState.value = 'idle'
    errorMessage.value = error instanceof Error && error.message ? error.message : '注册失败，请重试'
    toast.error('注册失败', errorMessage.value)
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
}

.auth-brand {
  position: relative;
  flex: 1.1;
  display: none;
  align-items: center;
  overflow: hidden;
  padding: 64px;
}

@media (min-width: 1024px) {
  .auth-brand {
    display: flex;
  }
}

.brand-particles {
  position: absolute;
  inset: 0;
}

.particle {
  position: absolute;
  border-radius: 999px;
  background: color-mix(in srgb, var(--ac-violet) 55%, white);
  animation: particle-float linear infinite;
}

@keyframes particle-float {
  0% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-32px) scale(1.22);
  }
  100% {
    transform: translateY(0) scale(1);
  }
}

body.ac-lite .particle {
  animation: none;
}

.brand-inner {
  position: relative;
  z-index: 1;
  max-width: 520px;
}

.brand-logo {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  margin-bottom: 28px;
  border-radius: 18px;
  color: #fff;
  background-image: var(--ac-gradient-primary);
  box-shadow: var(--ac-shadow-glow);
}

.brand-title {
  margin: 0 0 24px;
  font-size: 42px;
  line-height: 1.25;
  font-weight: 800;
  color: var(--ac-text-1);
  letter-spacing: -0.02em;
}

.brand-points {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.brand-points li {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 14px;
  color: var(--ac-text-2);
}

.brand-points svg {
  color: var(--ac-mint);
}

.auth-form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
}

.auth-card {
  width: min(430px, 100%);
  padding: 34px 32px 26px;
  border-radius: 24px;
}

.card-head {
  margin-bottom: 22px;
}

.card-title {
  margin: 0;
  font-size: 23px;
  font-weight: 700;
  color: var(--ac-text-1);
}

.card-sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--ac-text-3);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.float-field {
  position: relative;
  display: block;
  border: 1px solid var(--ac-border);
  border-radius: 14px;
  background: color-mix(in srgb, var(--ac-surface-solid) 70%, transparent);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.float-field:focus-within {
  border-color: color-mix(in srgb, var(--ac-primary) 55%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ac-primary) 16%, transparent);
}

.float-field input {
  width: 100%;
  height: 54px;
  padding: 16px 44px 0 14px;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: var(--ac-text-1);
}

.float-label {
  position: absolute;
  left: 14px;
  top: 17px;
  font-size: 13.5px;
  color: var(--ac-text-3);
  pointer-events: none;
  transition: transform 0.2s cubic-bezier(0.2, 0, 0, 1), font-size 0.2s ease, color 0.2s ease;
}

.float-field.active .float-label {
  transform: translateY(-9px);
  font-size: 11px;
  color: var(--ac-primary);
}

.eye-btn {
  position: absolute;
  right: 10px;
  top: 15px;
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--ac-text-3);
  cursor: pointer;
}

.strength {
  display: flex;
  align-items: center;
  gap: 6px;
}

.strength-bar {
  flex: 1;
  height: 5px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--ac-text-1) 8%, transparent);
  transition: background-color 0.24s ease;
}

.strength-bar.level-1 {
  background: var(--ac-coral);
}

.strength-bar.level-2 {
  background: var(--ac-amber);
}

.strength-bar.level-3 {
  background: var(--ac-cyan);
}

.strength-bar.level-4 {
  background: var(--ac-mint);
}

.strength-text {
  width: 56px;
  text-align: right;
  font-size: 11px;
  color: var(--ac-text-3);
}

.form-error {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--ac-coral);
}

.shake-enter-active {
  animation: shake-x 0.4s ease;
}

@keyframes shake-x {
  0%,
  100% {
    transform: translateX(0);
  }
  20% {
    transform: translateX(-6px);
  }
  40% {
    transform: translateX(6px);
  }
  60% {
    transform: translateX(-4px);
  }
  80% {
    transform: translateX(4px);
  }
}

.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 46px;
  border: none;
  border-radius: 13px;
  font-size: 14.5px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  background-image: var(--ac-gradient-primary);
  box-shadow: 0 14px 30px -14px color-mix(in srgb, var(--ac-primary) 85%, transparent);
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.24s ease, opacity 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.submit-btn.success {
  background-image: var(--ac-gradient-health);
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.card-footer {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: var(--ac-text-3);
}

.link {
  color: var(--ac-primary);
  font-weight: 600;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}
</style>
