import axios, { type AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types'

const ACCESS_KEY = 'ha_access_token'
const REFRESH_KEY = 'ha_refresh_token'

export const tokenStore = {
  getAccess(): string | null {
    return localStorage.getItem(ACCESS_KEY)
  },
  getRefresh(): string | null {
    return localStorage.getItem(REFRESH_KEY)
  },
  set(accessToken: string, refreshToken: string): void {
    localStorage.setItem(ACCESS_KEY, accessToken)
    localStorage.setItem(REFRESH_KEY, refreshToken)
  },
  clear(): void {
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
  },
}

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = tokenStore.getAccess()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshPromise: Promise<string> | null = null

function redirectToLogin(): void {
  tokenStore.clear()
  const current = window.location.pathname + window.location.search
  if (!current.startsWith('/login')) {
    window.location.href = `/login?redirect=${encodeURIComponent(current)}`
  }
}

async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStore.getRefresh()
  if (!refreshToken) {
    throw new Error('no refresh token')
  }
  const resp = await axios.post<ApiResult<{ accessToken: string; refreshToken: string }>>(
    '/api/auth/refresh',
    { refreshToken },
  )
  const body = resp.data
  if (body.code !== 0) {
    throw new Error(body.message)
  }
  tokenStore.set(body.data.accessToken, body.data.refreshToken)
  return body.data.accessToken
}

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(body)
    }
    return response
  },
  async (error: AxiosError) => {
    const config = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    if (error.response?.status === 401 && config && !config._retry && !config.url?.includes('/auth/')) {
      config._retry = true
      try {
        if (!refreshPromise) {
          refreshPromise = refreshAccessToken().finally(() => {
            refreshPromise = null
          })
        }
        const newToken = await refreshPromise
        config.headers.Authorization = `Bearer ${newToken}`
        return http(config)
      } catch {
        redirectToLogin()
        return Promise.reject(error)
      }
    }
    if (error.response?.status === 401) {
      redirectToLogin()
      return Promise.reject(error)
    }
    const body = error.response?.data as ApiResult<unknown> | undefined
    ElMessage.error(body?.message || error.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  },
)

export async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const resp = await http.get<ApiResult<T>>(url, { params })
  return resp.data.data
}

export async function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const resp = await http.post<ApiResult<T>>(url, data, config)
  return resp.data.data
}

export async function put<T>(url: string, data?: unknown): Promise<T> {
  const resp = await http.put<ApiResult<T>>(url, data)
  return resp.data.data
}

export async function del<T>(url: string): Promise<T> {
  const resp = await http.delete<ApiResult<T>>(url)
  return resp.data.data
}
