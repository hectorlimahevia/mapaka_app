import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
})

api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

let refreshPromise: Promise<boolean> | null = null

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const auth = useAuthStore()
    const original = error.config

    if (error.response?.status === 401 && auth.accessToken && !original._retried) {
      original._retried = true
      refreshPromise ??= auth.refresh().finally(() => {
        refreshPromise = null
      })
      const refreshed = await refreshPromise
      if (refreshed) {
        return api(original)
      }
      auth.clearSession()
    }

    return Promise.reject(error)
  },
)

export default api
