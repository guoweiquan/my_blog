export interface LoginPayload {
  username: string;
  password: string;
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
}

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  nickname?: string;
}

export interface UserProfile {
  id: number;
  username: string;
  nickname?: string;
  email: string;
  avatarUrl?: string;
  roles: string[];
}
