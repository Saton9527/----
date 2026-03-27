import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import type { LoginPayload, LoginResponse, UserProfile } from '@/types/auth';
import { loginApi } from '@/api/auth';

const TOKEN_KEY = 'acm_train_token';
const USER_KEY = 'acm_train_user';

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '');
  const cachedUser = localStorage.getItem(USER_KEY);
  const user = ref<UserProfile | null>(cachedUser ? JSON.parse(cachedUser) as UserProfile : null);

  const isLoggedIn = computed(() => Boolean(token.value));
  const role = computed(() => user.value?.role ?? 'student');

  async function login(payload: LoginPayload) {
    const response = await loginApi(payload);
    applyLogin(response);
  }

  function applyLogin(response: LoginResponse) {
    token.value = response.token;
    user.value = response.user;
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));
  }

  function logout() {
    token.value = '';
    user.value = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  return {
    token,
    user,
    role,
    isLoggedIn,
    login,
    logout
  };
});
