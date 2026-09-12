<template>
  <div class="auth-page">
    <div class="auth-brand">
      <div class="brand-particles" aria-hidden="true">
        <span
          v-for="(particle, index) in particles"
          :key="index"
          class="particle"
          :style="particle"
        />
      </div>
      <div class="brand-inner">
        <div class="brand-logo">
          <HeartPulse :size="22" />
        </div>
        <h1 class="brand-title">
          家庭健康，<br />
          <span class="ac-gradient-text">由 AI 读懂</span>
        </h1>
        <p class="brand-typewriter">
          <span>{{ typedText }}</span><span class="caret">▍</span>
        </p>
        <div class="brand-features">
          <span v-for="feature in features" :key="feature.label" class="brand-chip">
            <component :is="feature.icon" :size="13" />
            {{ feature.label }}
          </span>
        </div>
      </div>
    </div>

    <div class="auth-form-side">
      <div
        ref="cardRef"
        class="auth-card ac-glass"
        :style="tiltStyle"
        @mousemove="onTilt"
        @mouseleave="resetTilt"
      >
        <div class="card-head">
          <h2 class="card-title">欢迎回来</h2>
          <p class="card-sub">登录后继续管理家人的健康</p>
        </div>

        <form class="auth-form" @submit.prevent="submit">
          <label class="float-field" :class="{ active: focused === 'username' || form.username }">
            <span class="float-label">用户名</span>
            <input
              v-model.trim="form.username"
              type="text"
              autocomplete="username"
              @focus="focused = 'username'"
              @blur="focused = ''"
            />
          </label>
          <label class="float-field" :class="{ active: focused === 'password' || form.password }">
            <span class="float-label">密码</span>
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              @focus="focused = 'password'"
              @blur="focused = ''"
            />
            <button type="button" class="eye-btn" tabindex="-1" @click="showPassword = !showPassword">
              <EyeOff v-if="showPassword" :size="15" />
              <Eye v-else :size="15" />
            </button>
          </label>

          <Transition name="shake">
            <div v-if="errorMessage" class="form-error">
              <TriangleAlert :size="13" />
              {{ errorMessage }}
            </div>
          </Transition>

          <button type="submit" class="submit-btn" :class="buttonState" :disabled="buttonState === 'loading'">
            <template v-if="buttonState === 'idle'">登 录</template>
            <template v-else-if="buttonState === 'loading'">
              <Loader2 :size="16" class="spin" /> 正在登录
            </template>
            <template v-else>
              <Check :size="16" /> 登录成功
            </template>
          </button>
        </form>

        <div class="card-footer">
          还没有账号？
          <router-link to="/register" class="link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Check, Eye, EyeOff, HeartPulse, Loader2, MessageCircle, ScanLine, Sparkles, TriangleAlert } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const form = reactive({ username: '', password: '' })
const focused = ref('')
const showPassword = ref(false)
const errorMessage = ref('')
const buttonState = ref<'idle' | 'loading' | 'success'>('idle')
const cardRef = ref<HTMLElement>()
const tilt = ref({ rx: 0, ry: 0 })

const features = [
  { label: '报告 AI 解读', icon: ScanLine },
  { label: '个性化建议', icon: Sparkles },
  { label: '健康问答', icon: MessageCircle },
]

const phrases = [
  '上传体检报告，10 秒读懂指标',
  '一人一策的锻炼与康复建议',
  '和家人的健康数据对话',
  '血压、血糖、体重的长期趋势',
]
const typedText = ref('')
let phraseIndex = 0
let charIndex = 0
let deleting = false
let typeTimer: ReturnType<typeof setTimeout> | null = null

const particles = Array.from({ length: 16 }, () => {
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

const tiltStyle = computed(() => ({
  transform: `perspective(1000px) rotateX(${tilt.value.rx}deg) rotateY(${tilt.value.ry}deg)`,
}))

function onTilt(event: MouseEvent): void {
  if (!cardRef.value || document.body.classList.contains('ac-lite')) {
    return
  }
  const rect = cardRef.value.getBoundingClientRect()
  const px = (event.clientX - rect.left) / rect.width - 0.5
  const py = (event.clientY - rect.top) / rect.height - 0.5
  tilt.value = { rx: -py * 4, ry: px * 4 }
}

function resetTilt(): void {
  tilt.value = { rx: 0, ry: 0 }
}

function typeLoop(): void {
  const phrase = phrases[phraseIndex]
  if (!deleting) {
    typedText.value = phrase.slice(0, ++charIndex)
    if (charIndex === phrase.length) {
      deleting = true
      typeTimer = setTimeout(typeLoop, 2200)
      return
    }
  } else {
    typedText.value = phrase.slice(0, --charIndex)
    if (charIndex === 0) {
      deleting = false
      phraseIndex = (phraseIndex + 1) % phrases.length
    }
  }
  typeTimer = setTimeout(typeLoop, deleting ? 34 : 74)
}

async function submit(): Promise<void> {
  errorMessage.value = ''
  if (!form.username || !form.password) {
    errorMessage.value = '请输入用户名和密码'
    return
  }
  buttonState.value = 'loading'
  try {
    await userStore.login(form.username, form.password)
    buttonState.value = 'success'
    setTimeout(() => {
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
      router.push(redirect)
    }, 420)
  } catch (error) {
    buttonState.value = 'idle'
    errorMessage.value = error instanceof Error && error.message ? error.message : '用户名或密码错误'
    toast.error('登录失败', errorMessage.value)
  }
}

onMounted(() => typeTimer = setTimeout(typeLoop, 300))
onBeforeUnmount(() => {
  if (typeTimer) {
    clearTimeout(typeTimer)
  }
})
</script>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
}

.auth-brand {
  position: relative;
  flex: 1.15;
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
  background: color-mix(in srgb, var(--ac-primary) 60%, white);
  animation: particle-float linear infinite;
}

@keyframes particle-float {
  0% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-34px) scale(1.25);
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
  margin: 0 0 18px;
  font-size: 46px;
  line-height: 1.2;
  font-weight: 800;
  color: var(--ac-text-1);
  letter-spacing: -0.02em;
}

.brand-typewriter {
  min-height: 26px;
  margin: 0 0 30px;
  font-size: 15px;
  color: var(--ac-text-2);
}

.caret {
  margin-left: 2px;
  color: var(--ac-primary);
  animation: caret-blink 1s steps(2) infinite;
}

@keyframes caret-blink {
  50% {
    opacity: 0;
  }
}

.brand-features {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.brand-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 12.5px;
  color: var(--ac-text-2);
  background: color-mix(in srgb, var(--ac-surface-solid) 55%, transparent);
  border: 1px solid var(--ac-border);
  backdrop-filter: blur(10px);
}

.auth-form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
}

.auth-card {
  width: min(420px, 100%);
  padding: 36px 34px 28px;
  border-radius: 24px;
  transition: transform 0.16s ease-out;
  will-change: transform;
}

.card-head {
  margin-bottom: 26px;
}

.card-title {
  margin: 0;
  font-size: 24px;
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
  gap: 16px;
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

.eye-btn:hover {
  color: var(--ac-text-1);
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
  box-shadow: 0 18px 36px -16px color-mix(in srgb, var(--ac-primary) 90%, transparent);
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.submit-btn.loading {
  opacity: 0.85;
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
  margin-top: 20px;
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
