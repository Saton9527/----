<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { coachMenu, studentMenu } from '@/config/menu';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const menuItems = computed(() => (authStore.role === 'coach' ? coachMenu : studentMenu));

function goTo(path: string) {
  router.push(path);
}

function onLogout() {
  authStore.logout();
  router.push('/login');
}
</script>

<template>
  <div class="layout-shell">
    <aside class="layout-sidebar glass-panel">
      <div class="brand">
        <span class="brand-mark">A</span>
        <div>
          <strong>ACM Console</strong>
        </div>
      </div>

      <nav class="nav-list">
        <button
          v-for="item in menuItems"
          :key="item.path"
          class="nav-item"
          :class="{ active: route.path === item.path }"
          @click="goTo(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>
    </aside>

    <main class="layout-main">
      <header class="layout-header glass-panel">
        <div>
          <h2>{{ authStore.user?.realName ?? '未登录用户' }}</h2>
        </div>

        <div class="header-actions">
          <span class="role-badge">{{ authStore.role === 'coach' ? 'Coach' : 'Student' }}</span>
          <el-button type="primary" plain @click="onLogout">退出登录</el-button>
        </div>
      </header>

      <section class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </section>
    </main>
  </div>
</template>

<style scoped>
.layout-shell {
  display: grid;
  grid-template-columns: 280px 1fr;
  min-height: 100vh;
  gap: 18px;
  padding: 18px;
  align-items: start;
}

.layout-sidebar {
  position: sticky;
  top: 18px;
  height: calc(100vh - 36px);
  border-radius: 28px;
  padding: 24px 18px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  overflow-y: auto;
}

.brand {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 10px 12px;
}

.brand strong {
  font-size: 20px;
}

.brand-mark {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
  font-size: 24px;
  background: linear-gradient(135deg, var(--primary), var(--accent));
}

.nav-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-item {
  border: 0;
  background: transparent;
  text-align: left;
  padding: 14px 16px;
  border-radius: 16px;
  font-size: 15px;
  cursor: pointer;
  color: #22313f;
  transition: 0.25s ease;
}

.nav-item:hover,
.nav-item.active {
  background: rgba(29, 91, 143, 0.12);
  color: var(--primary-deep);
  transform: translateX(4px);
}

.layout-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.layout-header {
  border-radius: 28px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.layout-header h2 {
  margin: 0;
  font-size: 26px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-badge {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(220, 141, 36, 0.14);
  color: #905500;
  font-weight: 600;
}

.layout-content {
  min-width: 0;
}

@media (max-width: 960px) {
  .layout-shell {
    grid-template-columns: 1fr;
  }

  .layout-sidebar {
    padding: 16px;
  }

  .nav-list {
    flex-direction: row;
    overflow-x: auto;
  }

  .nav-item {
    white-space: nowrap;
  }
}

@media (max-width: 680px) {
  .layout-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
