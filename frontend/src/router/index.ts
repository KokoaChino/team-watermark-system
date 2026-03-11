import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', component: () => import('@/views/layout/MainLayout.vue'), meta: { requiresAuth: true }, children: [
      { path: '', name: 'home', component: () => import('@/views/home/DashboardView.vue'), meta: { title: '首页' } },
      { path: 'team', name: 'team', component: () => import('@/views/team/TeamOverviewView.vue'), meta: { title: '团队概览', parentTitle: '团队管理' } },
      { path: 'team/invite', name: 'teamInvite', component: () => import('@/views/team/TeamInviteView.vue'), meta: { title: '邀请码管理', parentTitle: '团队管理' } },
      { path: 'font', name: 'font', component: () => import('@/views/font/FontManageView.vue'), meta: { title: '字体管理', parentTitle: '水印模板' } },
      { path: 'template', name: 'template', component: () => import('@/views/template/TemplateListView.vue'), meta: { title: '模板列表', parentTitle: '水印模板' } },
      { path: 'template/draft', name: 'templateDraft', component: () => import('@/views/template/TemplateDraftView.vue'), meta: { title: '草稿区', parentTitle: '水印模板' } },
      { path: 'task', redirect: '/task/create' },
      { path: 'task/create', name: 'taskCreate', component: () => import('@/views/task/TaskCreateView.vue'), meta: { title: '创建批量任务', parentTitle: '批量任务' } },
      { path: 'task/execution', name: 'taskExecution', component: () => import('@/views/task/TaskListView.vue'), meta: { title: '批量任务执行', parentTitle: '批量任务' } },
      { path: 'logs', redirect: '/logs/team' },
      { path: 'logs/team', name: 'teamLogs', component: () => import('@/views/logs/TeamLogView.vue'), meta: { title: '团队变更', parentTitle: '操作日志' } },
      { path: 'logs/watermark', name: 'watermarkLogs', component: () => import('@/views/logs/WatermarkLogView.vue'), meta: { title: '水印资源', parentTitle: '操作日志' } },
      { path: 'logs/points', name: 'pointLogs', component: () => import('@/views/logs/PointLogView.vue'), meta: { title: '点数流水', parentTitle: '操作日志' } },
      { path: 'logs/tasks', name: 'taskLogs', component: () => import('@/views/logs/TaskLogView.vue'), meta: { title: '任务记录', parentTitle: '操作日志' } }
    ] },
    { path: '/auth', children: [
      { path: 'login', name: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { guest: true } },
      { path: 'register', name: 'register', component: () => import('@/views/auth/RegisterView.vue'), meta: { guest: true } },
      { path: 'forgot-password', name: 'forgotPassword', component: () => import('@/views/auth/ForgotPasswordView.vue'), meta: { guest: true } }
    ] },
    { path: '/:pathMatch(.*)*', name: 'notFound', component: () => import('@/views/NotFoundView.vue') }
  ],
  scrollBehavior(_to, _from, savedPosition) { return savedPosition || { top: 0 } }
})
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const isLoggedIn = !!userStore.token
  if (to.meta.requiresAuth && !isLoggedIn) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else if (to.meta.guest && isLoggedIn) {
    next({ name: 'home' })
  } else {
    next()
  }
})
export default router
