import { defineStore } from 'pinia';
import { authApi } from '@/api/modules/auth';
import { clearTokens, getAccessToken, getRefreshToken, setAccessToken as persistAccessToken, setTokens } from '@/utils/token';
import type { LoginPayload, RegisterPayload, UserProfile } from '@/types/auth';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserProfile | null;
  loading: boolean;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: getAccessToken(),
    refreshToken: getRefreshToken(),
    user: null,
    loading: false
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken),
    displayName: (state) => state.user?.nickname || state.user?.username || '访客',
    roles: (state) => state.user?.roles || [],
    isAdmin(): boolean {
      return this.roles.includes('ROLE_ADMIN');
    }
  },
  actions: {
    setSession(tokens: { accessToken: string; refreshToken: string }) {
      setTokens(tokens.accessToken, tokens.refreshToken);
      this.accessToken = tokens.accessToken;
      this.refreshToken = tokens.refreshToken;
    },
    async login(payload: LoginPayload) {
      this.loading = true;
      try {
        const tokens = await authApi.login(payload);
        this.setSession(tokens);
        await this.fetchProfile();
      } finally {
        this.loading = false;
      }
    },
    async register(payload: RegisterPayload) {
      this.loading = true;
      try {
        await authApi.register(payload);
      } finally {
        this.loading = false;
      }
    },
    async fetchProfile() {
      if (!this.accessToken) return;
      this.user = await authApi.fetchProfile();
    },
    async refreshTokens() {
      if (!this.refreshToken) {
        throw new Error('无可用的刷新令牌');
      }
      const tokens = await authApi.refresh(this.refreshToken);
      this.setSession(tokens);
      return tokens.accessToken;
    },
    async logout() {
      try {
        await authApi.logout(this.refreshToken || undefined);
      } catch (error) {
        console.warn('logout failed', error);
      }
      clearTokens();
      this.accessToken = null;
      this.refreshToken = null;
      this.user = null;
    },
    setAccessToken(token: string) {
      persistAccessToken(token);
      this.accessToken = token;
    }
  }
});
