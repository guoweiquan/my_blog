import { request } from '@/api/http';

export interface LoginPayload {
  username: string;
  password: string;
}

export interface LoginResult {
  accessToken: string;
  refreshToken?: string;
}

export interface UserProfile {
  id: number;
  username: string;
  nickname?: string;
  avatarUrl?: string;
  roles: string[];
}

export const authApi = {
  login(payload: LoginPayload) {
    return request<LoginResult>({
      url: '/auth/login',
      method: 'POST',
      data: payload
    });
  },
  fetchProfile() {
    return request<UserProfile>({
      url: '/auth/profile',
      method: 'GET'
    });
  },
  logout() {
    return request<void>({
      url: '/auth/logout',
      method: 'POST'
    });
  }
};
