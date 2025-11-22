import { defineStore } from 'pinia';
import { authApi, type LoginPayload, type UserProfile } from '@/api/modules/auth';
import { clearTokens, getAccessToken, setTokens } from '@/utils/token';

interface AuthState {
  accessToken: string | null;
  user: UserProfile | null;
  loading: boolean;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: getAccessToken(),
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
    async login(payload: LoginPayload) {
      this.loading = true;
      try {
        const tokens = await authApi.login(payload);
        setTokens(tokens.accessToken, tokens.refreshToken);
        this.accessToken = tokens.accessToken;
        await this.fetchProfile();
      } finally {
        this.loading = false;
      }
    },
    async fetchProfile() {
      if (!this.accessToken) return;
      this.user = await authApi.fetchProfile();
    },
    async logout() {
      try {
        await authApi.logout();
      } catch (error) {
        console.warn('logout failed', error);
      }
      clearTokens();
      this.accessToken = null;
      this.user = null;
    }
  }
});
