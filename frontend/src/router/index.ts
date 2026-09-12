import { createRouter, createWebHistory } from 'vue-router'
import { tokenStore } from '@/api/request'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { public: true, title: '注册' },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'members',
          name: 'member-list',
          component: () => import('@/views/member/MemberListView.vue'),
          meta: { title: '家庭成员' },
        },
        {
          path: 'members/new',
          name: 'member-create',
          component: () => import('@/views/member/MemberFormView.vue'),
          meta: { title: '新增成员' },
        },
        {
          path: 'members/:id/edit',
          name: 'member-edit',
          component: () => import('@/views/member/MemberFormView.vue'),
          meta: { title: '编辑成员' },
        },
        {
          path: 'members/:id',
          name: 'member-detail',
          component: () => import('@/views/member/MemberDetailView.vue'),
          meta: { title: '成员档案' },
        },
        {
          path: 'reports',
          name: 'report-list',
          component: () => import('@/views/report/ReportListView.vue'),
          meta: { title: '体检报告' },
        },
        {
          path: 'reports/:id',
          name: 'report-detail',
          component: () => import('@/views/report/ReportDetailView.vue'),
          meta: { title: '报告详情' },
        },
        {
          path: 'exercise',
          name: 'exercise',
          component: () => import('@/views/exercise/ExerciseView.vue'),
          meta: { title: '运动记录' },
        },
        {
          path: 'charts',
          name: 'charts',
          component: () => import('@/views/charts/ChartsView.vue'),
          meta: { title: '数据分析' },
        },
        {
          path: 'suggestions',
          name: 'suggestion-list',
          component: () => import('@/views/suggestion/SuggestionListView.vue'),
          meta: { title: '健康建议' },
        },
        {
          path: 'suggestions/:id',
          name: 'suggestion-detail',
          component: () => import('@/views/suggestion/SuggestionDetailView.vue'),
          meta: { title: '建议详情' },
        },
        {
          path: 'chat',
          name: 'chat',
          component: () => import('@/views/chat/ChatView.vue'),
          meta: { title: '健康问答' },
        },
        {
          path: 'admin',
          name: 'admin',
          component: () => import('@/views/admin/AdminView.vue'),
          meta: { title: '管理后台', requiresAdmin: true },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach(async (to) => {
  const loggedIn = Boolean(tokenStore.getAccess() || tokenStore.getRefresh())
  if (!to.meta.public && !loggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.public && loggedIn) {
    return { path: '/' }
  }
  if (to.meta.requiresAdmin) {
    const userStore = useUserStore()
    if (!userStore.user) {
      try {
        await userStore.fetchMe()
      } catch {
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }
    if (userStore.user?.role !== 'ADMIN') {
      return { path: '/' }
    }
  }
  document.title = to.meta.title ? `${to.meta.title} - 智能健康助手` : '智能健康助手'
  return true
})

export default router
