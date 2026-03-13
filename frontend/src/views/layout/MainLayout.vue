<template>
  <div class="main-layout">
    <aside class="sidebar">
      <div class="logo">
        <h2>批量图片水印协作平台</h2>
      </div>
      <el-menu
        ref="sidebarMenuRef"
        :default-active="activeMenu"
        :default-openeds="defaultOpeneds"
        class="sidebar-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        @select="handleMenuSelect"
        @open="handleSubMenuOpen"
        @close="handleSubMenuClose"
      >
        <el-menu-item index="/">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-sub-menu index="/team">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>团队管理</span>
          </template>
          <el-menu-item index="/team">团队概览</el-menu-item>
          <el-menu-item index="/team/invite">邀请码管理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/template">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>水印模板</span>
          </template>
          <el-menu-item index="/template">模板列表</el-menu-item>
          <el-menu-item index="/template/draft">草稿区</el-menu-item>
          <el-menu-item index="/font">字体管理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/task">
          <template #title>
            <el-icon><Files /></el-icon>
            <span>批量任务</span>
          </template>
          <el-menu-item index="/task/create">创建批量任务</el-menu-item>
          <el-menu-item index="/task/execution">批量任务执行</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/logs">
          <template #title>
            <el-icon><List /></el-icon>
            <span>操作日志</span>
          </template>
          <el-menu-item index="/logs/team">团队变更</el-menu-item>
          <el-menu-item index="/logs/watermark">水印资源</el-menu-item>
          <el-menu-item index="/logs/points">点数流水</el-menu-item>
          <el-menu-item index="/logs/tasks">任务记录</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>
    <div class="main-content">
      <header class="header">
        <div class="header-left">
          <h3 class="page-title">{{ pageTitle }}</h3>
        </div>
        <div class="header-right">
          <div class="user-info">
            <span class="username">{{ userStore.userInfo?.username }}</span>
            <span class="role-badge" :class="teamRoleClass">
              {{ isLeaderRole ? '队长' : '成员' }}
            </span>
            <span class="points">
              <el-icon><Coin /></el-icon>
              {{ teamInfo?.pointBalance || 0 }} 点
            </span>
          </div>
          <el-dropdown @command="handleCommand">
            <span class="dropdown-trigger">
              <el-icon><Setting /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">修改用户信息</el-dropdown-item>
                <el-dropdown-item command="unregister" divided>注销账户</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <el-dialog v-model="showProfileDialog" title="修改用户信息" width="450px" :close-on-click-modal="false" @close="handleProfileDialogClose">
      <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="profileForm.username" :placeholder="userStore.userInfo?.username || '请输入用户名'" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" :placeholder="userStore.userInfo?.email || '请输入邮箱'" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="profileForm.newPassword" type="password" placeholder="不修改则留空" />
        </el-form-item>
        <el-form-item label="验证码" prop="code" v-if="profileForm.email && profileForm.email !== userStore.userInfo?.email">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-input v-model="profileForm.code" placeholder="请输入验证码" style="flex: 1" />
            <el-button 
              :disabled="emailCodeDisabled" 
              @click="handleSendCode"
            >
              {{ emailCodeText }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProfileDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveProfile">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showPaymentDialog"
      title="点数充值（支付宝沙箱）"
      width="560px"
      :close-on-click-modal="false"
      @close="handlePaymentDialogClose"
    >
      <div class="payment-dialog">
        <el-steps :active="paymentStepIndex" finish-status="success" simple>
          <el-step title="确认订单" />
          <el-step title="扫码支付" />
          <el-step title="充值成功" />
        </el-steps>

        <div v-if="paymentStep === 'confirm'" class="payment-stage confirm-stage">
          <div class="balance-grid">
            <div class="balance-card">
              <span class="label">当前点数</span>
              <span class="value">{{ currentBalance }}</span>
            </div>
            <div class="balance-card">
              <span class="label">充值点数</span>
              <span class="value">{{ paymentForm.points }}</span>
            </div>
            <div class="balance-card">
              <span class="label">应付金额</span>
              <span class="value amount">¥{{ paymentAmount }}</span>
            </div>
            <div class="balance-card">
              <span class="label">充值后预计点数</span>
              <span class="value success">{{ expectedBalance }}</span>
            </div>
          </div>
          <el-form label-width="90px">
            <el-form-item label="充值点数">
              <el-input-number v-model="paymentForm.points" :min="1" :max="1000000" />
            </el-form-item>
            <el-form-item label="计费规则">
              <span>1 点数 = 1 分钱（¥0.01）</span>
            </el-form-item>
          </el-form>
          <el-alert class="sandbox-alert sandbox-alert-compact" type="warning" :closable="false" show-icon>
            <template #title>当前为支付宝沙箱环境，仅供学习与测试，不会发生真实资金交易</template>
            <div class="sandbox-links">
              <a :href="alipaySandboxDocUrl" target="_blank" rel="noopener">支付宝沙箱文档</a>
              <a :href="alipaySandboxAppUrl" target="_blank" rel="noopener">沙箱版 App 下载说明</a>
            </div>
          </el-alert>
        </div>

        <div v-else-if="paymentStep === 'qrcode'" class="payment-stage qrcode-stage">
          <div class="qrcode-box">
            <img
              v-if="currentPaymentOrder?.qrCodeBase64"
              :src="currentPaymentOrder.qrCodeBase64"
              alt="支付宝沙箱支付二维码"
            />
            <el-skeleton v-else animated :rows="6" />
          </div>
          <div class="qrcode-meta">
            <div>订单号：{{ currentPaymentOrder?.orderNo || '-' }}</div>
            <div>支付金额：¥{{ currentPaymentOrder ? formatOrderAmount(currentPaymentOrder.amount) : paymentAmount }}</div>
            <div>状态：{{ paymentStatusText }}</div>
          </div>
          <el-alert class="sandbox-alert" type="info" :closable="false" show-icon>
            <template #title>请使用支付宝沙箱版 App 扫码，支付完成后本页面会自动刷新状态</template>
          </el-alert>
        </div>

        <div v-else class="payment-stage success-stage">
          <div class="success-visual">
            <div class="success-ring">
              <el-icon><Check /></el-icon>
            </div>
          </div>
          <h4 class="success-title">充值成功</h4>
          <p class="success-desc">团队点数已到账，可以继续使用批量处理功能</p>
          <div class="success-balance">{{ animatedBalance }} 点</div>
        </div>
      </div>
      <template #footer>
        <template v-if="paymentStep === 'confirm'">
          <el-button @click="showPaymentDialog = false">取消</el-button>
          <el-button type="primary" :loading="paymentLoading" @click="handlePayment">继续充值</el-button>
        </template>
        <template v-else-if="paymentStep === 'qrcode'">
          <el-button @click="showPaymentDialog = false">取消支付</el-button>
          <el-button type="primary" :loading="paymentManualQuerying" @click="handleManualPaymentRefresh">我已支付，立即刷新</el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="showPaymentDialog = false">完成</el-button>
        </template>
      </template>
    </el-dialog>

    <el-dialog v-model="showUnregisterDialog" title="注销账户" width="400px">
      <div class="unregister-tip">
        <el-alert type="warning" :closable="false">
          注销账户将永久删除您的账户及所有关联数据，此操作不可恢复
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="showUnregisterDialog = false">取消</el-button>
        <el-button type="danger" :loading="unregisterLoading" @click="handleUnregister">确认注销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onBeforeUnmount, provide, watch, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  useUserStore,
  DEFAULT_SIDEBAR_ACTIVE_PATH,
  DEFAULT_SIDEBAR_OPENED_MENUS,
  SIDEBAR_MENU_ROUTE_PATHS
} from '@/stores/user'
import { getTeamInfo } from '@/api/team'
import { updateProfile } from '@/api/user'
import { sendCode } from '@/api/auth'
import { unregister as unregisterApi } from '@/api/auth'
import { createPaymentOrder, queryPaymentOrder } from '@/api/payment'
import { OPEN_RECHARGE_DIALOG_KEY, TEAM_INFO_UPDATED_EVENT, TEAM_POINTS_UPDATED_EVENT } from '@/constants/payment'
import type { PaymentOrderVO, TeamMemberVO } from '@/types'
import {
  House,
  UserFilled,
  Document,
  Files,
  Coin,
  List,
  Setting,
  Check
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const teamInfo = ref<TeamMemberVO | null>(null)
const showProfileDialog = ref(false)
const showPaymentDialog = ref(false)
const showUnregisterDialog = ref(false)
const saving = ref(false)
const paymentLoading = ref(false)
const unregisterLoading = ref(false)
const emailCountdown = ref(0)
const profileFormRef = ref<FormInstance>()
const sidebarMenuRef = ref<{ updateActiveIndex: (index: string) => void } | null>(null)
const defaultOpeneds = ref<string[]>([...userStore.getSidebarState().openedMenus])

const sideBarOpenedMenuSet = new Set(DEFAULT_SIDEBAR_OPENED_MENUS)
const menuRoutePathSet = new Set(SIDEBAR_MENU_ROUTE_PATHS)

const usernameRegex = /^[a-zA-Z0-9_\u4e00-\u9fa5]{4,16}$/
const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
const emailCodeRegex = /^\d{6}$/

const validateProfileCode = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  const needEmailCode = !!profileForm.email && profileForm.email !== userStore.userInfo?.email
  if (!needEmailCode) {
    callback()
    return
  }
  if (!value) {
    callback(new Error('请输入验证码'))
    return
  }
  if (!emailCodeRegex.test(value)) {
    callback(new Error('邮箱验证码长度为6位'))
    return
  }
  callback()
}

const profileForm = reactive({
  username: '',
  email: '',
  newPassword: '',
  code: ''
})

const profileRules: FormRules = {
  username: [
    { pattern: usernameRegex, message: '用户名为4-16位字母、数字、下划线或中文', trigger: 'blur' }
  ],
  email: [
    { pattern: emailRegex, message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  newPassword: [
    { min: 6, max: 16, message: '密码长度需在6-16位之间', trigger: 'blur' }
  ],
  code: [
    { validator: validateProfileCode, trigger: 'blur' }
  ]
}

const paymentForm = reactive({
  points: 100
})
type PaymentStep = 'confirm' | 'qrcode' | 'success'

const PAYMENT_POLL_INTERVAL_MS = 2000
const PAYMENT_POLL_TIMEOUT_MS = 5 * 60 * 1000
const POINT_PRICE = 0.01
const alipaySandboxDocUrl = 'https://open.alipay.com/develop/sandbox/app'
const alipaySandboxAppUrl = 'https://open.alipay.com/develop/sandbox/tool/alipayclint'

const paymentStep = ref<PaymentStep>('confirm')
const paymentQuerying = ref(false)
const paymentManualQuerying = ref(false)
const paymentStatusText = ref('等待扫码支付')
const currentPaymentOrder = ref<PaymentOrderVO | null>(null)
const paymentPollTimer = ref<number | null>(null)
const paymentDeadline = ref<number>(0)
const animatedBalance = ref<number>(0)
const balanceAnimationFrameId = ref<number | null>(null)

const activeMenu = computed(() => {
  return route.path
})

const teamRoleClass = computed(() => teamInfo.value?.role || '')
const isLeaderRole = computed(() => teamInfo.value?.role === 'leader')
const emailCodeDisabled = computed(() => emailCountdown.value > 0)
const emailCodeText = computed(() => emailCountdown.value > 0 ? `${emailCountdown.value}s` : '发送验证码')
const paymentStepIndex = computed(() => {
  if (paymentStep.value === 'qrcode') {
    return 1
  }
  if (paymentStep.value === 'success') {
    return 3
  }
  return 0
})
const currentBalance = computed(() => teamInfo.value?.pointBalance || 0)
const expectedBalance = computed(() => currentBalance.value + paymentForm.points)
const paymentAmount = computed(() => (paymentForm.points * POINT_PRICE).toFixed(2))
const pageTitle = computed(() => {
  const title = route.meta?.title as string | undefined
  const parentTitle = route.meta?.parentTitle as string | undefined
  if (parentTitle && title) {
    return `${parentTitle} > ${title}`
  }
  return title || '首页'
})

function normalizeActiveMenu(path: string | undefined) {
  if (!path || !menuRoutePathSet.has(path)) {
    return DEFAULT_SIDEBAR_ACTIVE_PATH
  }
  return path
}

function normalizeOpenedMenus(openedMenus: string[] | undefined) {
  if (!Array.isArray(openedMenus)) {
    return [...DEFAULT_SIDEBAR_OPENED_MENUS]
  }
  const normalized = Array.from(new Set(openedMenus.filter((menu) => sideBarOpenedMenuSet.has(menu))))
  return normalized.length > 0 ? normalized : [...DEFAULT_SIDEBAR_OPENED_MENUS]
}

function persistSidebarState(activePath = route.path) {
  userStore.setSidebarState({
    activePath: normalizeActiveMenu(activePath),
    openedMenus: normalizeOpenedMenus(defaultOpeneds.value)
  })
}

function handleSubMenuOpen(index: string) {
  if (!sideBarOpenedMenuSet.has(index)) {
    return
  }
  if (!defaultOpeneds.value.includes(index)) {
    defaultOpeneds.value = [...defaultOpeneds.value, index]
    persistSidebarState()
  }
}

function handleSubMenuClose(index: string) {
  if (!sideBarOpenedMenuSet.has(index)) {
    return
  }
  defaultOpeneds.value = defaultOpeneds.value.filter((menu) => menu !== index)
  persistSidebarState()
}

async function handleMenuSelect(index: string) {
  if (!index || index === route.path) {
    return
  }

  try {
    const navigationResult = await router.push(index)
    if (navigationResult) {
      nextTick(() => {
        sidebarMenuRef.value?.updateActiveIndex(route.path)
      })
    } else {
      persistSidebarState(index)
    }
  } catch (error) {
    nextTick(() => {
      sidebarMenuRef.value?.updateActiveIndex(route.path)
    })
    console.error('菜单跳转失败:', error)
  }
}

async function fetchTeamInfo() {
  try {
    const res = await getTeamInfo()
    if (res.code === 200) {
      teamInfo.value = res.data
      userStore.setTeamInfo(res.data)
    }
  } catch (error) {
    console.error('获取团队信息失败:', error)
  }
}

function handleTeamPointsUpdated() {
  void fetchTeamInfo()
}

function handleTeamInfoUpdated() {
  void fetchTeamInfo()
}

function handleCommand(command: string) {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    profileForm.username = ''
    profileForm.email = ''
    profileForm.newPassword = ''
    profileForm.code = ''
    showProfileDialog.value = true
  } else if (command === 'unregister') {
    showUnregisterDialog.value = true
  }
}

async function handleLogout() {
  persistSidebarState(route.path)
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/auth/login')
}

function handleProfileDialogClose() {
  profileForm.username = ''
  profileForm.email = ''
  profileForm.newPassword = ''
  profileForm.code = ''
  profileFormRef.value?.clearValidate()
}

function handlePaymentDialogClose() {
  stopPaymentPolling()
  stopBalanceAnimation()
  paymentForm.points = 100
  paymentStep.value = 'confirm'
  paymentStatusText.value = '等待扫码支付'
  currentPaymentOrder.value = null
  animatedBalance.value = currentBalance.value
}

async function handleSaveProfile() {
  if (!profileFormRef.value) return

  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  const nextUsername = profileForm.username.trim()
  const nextEmail = profileForm.email.trim()
  const nextPassword = profileForm.newPassword
  const nextCode = profileForm.code.trim()
  const needEmailCode = !!nextEmail && nextEmail !== userStore.userInfo?.email

  const payload: Record<string, string> = {}
  if (nextUsername) {
    payload.username = nextUsername
  }
  if (nextPassword) {
    payload.newPassword = nextPassword
  }
  if (needEmailCode) {
    payload.newEmail = nextEmail
    payload.emailCode = nextCode
  }

  if (Object.keys(payload).length === 0) {
    ElMessage.warning('请至少修改一项信息')
    return
  }

  saving.value = true
  try {
    await updateProfile(payload)
    if (userStore.userInfo) {
      if (payload.username) {
        userStore.userInfo.username = payload.username
      }
      if (payload.newEmail) {
        userStore.userInfo.email = payload.newEmail
      }
    }
    ElMessage.success('保存成功')
    showProfileDialog.value = false

    if (payload.newPassword || payload.newEmail) {
      persistSidebarState(route.path)
      await userStore.logout(true)
      ElMessage.success('已退出登录')
      await router.push('/auth/login')
    }
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}

async function handleSendCode() {
  if (!profileForm.email) {
    ElMessage.warning('请输入邮箱')
    return
  }
  if (!emailRegex.test(profileForm.email)) {
    ElMessage.warning('请输入有效的邮箱地址')
    return
  }
  try {
    await sendCode({ email: profileForm.email, type: 'update_email' })
    ElMessage.success('验证码已发送')
    emailCountdown.value = 60
    const timer = setInterval(() => {
      emailCountdown.value--
      if (emailCountdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

async function handlePayment() {
  if (paymentStep.value !== 'confirm') {
    return
  }
  paymentStatusText.value = '正在创建支付订单...'
  paymentLoading.value = true
  try {
    const res = await createPaymentOrder({ points: paymentForm.points })
    if (res.code === 200) {
      currentPaymentOrder.value = res.data
      paymentStep.value = 'qrcode'
      paymentStatusText.value = '请使用支付宝沙箱 App 扫码支付'
      paymentDeadline.value = Date.now() + PAYMENT_POLL_TIMEOUT_MS
      ElMessage.success('订单创建成功，请扫码支付')
      await pollPaymentStatus()
      stopPaymentPolling()
      paymentPollTimer.value = window.setInterval(() => {
        void pollPaymentStatus()
      }, PAYMENT_POLL_INTERVAL_MS)
    }
  } catch (error) {
    paymentStatusText.value = '订单创建失败，请重试'
    console.error('创建订单失败:', error)
  } finally {
    paymentLoading.value = false
  }
}

function stopPaymentPolling() {
  if (paymentPollTimer.value !== null) {
    window.clearInterval(paymentPollTimer.value)
    paymentPollTimer.value = null
  }
  paymentQuerying.value = false
  paymentManualQuerying.value = false
}

function stopBalanceAnimation() {
  if (balanceAnimationFrameId.value !== null) {
    cancelAnimationFrame(balanceAnimationFrameId.value)
    balanceAnimationFrameId.value = null
  }
}

function animateBalance(from: number, to: number, durationMs = 900) {
  stopBalanceAnimation()
  if (from === to) {
    animatedBalance.value = to
    return
  }
  const start = performance.now()
  const delta = to - from
  const tick = (timestamp: number) => {
    const progress = Math.min(1, (timestamp - start) / durationMs)
    animatedBalance.value = Math.round(from + delta * progress)
    if (progress < 1) {
      balanceAnimationFrameId.value = requestAnimationFrame(tick)
    } else {
      balanceAnimationFrameId.value = null
    }
  }
  balanceAnimationFrameId.value = requestAnimationFrame(tick)
}

function formatOrderAmount(amount: number | string | undefined) {
  const amountNumber = Number(amount)
  if (Number.isFinite(amountNumber)) {
    return amountNumber.toFixed(2)
  }
  return paymentAmount.value
}

async function handlePaymentSuccess() {
  const beforeBalance = currentBalance.value
  stopPaymentPolling()
  paymentStep.value = 'success'
  paymentStatusText.value = '支付成功'
  await fetchTeamInfo()
  const afterBalance = teamInfo.value?.pointBalance ?? beforeBalance
  animatedBalance.value = beforeBalance
  animateBalance(beforeBalance, afterBalance)
  window.dispatchEvent(new Event(TEAM_POINTS_UPDATED_EVENT))
}

async function handleManualPaymentRefresh() {
  await pollPaymentStatus(true)
}

async function pollPaymentStatus(forceSync = false) {
  if (paymentStep.value !== 'qrcode' || !currentPaymentOrder.value) {
    return
  }
  if (paymentQuerying.value) {
    return
  }
  if (Date.now() >= paymentDeadline.value) {
    stopPaymentPolling()
    paymentStatusText.value = '支付等待超时，请重新下单'
    ElMessage.warning('支付等待超时，请重新发起充值')
    return
  }
  paymentQuerying.value = true
  if (forceSync) {
    paymentManualQuerying.value = true
  }
  try {
    const res = await queryPaymentOrder(currentPaymentOrder.value.orderNo, forceSync)
    if (res.code === 200) {
      currentPaymentOrder.value = {
        ...res.data,
        qrCodeBase64: currentPaymentOrder.value.qrCodeBase64 || res.data.qrCodeBase64
      }
      if (res.data.status === 'paid') {
        await handlePaymentSuccess()
      } else {
        paymentStatusText.value = '等待扫码支付'
      }
    }
  } catch (error) {
    console.error('查询支付状态失败:', error)
  } finally {
    paymentQuerying.value = false
    if (forceSync) {
      paymentManualQuerying.value = false
    }
  }
}

async function handleUnregister() {
  try {
    await ElMessageBox.confirm(
      '请再次确认：您确定要注销账户吗？',
      {
        confirmButtonText: '确定注销',
        cancelButtonText: '取消',
        type: 'error'
      }
    )
    unregisterLoading.value = true
    await unregisterApi()
    userStore.logout()
    ElMessage.success('账户已注销')
    router.push('/auth/login')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('注销失败:', error)
    }
  } finally {
    unregisterLoading.value = false
    showUnregisterDialog.value = false
  }
}

function openPaymentDialog() {
  handlePaymentDialogClose()
  showPaymentDialog.value = true
}

provide(OPEN_RECHARGE_DIALOG_KEY, openPaymentDialog)

onMounted(() => {
  void fetchTeamInfo()
  window.addEventListener(TEAM_POINTS_UPDATED_EVENT, handleTeamPointsUpdated)
  window.addEventListener(TEAM_INFO_UPDATED_EVENT, handleTeamInfoUpdated)
  const { activePath, openedMenus } = userStore.getSidebarState()
  defaultOpeneds.value = normalizeOpenedMenus(openedMenus)
  const restoredPath = normalizeActiveMenu(activePath)

  if (route.path === DEFAULT_SIDEBAR_ACTIVE_PATH && restoredPath !== DEFAULT_SIDEBAR_ACTIVE_PATH) {
    void router.replace(restoredPath).catch((error) => {
      console.error('恢复菜单页面失败:', error)
    })
  }

  nextTick(() => {
    sidebarMenuRef.value?.updateActiveIndex(route.path)
  })
  persistSidebarState(route.path)
})

onBeforeUnmount(() => {
  window.removeEventListener(TEAM_POINTS_UPDATED_EVENT, handleTeamPointsUpdated)
  window.removeEventListener(TEAM_INFO_UPDATED_EVENT, handleTeamInfoUpdated)
  stopPaymentPolling()
  stopBalanceAnimation()
})

watch(
  () => route.path,
  (path) => {
    void fetchTeamInfo()
    persistSidebarState(path)
    nextTick(() => {
      sidebarMenuRef.value?.updateActiveIndex(route.path)
    })
  }
)
</script>

<style scoped lang="scss">
.main-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.sidebar {
  width: 220px;
  background-color: #304156;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #2b3a4a;

    h2 {
      color: #fff;
      font-size: 18px;
      font-weight: 600;
    }
  }

  .sidebar-menu {
    border-right: none;
    flex: 1;
    overflow-y: auto;
  }
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--color-bg-page);
}

.header {
  height: 60px;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: var(--shadow-light);
  flex-shrink: 0;

  .header-left {
    .page-title {
      font-size: 18px;
      font-weight: 500;
      color: var(--color-text-primary);
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .user-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .username {
        font-size: 14px;
        color: var(--color-text-primary);
      }

      .role-badge {
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 12px;

        &.leader {
          background-color: #e6f7ff;
          color: #1890ff;
        }

        &.member {
          background-color: #f6f6f6;
          color: #666;
        }
      }

      .points {
        display: flex;
        align-items: center;
        gap: 4px;
        color: #e6a23c;
        font-size: 14px;
        font-weight: 500;
      }
    }

    .dropdown-trigger {
      cursor: pointer;
      padding: 8px 12px;
      border-radius: 4px;
      transition: background-color 0.3s;
      display: flex;
      align-items: center;
      gap: 4px;

      &:hover {
        background-color: #f5f7fa;
      }

      .setting-text {
        font-size: 14px;
        color: var(--color-text-regular);
      }
    }
  }
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.payment-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 4px;
}

.payment-stage {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.balance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.balance-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid var(--color-border-light);
  background: linear-gradient(135deg, #f9fbff, #ffffff);

  .label {
    font-size: 12px;
    color: var(--color-text-secondary);
  }

  .value {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .value.amount {
    color: #f56c6c;
  }

  .value.success {
    color: #67c23a;
  }
}

.sandbox-alert {
  margin-top: 4px;
}

.sandbox-alert-compact {
  :deep(.el-alert__title) {
    font-size: 12px;
    white-space: nowrap;
  }
}

.sandbox-links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;

  a {
    color: var(--color-primary);
    font-size: 13px;
    font-weight: 500;
  }
}

.qrcode-stage {
  text-align: center;
}

.qrcode-box {
  width: 260px;
  min-height: 260px;
  margin: 0 auto;
  border-radius: 12px;
  border: 1px solid var(--color-border-light);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-light);

  img {
    width: 240px;
    height: 240px;
    object-fit: contain;
  }
}

.qrcode-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--color-text-regular);
  font-size: 13px;
}

.success-stage {
  align-items: center;
  text-align: center;
  padding: 8px 0;
}

.success-visual {
  width: 88px;
  height: 88px;
}

.success-ring {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: radial-gradient(circle at 30% 30%, #7fd67f, #53b953);
  box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.3);
  animation: success-pop 0.45s ease-out, success-pulse 1.8s ease-in-out infinite 0.45s;

  :deep(svg) {
    font-size: 42px;
    font-weight: 700;
  }
}

.success-title {
  font-size: 22px;
  color: #2f9c2f;
}

.success-desc {
  color: var(--color-text-regular);
}

.success-balance {
  font-size: 30px;
  font-weight: 700;
  color: #2f9c2f;
}

@keyframes success-pop {
  0% {
    transform: scale(0.7);
    opacity: 0;
  }

  70% {
    transform: scale(1.08);
    opacity: 1;
  }

  100% {
    transform: scale(1);
  }
}

@keyframes success-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.35);
  }

  70% {
    box-shadow: 0 0 0 16px rgba(103, 194, 58, 0);
  }

  100% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0);
  }
}

@media (max-width: 768px) {
  .balance-grid {
    grid-template-columns: 1fr;
  }

  .qrcode-box {
    width: 220px;
    min-height: 220px;

    img {
      width: 200px;
      height: 200px;
    }
  }
}

.unregister-tip {
  padding: 8px 0;
}
</style>
