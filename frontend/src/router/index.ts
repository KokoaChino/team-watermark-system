import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/views/layout/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/home/DashboardView.vue'),
          meta: { title: '首页' }
        },
        {
          path: 'team',
          name: 'team',
          component: () => import('@/views/team/TeamOverviewView.vue'),
          meta: { title: '团队概览' }
        },
        {
          path: 'team/invite',
          name: 'teamInvite',
          component: () => import('@/views/team/TeamInviteView.vue'),
          meta: { title: '邀请码管理', parent: 'team' }
        },
        {
          path: 'font',
          name: 'font',
          component: () => import('@/views/font/FontManageView.vue'),
          meta: { title: '字体管理' }
        },
        {
          path: 'template',
          name: 'template',
          component: () => import('@/views/template/TemplateListView.vue'),
          meta: { title: '模板列表' }
        },
        {
          path: 'template/draft',
          name: 'templateDraft',
          component: () => import('@/views/template/TemplateDraftView.vue'),
          meta: { title: '草稿区' }
        },
        {
          path: 'task',
          name: 'task',
          component: () => import('@/views/task/TaskListView.vue'),
          meta: { title: '批量任务' }
        },
        {
          path: 'task/create',
          name: 'taskCreate',
          component: () => import('@/views/task/TaskCreateView.vue'),
          meta: { title: '创建任务', parent: 'task' }
        },
        {
          path: 'logs',
          name: 'logs',
          component: () => import('@/views/logs/OperationLogView.vue'),
          meta: { title: '操作日志' }
        }
      ]
    },
    {
      path: '/auth',
      children: [
        {
          path: 'login',
          name: 'login',
          component: () => import('@/views/auth/LoginView.vue'),
          meta: { guest: true }
        },
        {
          path: 'register',
          name: 'register',
          component: () => import('@/views/auth/RegisterView.vue'),
          meta: { guest: true }
        },
        {
          path: 'forgot-password',
          name: 'forgotPassword',
          component: () => import('@/views/auth/ForgotPasswordView.vue'),
          meta: { guest: true }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: () => import('@/views/NotFoundView.vue')
    }
  ],
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const isLoggedIn = !!userStore.token

  if (to.meta.requiresAuth && !isLoggedIn) {
    next({
      name: 'login',
      query: { redirect: to.fullPath }
    })
  } else if (to.meta.guest && isLoggedIn) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
