<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useAuthStore } from '@/store/auth';

const router = useRouter();
const authStore = useAuthStore();
const formRef = ref<FormInstance>();
const loading = ref(false);

const form = reactive({
  username: '',
  password: ''
});

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  const username = form.username.trim();
  const password = form.password.trim();
  if (!username || !password) {
    ElMessage.warning('请输入账号和密码');
    return;
  }

  try {
    loading.value = true;
    await authStore.login({ username, password });
    ElMessage.success('登录成功');
    router.push('/dashboard');
  } catch {
    ElMessage.error('登录失败，请检查账号或密码');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-panel glass-panel">
      <p class="eyebrow">ACM Training</p>
      <h1>训练管理系统</h1>
      <p class="tip-text">账号由教练统一导入，无需注册，直接使用分配账号登录。</p>

      <div class="login-form-wrap">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" placeholder="输入导入账号" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="输入密码"
              size="large"
              show-password
              @keyup.enter="onSubmit"
            />
          </el-form-item>
          <el-button class="submit-btn" type="primary" size="large" :loading="loading" @click="onSubmit">
            进入系统
          </el-button>
        </el-form>
      </div>

      <p class="demo-account">示例账号：<code>student01 / 123456</code> 或 <code>coach01 / 123456</code></p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 28px;
}

.login-panel {
  width: 100%;
  max-width: 560px;
  border-radius: 32px;
  padding: 40px;
}

.login-panel h1 {
  font-size: clamp(40px, 5vw, 56px);
  line-height: 1.04;
  margin: 12px 0 0;
}

.tip-text {
  margin: 10px 0 0;
  color: var(--muted);
}

.login-form-wrap {
  margin-top: 24px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.demo-account {
  margin: 14px 0 0;
  color: var(--muted);
  font-size: 13px;
}

@media (max-width: 960px) {
  .login-panel {
    padding: 28px;
  }
}
</style>
