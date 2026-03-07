import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

interface HandledRequestError extends Error {
  __handled?: boolean
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const newToken = response.headers['x-new-token']
    if (newToken) {
      const userStore = useUserStore()
      userStore.setToken(newToken)
    }

    const { data } = response
    if (data.code !== 200) {
      const requestError: HandledRequestError = new Error(data.message || '请求失败')
      requestError.__handled = true
      ElMessage.error(requestError.message)
      return Promise.reject(requestError)
    }
    return data
  },
  (error) => {
    let message = '网络错误'

    if (error.response) {
      message = error.response?.data?.message || `请求失败 (${error.response.status})`
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    } else if (error.message === 'Network Error') {
      message = '网络连接失败，请检查网络'
    } else if (error.message) {
      message = error.message
    }

    const handledError: HandledRequestError = error instanceof Error ? error : new Error(message)
    handledError.message = message
    handledError.__handled = true

    ElMessage.error(message)
    return Promise.reject(handledError)
  }
)

export default request
