import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/pages/home/HomePage.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'post/:slug',
        name: 'PostDetail',
        component: () => import('@/pages/post/PostDetailPage.vue'),
        meta: { title: '文章详情' }
      },
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/pages/auth/LoginPage.vue'),
        meta: { title: '登录' }
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/pages/auth/RegisterPage.vue'),
        meta: { title: '注册' }
      },
      {
        path: 'search',
        name: 'SearchResult',
        component: () => import('@/pages/search/SearchResultPage.vue'),
        meta: { title: '搜索结果' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['ROLE_ADMIN'], title: '后台管理' },
    children: [
      { path: '', redirect: { name: 'AdminDashboard' } },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/pages/admin/AdminDashboardPage.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'posts',
        name: 'AdminPosts',
        component: () => import('@/pages/admin/AdminPostsPage.vue'),
        meta: { title: '文章管理' }
      },
      {
        path: 'comments',
        name: 'AdminComments',
        component: () => import('@/pages/admin/AdminCommentsPage.vue'),
        meta: { title: '评论管理' }
      },
      {
        path: 'tags',
        name: 'AdminTags',
        component: () => import('@/pages/admin/AdminTagsPage.vue'),
        meta: { title: '标签管理' }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/pages/admin/AdminSettingsPage.vue'),
        meta: { title: '站点设置' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/NotFoundPage.vue')
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  if (authStore.isAuthenticated && !authStore.user) {
    try {
      await authStore.fetchProfile();
    } catch (error) {
      console.warn('fetch profile failed', error);
      await authStore.logout();
    }
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'Login', query: { redirect: to.fullPath } };
  }

  if (to.meta.roles && to.meta.roles.length) {
    const hasAccess = authStore.roles.some((role) => to.meta.roles?.includes(role));
    if (!hasAccess) {
      return { name: 'Home' };
    }
  }

  if (to.meta.title) {
    document.title = `${to.meta.title} | 我的博客`;
  }

  return true;
});

export default router;
