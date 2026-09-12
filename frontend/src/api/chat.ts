import { del, get, post, put, tokenStore } from './request'
import type { ChatMessage, ChatSession } from '@/types'

export interface StreamHandlers {
  onDelta: (text: string) => void
  onDone: (data: { messageId?: string; tokenIn?: number; tokenOut?: number }) => void
  onError: (message: string) => void
}

export const chatApi = {
  listSessions(memberId: string): Promise<ChatSession[]> {
    return get<ChatSession[]>('/chat/sessions', { memberId })
  },
  createSession(memberId: string, title?: string): Promise<ChatSession> {
    return post<ChatSession>('/chat/sessions', { memberId, title })
  },
  renameSession(id: string, title: string): Promise<ChatSession> {
    return put<ChatSession>(`/chat/sessions/${id}`, { title })
  },
  removeSession(id: string): Promise<void> {
    return del<void>(`/chat/sessions/${id}`)
  },
  listMessages(sessionId: string): Promise<ChatMessage[]> {
    return get<ChatMessage[]>(`/chat/sessions/${sessionId}/messages`)
  },
}

export async function streamChat(
  sessionId: string,
  content: string,
  handlers: StreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  let response: Response
  try {
    response = await fetch(`/api/chat/sessions/${sessionId}/messages/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json; charset=utf-8',
        Accept: 'text/event-stream',
        Authorization: `Bearer ${tokenStore.getAccess() ?? ''}`,
      },
      body: JSON.stringify({ content }),
      signal,
    })
  } catch (error) {
    if ((error as Error).name === 'AbortError') {
      return
    }
    handlers.onError(error instanceof Error ? error.message : '网络异常')
    return
  }
  if (!response.ok || !response.body) {
    let detail = `HTTP ${response.status}`
    try {
      const body = await response.json()
      if (body?.message) {
        detail = body.message
      }
    } catch {
      // ignore
    }
    handlers.onError(detail)
    return
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      let index = buffer.indexOf('\n\n')
      while (index >= 0) {
        const chunk = buffer.slice(0, index)
        buffer = buffer.slice(index + 2)
        handleChunk(chunk, handlers)
        index = buffer.indexOf('\n\n')
      }
    }
  } catch (error) {
    if ((error as Error).name !== 'AbortError') {
      handlers.onError(error instanceof Error ? error.message : '流式读取异常')
    }
  }
}

function handleChunk(chunk: string, handlers: StreamHandlers): void {
  let eventName = ''
  const dataLines: string[] = []
  for (const line of chunk.split('\n')) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }
  if (dataLines.length === 0) {
    return
  }
  let data: Record<string, unknown>
  try {
    data = JSON.parse(dataLines.join('\n'))
  } catch {
    return
  }
  if (eventName === 'delta') {
    handlers.onDelta(String(data.content ?? ''))
  } else if (eventName === 'done') {
    handlers.onDone({
      messageId: data.messageId ? String(data.messageId) : undefined,
      tokenIn: typeof data.tokenIn === 'number' ? data.tokenIn : undefined,
      tokenOut: typeof data.tokenOut === 'number' ? data.tokenOut : undefined,
    })
  } else if (eventName === 'error') {
    handlers.onError(String(data.message ?? 'AI 服务异常'))
  }
}
