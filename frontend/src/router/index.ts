import { createRouter, createWebHistory } from 'vue-router';
import MainLayout from '@/layouts/MainLayout.vue';
import { useAuthStore } from '@/store/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: MainLayout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue')
        },
        {
          path: 'tasks',
          name: 'tasks',
          component: () => import('@/views/task/TaskListView.vue')
        },
        {
          path: 'teams',
          name: 'teams',
          component: () => import('@/views/TeamView.vue')
        },
        {
          path: 'problemsets',
          name: 'problemsets',
          component: () => import('@/views/ProblemsetView.vue')
        },
        {
          path: 'problems',
          name: 'problems',
          component: () => import('@/views/ProblemListView.vue')
        },
        {
          path: 'contests',
          name: 'contests',
          component: () => import('@/views/ContestView.vue')
        },
        {
          path: 'coach-tasks',
          name: 'coach-tasks',
          component: () => import('@/views/CoachTaskView.vue')
        },
        {
          path: 'ranking',
          name: 'ranking',
          component: () => import('@/views/RankingView.vue')
        },
        {
          path: 'points',
          name: 'points',
          component: () => import('@/views/PointsView.vue')
        },
        {
          path: 'recommend',
          name: 'recommend',
          component: () => import('@/views/RecommendView.vue')
        },
        {
          path: 'alerts',
          name: 'alerts',
          component: () => import('@/views/AlertView.vue'),
          meta: { roles: ['coach'] }
        },
        {
          path: 'students',
          name: 'students',
          component: () => import('@/views/student/StudentView.vue'),
          meta: { roles: ['coach'] }
        }
      ]
    }
  ]
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  if (to.meta.public) {
    if (to.path === '/login' && authStore.isLoggedIn) {
      return '/dashboard';
    }

    return true;
  }

  if (!authStore.isLoggedIn) {
    return '/login';
  }

  const roles = to.meta.roles as string[] | undefined;
  if (roles && !roles.includes(authStore.role)) {
    return '/dashboard';
  }

  return true;
});

export default router;
