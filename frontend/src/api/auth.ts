import http from './http';
import type { LoginPayload, LoginResponse } from '@/types/auth';

export function loginApi(payload: LoginPayload): Promise<LoginResponse> {
  if (import.meta.env.VITE_ENABLE_MOCK === 'true') {
    const role = payload.username.toLowerCase().includes('coach') ? 'coach' : 'student';
    return Promise.resolve({
      token: 'mock-token-demo',
      user: {
        id: role === 'coach' ? 1 : 2,
        username: payload.username,
        realName: role === 'coach' ? '演示教练' : '演示学生',
        role
      }
    });
  }

  return http.post('/api/auth/login', payload);
}
