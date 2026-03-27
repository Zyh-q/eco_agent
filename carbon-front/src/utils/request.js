import axios from 'axios'

const service = axios.create({
  baseURL: '/api',
  timeout: 20000,
  withCredentials: true
})

// 请求拦截器
service.interceptors.request.use(
  config => config,
  error => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    // 业务状态码不为 200 时视为错误
    if (res.code !== 200) {
      const error = new Error(res.msg || '请求失败')
      error.response = response
      return Promise.reject(error)
    } else {
      // 成功时直接返回后端数据
      return res
    }
  },
  error => {
    const err = new Error(error.message || '网络连接失败')
    err.response = error.response
    return Promise.reject(err)
  }
)

export default service