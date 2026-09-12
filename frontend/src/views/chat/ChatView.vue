<template>
  <div class="chat-layout">
    <aside class="session-panel ac-glass">
      <div class="session-head">
        <span class="session-title">对话</span>
        <button type="button" class="new-session" @click="createSession">
          <Plus :size="14" /> 新建
        </button>
      </div>
      <div v-loading="sessionsLoading" class="session-list ac-scroll">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: session.id === currentSessionId }"
          @click="selectSession(session.id)"
        >
          <span class="session-active-bar" />
          <MessageSquare :size="14" class="session-icon" />
          <span class="session-name">{{ session.title }}</span>
          <el-dropdown trigger="click" @command="(command: string) => onSessionCommand(command, session as ChatSession)">
            <span class="session-more" @click.stop><MoreHorizontal :size="15" /></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">重命名</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div v-if="!sessionsLoading && sessions.length === 0" class="session-empty">暂无对话</div>
      </div>
    </aside>

    <section class="message-panel ac-glass" data-tour="chat">
      <template v-if="currentSessionId">
        <div ref="messagesRef" class="messages ac-scroll">
          <div v-if="messages.length === 0 && !streaming" class="hint-section">
            <div class="hint-orb"><Sparkles :size="18" /></div>
            <div class="hint-title">可以这样问我</div>
            <div class="hint-chips">
              <button v-for="hint in hints" :key="hint" type="button" class="hint-chip" @click="input = hint">
                {{ hint }}
              </button>
            </div>
          </div>

          <div
            v-for="(message, index) in messages"
            :key="index"
            class="msg"
            :class="message.role === 'USER' ? 'msg-user' : 'msg-assistant'"
          >
            <div class="bubble" :class="message.role === 'USER' ? 'bubble-user' : 'bubble-assistant'">
              <pre class="msg-text">{{ message.content }}</pre>
              <span v-if="message.status === 'FAILED'" class="msg-failed">生成失败</span>
            </div>
          </div>

          <div v-if="streaming" class="msg msg-assistant">
            <div class="bubble bubble-assistant">
              <div v-if="!streamingContent" class="typing-dots">
                <span /><span /><span />
              </div>
              <pre v-else class="msg-text">{{ streamingContent }}<span class="stream-caret">▌</span></pre>
            </div>
          </div>
        </div>

        <div class="input-bar">
          <textarea
            v-model="input"
            class="chat-input"
            rows="2"
            placeholder="输入健康问题，如：我最近的血压怎么样？要注意什么？"
            @keydown.enter.exact.prevent="send"
          />
          <div class="input-actions">
            <span class="input-hint">Enter 发送 / Shift+Enter 换行</span>
            <button v-if="!streaming" type="button" class="send-btn" :disabled="!input.trim()" @click="send">
              <SendHorizontal :size="16" />
            </button>
            <button v-else type="button" class="send-btn stop" @click="stop">
              <Square :size="14" />
            </button>
          </div>
        </div>
      </template>
      <AcEmptyState v-else art="chat" title="开始一段健康对话" description="选择左侧会话，或点击「新建」开始提问。" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  MessageSquare,
  MoreHorizontal,
  Plus,
  SendHorizontal,
  Sparkles,
  Square,
} from 'lucide-vue-next'
import AcEmptyState from '@/components/ui/AcEmptyState.vue'
import { chatApi, streamChat } from '@/api/chat'
import { useMemberStore } from '@/stores/member'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'
import type { ChatMessage, ChatSession } from '@/types'

const memberStore = useMemberStore()
const confirm = useConfirm()
const toast = useToast()

const sessions = ref<ChatSession[]>([])
const sessionsLoading = ref(false)
const currentSessionId = ref<string>()
const messages = ref<ChatMessage[]>([])
const input = ref('')
const streaming = ref(false)
const streamingContent = ref('')
const messagesRef = ref<HTMLDivElement>()
let abortController: AbortController | null = null

const hints = [
  '我最近的体检有什么要注意的？',
  '帮我分析最近的体重和血压趋势',
  '根据我的情况，这周适合做什么运动？',
  '我的用药和异常指标有冲突吗？',
]

onMounted(init)
watch(() => memberStore.currentMemberId, init)
onBeforeUnmount(() => stop())

async function init(): Promise<void> {
  if (!memberStore.currentMemberId) {
    sessions.value = []
    currentSessionId.value = undefined
    return
  }
  sessionsLoading.value = true
  try {
    sessions.value = await chatApi.listSessions(memberStore.currentMemberId)
    if (sessions.value.length === 0) {
      await createSession()
    } else {
      await selectSession(sessions.value[0].id)
    }
  } finally {
    sessionsLoading.value = false
  }
}

async function createSession(): Promise<void> {
  if (!memberStore.currentMemberId) {
    toast.info('请先选择一个成员')
    return
  }
  const session = await chatApi.createSession(memberStore.currentMemberId)
  sessions.value.unshift(session)
  await selectSession(session.id)
}

async function selectSession(id: string): Promise<void> {
  if (streaming.value) {
    stop()
  }
  currentSessionId.value = id
  messages.value = await chatApi.listMessages(id)
  await scrollToBottom()
}

async function onSessionCommand(command: string, session: ChatSession): Promise<void> {
  if (command === 'rename') {
    const result = await window.prompt('输入新的会话标题', session.title)
    if (result && result.trim()) {
      await chatApi.renameSession(session.id, result.trim())
      session.title = result.trim()
      toast.success('已重命名')
    }
  } else if (command === 'delete') {
    const confirmed = await confirm.danger('删除该会话？', '会话内的全部消息将被删除，且无法恢复。')
    if (!confirmed) {
      return
    }
    await chatApi.removeSession(session.id)
    sessions.value = sessions.value.filter((item) => item.id !== session.id)
    if (currentSessionId.value === session.id) {
      currentSessionId.value = undefined
      messages.value = []
      if (sessions.value.length > 0) {
        await selectSession(sessions.value[0].id)
      }
    }
    toast.success('已删除')
  }
}

function send(): void {
  const content = input.value.trim()
  if (!content || !currentSessionId.value || streaming.value) {
    return
  }
  input.value = ''
  messages.value.push({ role: 'USER', content, status: 'DONE' })
  streaming.value = true
  streamingContent.value = ''
  scrollToBottom()
  abortController = new AbortController()
  const sessionId = currentSessionId.value
  streamChat(
    sessionId,
    content,
    {
      onDelta: (text) => {
        streamingContent.value += text
        scrollToBottom()
      },
      onDone: async () => {
        const reply = streamingContent.value
        streaming.value = false
        streamingContent.value = ''
        messages.value.push({ role: 'ASSISTANT', content: reply, status: 'DONE' })
        await refreshSessions(sessionId)
        scrollToBottom()
      },
      onError: (message) => {
        streaming.value = false
        if (streamingContent.value) {
          messages.value.push({ role: 'ASSISTANT', content: streamingContent.value, status: 'FAILED' })
        }
        streamingContent.value = ''
        messages.value.push({ role: 'ASSISTANT', content: `抱歉，出错了：${message}`, status: 'FAILED' })
        scrollToBottom()
      },
    },
    abortController.signal,
  )
}

async function refreshSessions(sessionId: string): Promise<void> {
  if (!memberStore.currentMemberId) {
    return
  }
  try {
    sessions.value = await chatApi.listSessions(memberStore.currentMemberId)
    currentSessionId.value = sessionId
  } catch {
    // ignore
  }
}

function stop(): void {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  if (streaming.value && streamingContent.value) {
    messages.value.push({ role: 'ASSISTANT', content: streamingContent.value, status: 'DONE' })
  }
  streaming.value = false
  streamingContent.value = ''
}

async function scrollToBottom(): Promise<void> {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 130px);
}

.session-panel {
  width: 250px;
  flex: none;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  padding: 14px 10px;
}

.session-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 6px 12px;
}

.session-title {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--ac-text-1);
}

.new-session {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: none;
  border-radius: 10px;
  font-size: 12.5px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  background-image: var(--ac-gradient-primary);
  transition: transform 0.18s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.new-session:active {
  transform: scale(0.95);
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 10px 10px 14px;
  border-radius: 12px;
  margin-bottom: 4px;
  cursor: pointer;
  color: var(--ac-text-2);
  transition: background-color 0.18s ease, color 0.18s ease;
}

.session-item:hover {
  background: color-mix(in srgb, var(--ac-primary) 7%, transparent);
  color: var(--ac-text-1);
}

.session-item.active {
  background: color-mix(in srgb, var(--ac-primary) 11%, transparent);
  color: var(--ac-text-1);
}

.session-active-bar {
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 999px;
  background: transparent;
  transition: background-color 0.2s ease;
}

.session-item.active .session-active-bar {
  background-image: var(--ac-gradient-primary);
}

.session-icon {
  flex: none;
  color: var(--ac-text-3);
}

.session-name {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-more {
  opacity: 0;
  color: var(--ac-text-3);
  transition: opacity 0.16s ease;
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 7px;
}

.session-item:hover .session-more {
  opacity: 1;
}

.session-more:hover {
  background: color-mix(in srgb, var(--ac-text-1) 8%, transparent);
}

.session-empty {
  padding: 30px 0;
  text-align: center;
  font-size: 12.5px;
  color: var(--ac-text-3);
}

.message-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  padding: 14px;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 6px 4px;
}

.hint-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 46px 16px;
}

.hint-orb {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  color: #fff;
  background-image: var(--ac-gradient-primary);
  box-shadow: var(--ac-shadow-glow);
  animation: orb-breathe 2.6s ease-in-out infinite;
}

@keyframes orb-breathe {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.06);
  }
}

body.ac-lite .hint-orb {
  animation: none;
}

.hint-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ac-text-1);
}

.hint-chips {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: min(420px, 100%);
}

.hint-chip {
  padding: 11px 16px;
  border: 1px solid var(--ac-border);
  border-radius: 13px;
  background: color-mix(in srgb, var(--ac-surface-solid) 55%, transparent);
  color: var(--ac-text-2);
  font-size: 13px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), border-color 0.2s ease, color 0.2s ease;
}

.hint-chip:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--ac-primary) 45%, transparent);
  color: var(--ac-text-1);
}

.msg {
  display: flex;
  margin-bottom: 14px;
  animation: msg-in 0.32s cubic-bezier(0.05, 0.7, 0.1, 1) both;
}

@keyframes msg-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

body.ac-lite .msg {
  animation: none;
}

.msg-user {
  justify-content: flex-end;
}

.bubble {
  max-width: 78%;
  padding: 11px 15px;
  border-radius: 16px;
}

.bubble-user {
  color: #fff;
  background-image: var(--ac-gradient-primary);
  border-bottom-right-radius: 6px;
  box-shadow: 0 10px 24px -16px color-mix(in srgb, var(--ac-primary) 80%, transparent);
}

.bubble-assistant {
  background: color-mix(in srgb, var(--ac-text-1) 5%, transparent);
  border: 1px solid var(--ac-border);
  border-bottom-left-radius: 6px;
}

.msg-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 13.8px;
  line-height: 1.75;
}

.msg-failed {
  display: block;
  margin-top: 6px;
  font-size: 11.5px;
  color: var(--ac-coral);
}

.typing-dots {
  display: flex;
  gap: 5px;
  padding: 4px 2px;
}

.typing-dots span {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--ac-text-3);
  animation: dot-bounce 1.2s ease-in-out infinite;
}

.typing-dots span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-dots span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes dot-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

.stream-caret {
  color: var(--ac-primary);
  animation: caret-blink 1s steps(2) infinite;
}

@keyframes caret-blink {
  50% {
    opacity: 0;
  }
}

.input-bar {
  border-top: 1px solid var(--ac-border);
  padding-top: 10px;
}

.chat-input {
  width: 100%;
  padding: 11px 14px;
  border: 1px solid var(--ac-border);
  border-radius: 14px;
  background: color-mix(in srgb, var(--ac-surface-solid) 70%, transparent);
  color: var(--ac-text-1);
  font-size: 13.8px;
  font-family: inherit;
  line-height: 1.6;
  resize: none;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.chat-input:focus {
  border-color: color-mix(in srgb, var(--ac-primary) 55%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ac-primary) 14%, transparent);
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.input-hint {
  font-size: 11.5px;
  color: var(--ac-text-3);
}

.send-btn {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 999px;
  color: #fff;
  cursor: pointer;
  background-image: var(--ac-gradient-primary);
  box-shadow: 0 10px 22px -12px color-mix(in srgb, var(--ac-primary) 85%, transparent);
  transition: transform 0.18s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.06);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.94);
}

.send-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.send-btn.stop {
  background-image: var(--ac-gradient-danger);
}

@media (max-width: 900px) {
  .chat-layout {
    flex-direction: column;
    height: auto;
  }

  .session-panel {
    width: 100%;
    max-height: 190px;
  }

  .message-panel {
    height: calc(100vh - 420px);
    min-height: 420px;
  }
}
</style>
