export interface UserProfile {
  id: number;
  username: string;
  realName: string;
  role: 'student' | 'coach';
}

export interface LoginPayload {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: UserProfile;
}
