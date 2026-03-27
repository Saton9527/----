import axios from 'axios';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 12000
});

http.interceptors.request.use((config) => {
  const authStore = useAuthStore();
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const authStore = useAuthStore();
    if (error.response?.status === 401) {
      authStore.logout();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
      ElMessage.error('登录已失效，请重新登录');
      return Promise.reject(error);
    }

    const message = error.response?.data?.message ?? '请求失败，请稍后重试';
    ElMessage.error(message);
    return Promise.reject(error);
  }
);

export default http;
