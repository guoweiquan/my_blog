import axios from 'axios';
import type { AxiosRequestConfig, AxiosResponse } from 'axios';
import type { ApiResponse } from '@/types/api';
import { getAccessToken } from '@/utils/token';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

const http = axios.create({
  baseURL,
  withCredentials: true,
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    if (response.data && response.data.success) {
      return response.data.data;
    }
    return Promise.reject(response.data || { message: '请求失败' });
  },
  (error) => Promise.reject(error)
);

export function request<T = unknown>(config: AxiosRequestConfig) {
  return http.request<unknown, T>(config);
}

export default http;
