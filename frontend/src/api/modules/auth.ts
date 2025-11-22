import { request } from '@/api/http';
import type { LoginPayload, LoginResult, RegisterPayload, UserProfile } from '@/types/auth';

export const authApi = {
  login(payload: LoginPayload) {
    return request<LoginResult>({
      url: '/auth/login',
      method: 'POST',
      data: payload
    });
  },
  register(payload: RegisterPayload) {
    return request<UserProfile>({
      url: '/auth/register',
      method: 'POST',
      data: payload
    });
  },
  refresh(refreshToken: string) {
    return request<LoginResult>({
      url: '/auth/refresh',
      method: 'POST',
      data: { refreshToken }
    });
  },
  fetchProfile() {
    return request<UserProfile>({
      url: '/auth/profile',
      method: 'GET'
    });
  },
  logout(refreshToken?: string) {
    return request<void>({
      url: '/auth/logout',
      method: 'POST',
      data: refreshToken ? { refreshToken } : undefined
    });
  }
};
