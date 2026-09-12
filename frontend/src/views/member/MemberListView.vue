<template>
  <div>
    <div class="page-head">
      <div>
        <h2 class="page-title">家庭成员</h2>
        <p class="page-sub">一个账号管理全家的健康档案</p>
      </div>
      <button type="button" class="primary-action" @click="router.push('/members/new')">
        <Plus :size="16" />
        新增成员
      </button>
    </div>

    <div v-if="loading" class="member-grid">
      <div v-for="index in 3" :key="index" class="member-skeleton ac-glass">
        <div class="ac-shimmer skeleton-avatar" />
        <div class="ac-shimmer skeleton-line" />
        <div class="ac-shimmer skeleton-line short" />
      </div>
    </div>

    <AcEmptyState
      v-else-if="memberStore.members.length === 0"
      art="none"
      title="还没有家庭成员"
      description="添加成员后，就可以为他建立健康档案、上传体检报告并生成个性化建议。"
    >
      <template #action>
        <button type="button" class="primary-action" @click="router.push('/members/new')">
          <Plus :size="16" />
          添加第一位成员
        </button>
      </template>
    </AcEmptyState>

    <div v-else v-auto-animate class="member-grid">
      <div
        v-for="member in memberStore.members"
        :key="member.id"
        class="member-card ac-glass"
        :class="{ current: member.id === memberStore.currentMemberId }"
        @click="openDetail(member)"
      >
        <div class="card-banner" :style="bannerStyle(member)">
          <span class="card-relation">{{ labelOf(RELATION_LABELS, member.relation) }}</span>
          <span v-if="member.id === memberStore.currentMemberId" class="card-current">
            <Star :size="12" /> 当前
          </span>
        </div>
        <div class="card-body">
          <div class="card-avatar" :style="bannerStyle(member)">{{ member.name.charAt(0) }}</div>
          <div class="card-name">{{ member.name }}</div>
          <div class="card-meta">
            {{ GENDER_LABELS[member.gender] ?? '-' }} · {{ member.age ?? '-' }} 岁
            <template v-if="member.bloodType"> · {{ member.bloodType }} 型</template>
          </div>
          <div class="card-metrics">
            <span class="metric-chip">
              BMI
              <b class="ac-num">{{ member.bmi ?? '-' }}</b>
            </span>
            <span v-if="member.heightCm" class="metric-chip">
              身高
              <b class="ac-num">{{ member.heightCm }}</b>
            </span>
            <span v-if="member.weightKg" class="metric-chip">
              体重
              <b class="ac-num">{{ member.weightKg }}</b>
            </span>
          </div>
        </div>
        <div class="card-actions">
          <button type="button" class="action-btn" @click.stop="openDetail(member)">
            <FolderOpen :size="14" /> 档案
          </button>
          <button type="button" class="action-btn" @click.stop="router.push(`/members/${member.id}/edit`)">
            <Pencil :size="14" /> 编辑
          </button>
          <button type="button" class="action-btn danger" @click.stop="removeMember(member)">
            <Trash2 :size="14" /> 删除
          </button>
        </div>
      </div>

      <button type="button" class="add-card" @click="router.push('/members/new')">
        <span class="add-icon">
          <Plus :size="22" />
        </span>
        <span class="add-text">新增成员</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { FolderOpen, Pencil, Plus, Star, Trash2 } from 'lucide-vue-next'
import { vAutoAnimate } from '@formkit/auto-animate/vue'
import AcEmptyState from '@/components/ui/AcEmptyState.vue'
import { memberApi } from '@/api/member'
import { useMemberStore } from '@/stores/member'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'
import { GENDER_LABELS, RELATION_LABELS, labelOf } from '@/utils/dict'
import type { Member } from '@/types'

const router = useRouter()
const memberStore = useMemberStore()
const confirm = useConfirm()
const toast = useToast()
const loading = ref(false)

const HUES = [232, 258, 190, 160, 24, 340, 285, 205]

function bannerStyle(member: Member): Record<string, string> {
  const hue = HUES[hashCode(member.id) % HUES.length]
  return {
    backgroundImage: `linear-gradient(135deg, hsl(${hue} 80% 58%), hsl(${hue + 34} 76% 52%))`,
  }
}

function hashCode(value: string): number {
  let hash = 0
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
}

function openDetail(member: Member): void {
  memberStore.setCurrent(member.id)
  router.push(`/members/${member.id}`)
}

async function removeMember(member: Member): Promise<void> {
  const confirmed = await confirm.danger(
    `删除成员「${member.name}」？`,
    '其全部健康档案（病史、报告、运动、对话）将一并删除，且无法恢复。',
  )
  if (!confirmed) {
    return
  }
  await memberApi.remove(member.id)
  toast.success('已删除成员', member.name)
  await memberStore.loadMembers()
}

async function load(): Promise<void> {
  loading.value = true
  try {
    await memberStore.loadMembers()
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => memberStore.currentMemberId, () => {
  // keep list in sync when switching members elsewhere
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--ac-text-1);
}

.page-sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--ac-text-3);
}

.primary-action {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 40px;
  padding: 0 18px;
  border: none;
  border-radius: 12px;
  font-size: 13.5px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  background-image: var(--ac-gradient-primary);
  box-shadow: 0 12px 26px -14px color-mix(in srgb, var(--ac-primary) 80%, transparent);
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.24s ease;
}

.primary-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px -14px color-mix(in srgb, var(--ac-primary) 88%, transparent);
}

.member-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.member-card {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.28s cubic-bezier(0.2, 0, 0, 1), border-color 0.24s ease, box-shadow 0.24s ease;
}

.member-card:hover {
  transform: translateY(-4px);
  border-color: var(--ac-border-strong);
  box-shadow: var(--ac-shadow-3);
}

.member-card.current {
  border-color: color-mix(in srgb, var(--ac-primary) 45%, transparent);
  box-shadow: var(--ac-shadow-glow);
}

.card-banner {
  height: 62px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 10px 12px;
}

.card-relation {
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11px;
  color: #fff;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(6px);
}

.card-current {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11px;
  color: #fff;
  background: rgba(0, 0, 0, 0.24);
}

.card-body {
  position: relative;
  padding: 30px 16px 14px;
}

.card-avatar {
  position: absolute;
  top: -26px;
  left: 16px;
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  border: 3px solid var(--ac-surface-solid);
  box-shadow: var(--ac-shadow-2);
}

.card-name {
  font-size: 16.5px;
  font-weight: 700;
  color: var(--ac-text-1);
}

.card-meta {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--ac-text-3);
}

.card-metrics {
  display: flex;
  gap: 6px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.metric-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 11.5px;
  color: var(--ac-text-2);
  background: color-mix(in srgb, var(--ac-text-1) 6%, transparent);
}

.metric-chip b {
  color: var(--ac-text-1);
}

.card-actions {
  display: flex;
  gap: 4px;
  padding: 10px 12px 12px;
  opacity: 0;
  transform: translateY(6px);
  transition: opacity 0.24s ease, transform 0.28s cubic-bezier(0.2, 0, 0, 1);
}

.member-card:hover .card-actions {
  opacity: 1;
  transform: translateY(0);
}

body.ac-lite .card-actions {
  opacity: 1;
  transform: none;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 10px;
  border: 1px solid var(--ac-border);
  border-radius: 9px;
  background: transparent;
  font-size: 12px;
  color: var(--ac-text-2);
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, background-color 0.18s ease;
}

.action-btn:hover {
  border-color: var(--ac-border-strong);
  color: var(--ac-text-1);
}

.action-btn.danger:hover {
  color: var(--ac-coral);
  border-color: color-mix(in srgb, var(--ac-coral) 45%, transparent);
  background: var(--ac-danger-bg);
}

.add-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 220px;
  border: 1.5px dashed var(--ac-border-strong);
  border-radius: 20px;
  background: transparent;
  color: var(--ac-text-3);
  cursor: pointer;
  transition: border-color 0.24s ease, color 0.24s ease, transform 0.24s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.add-card:hover {
  color: var(--ac-primary);
  border-color: color-mix(in srgb, var(--ac-primary) 55%, transparent);
  transform: translateY(-3px);
}

.add-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
  animation: add-breathe 2.4s ease-in-out infinite;
}

@keyframes add-breathe {
  0%,
  100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 color-mix(in srgb, var(--ac-primary) 25%, transparent);
  }
  50% {
    transform: scale(1.06);
    box-shadow: 0 0 0 10px transparent;
  }
}

body.ac-lite .add-icon {
  animation: none;
}

.add-text {
  font-size: 13.5px;
  font-weight: 600;
}

.member-skeleton {
  padding: 18px;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-avatar {
  width: 48px;
  height: 48px;
  border-radius: 16px;
}

.skeleton-line {
  width: 70%;
  height: 14px;
  border-radius: 8px;
}

.skeleton-line.short {
  width: 45%;
}
</style>
