import http from './http';
import type { LoginPayload, LoginResponse } from '@/types/auth';

export function loginApi(payload: LoginPayload): Promise<LoginResponse> {
  const normalizedPayload: LoginPayload = {
    username: payload.username.trim(),
    password: payload.password.trim()
  };

  if (import.meta.env.VITE_ENABLE_MOCK === 'true') {
    const role = normalizedPayload.username.toLowerCase().includes('coach') ? 'coach' : 'student';
    return Promise.resolve({
      token: 'mock-token-demo',
      user: {
        id: role === 'coach' ? 1 : 2,
        username: normalizedPayload.username,
        realName: role === 'coach' ? '演示教练' : '演示学生',
        role
      }
    });
  }

  return http.post('/api/auth/login', normalizedPayload, {
    skipErrorMessage: true
  });
}
