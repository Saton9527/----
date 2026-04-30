import axios from 'axios';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';

declare module 'axios' {
  interface AxiosRequestConfig {
    skipErrorMessage?: boolean;
  }
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 12000
});

function extractErrorMessage(error: unknown): string {
  if (!axios.isAxiosError(error)) {
    return error instanceof Error ? error.message : '请求失败，请稍后重试';
  }

  const data = error.response?.data;
  if (!data) {
    return error.message || '网络异常，请检查服务是否启动';
  }

  if (typeof data === 'string' && data.trim()) {
    return data;
  }

  if (typeof data.message === 'string' && data.message.trim()) {
    if (data.errors && typeof data.errors === 'object') {
      const details = Object.values(data.errors)
        .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
        .join('；');
      if (details) {
        return `${data.message}：${details}`;
      }
    }
    return data.message;
  }

  if (typeof data.error === 'string' && data.error.trim()) {
    return data.error;
  }

  return error.message || '请求失败，请稍后重试';
}

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

    if (!error.config?.skipErrorMessage) {
      ElMessage.error(extractErrorMessage(error));
    }
    return Promise.reject(error);
  }
);

export default http;
