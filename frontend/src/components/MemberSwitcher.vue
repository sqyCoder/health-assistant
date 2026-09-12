<template>
  <el-select
    :model-value="memberStore.currentMemberId ?? ''"
    placeholder="选择成员"
    class="member-switcher"
    :no-data-text="memberStore.loaded ? '暂无成员' : '加载中'"
    @update:model-value="onChange"
  >
    <el-option v-for="item in memberStore.members" :key="item.id" :label="item.name" :value="item.id">
      <span>{{ item.name }}</span>
      <span class="option-relation">{{ labelOf(RELATION_LABELS, item.relation) }}</span>
    </el-option>
  </el-select>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useMemberStore } from '@/stores/member'
import { RELATION_LABELS, labelOf } from '@/utils/dict'

const router = useRouter()
const memberStore = useMemberStore()

function onChange(value: string): void {
  if (!value) {
    router.push('/members/new')
    return
  }
  memberStore.setCurrent(value)
}
</script>

<style scoped>
.member-switcher {
  width: 160px;
}

.option-relation {
  float: right;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
