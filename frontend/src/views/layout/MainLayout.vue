<template>
  <div class="main-layout">
    <aside class="sidebar">
      <div class="logo">
        <h2>批量图片水印协作平台</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        :default-openeds="defaultOpeneds"
        class="sidebar-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        @select="handleMenuSelect"
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
          <el-menu-item index="/font">字体管理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/task">
          <template #title>
            <el-icon><Files /></el-icon>
            <span>批量任务</span>
          </template>
          <el-menu-item index="/task">任务列表</el-menu-item>
          <el-menu-item index="/task/create">创建任务</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/logs">
          <el-icon><List /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
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
            <span class="role-badge" :class="teamInfo?.role">
              {{ teamInfo?.role === 'leader' ? '队长' : '成员' }}
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
              :disabled="emailCountdown > 0" 
              @click="handleSendCode"
            >
              {{ emailCountdown > 0 ? `${emailCountdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProfileDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveProfile">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showPaymentDialog" title="点数充值" width="400px" @close="handlePaymentDialogClose">
      <div class="balance-info">
        <span class="label">当前团队点数：</span>
        <span class="value">{{ teamInfo?.pointBalance || 0 }}</span>
      </div>
      <el-divider />
      <el-form label-width="80px">
        <el-form-item label="充值点数">
          <el-input-number v-model="paymentForm.points" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item label="应付金额">
          <span class="amount">¥{{ (paymentForm.points * 0.01).toFixed(2) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPaymentDialog = false">取消</el-button>
        <el-button type="primary" :loading="paymentLoading" @click="handlePayment">立即充值</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showUnregisterDialog" title="注销账户" width="400px">
      <div class="unregister-tip">
        <el-alert type="warning" :closable="false">
          注销账户将永久删除您的账户及所有关联数据，此操作不可恢复。
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
import { ref, computed, onMounted, watch, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getTeamInfo } from '@/api/team'
import { updateProfile } from '@/api/user'
import { sendCode } from '@/api/auth'
import { logout as logoutApi, unregister as unregisterApi } from '@/api/auth'
import { createPaymentOrder } from '@/api/payment'
import type { TeamMemberVO } from '@/types'
import {
  House,
  UserFilled,
  Document,
  Files,
  Coin,
  List,
  Setting
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

const usernameRegex = /^[a-zA-Z0-9_\u4e00-\u9fa5]{4,16}$/
const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/

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
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

const paymentForm = reactive({
  points: 100
})

const activeMenu = computed(() => {
  return route.path
})

const defaultOpeneds = computed(() => {
  const path = route.path
  if (path === '/') return []
  const basePath = '/' + path.split('/')[1]
  return [basePath]
})

const pageTitle = computed(() => {
  return (route.meta?.title as string) || '首页'
})

function handleMenuSelect(index: string) {
  if (index && index !== route.path) {
    router.push(index)
  }
}

async function fetchTeamInfo() {
  try {
    const res = await getTeamInfo()
    if (res.code === 200) {
      teamInfo.value = res.data
    }
  } catch (error) {
    console.error('获取团队信息失败:', error)
  }
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
  paymentForm.points = 100
}

async function handleSaveProfile() {
  if (!profileFormRef.value) return
  
  await profileFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    const needEmailCode = profileForm.email && profileForm.email !== userStore.userInfo?.email
    const needPasswordCode = !!profileForm.newPassword
    
    if (profileForm.email && !profileForm.code) {
      ElMessage.warning('请输入验证码')
      return
    }
    
    saving.value = true
    try {
      const data: any = {}
      if (profileForm.username) {
          data.username = profileForm.username
      }
      if (profileForm.newPassword) {
        data.newPassword = profileForm.newPassword
      }
      if (needEmailCode) {
        data.newEmail = profileForm.email
        data.emailCode = profileForm.code
      } else if (needPasswordCode) {
        data.emailCode = profileForm.code
      }
      
      await updateProfile(data)
      userStore.userInfo!.username = profileForm.username
      userStore.userInfo!.email = profileForm.email || userStore.userInfo!.email
      ElMessage.success('保存成功')
      
      if (profileForm.newPassword || needEmailCode) {
        showProfileDialog.value = false
        handleLogout()
      } else {
        showProfileDialog.value = false
      }
    } catch (error) {
      console.error('保存失败:', error)
    } finally {
      saving.value = false
    }
  })
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
    await sendCode({ email: profileForm.email, type: 'update_password' })
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
  paymentLoading.value = true
  try {
    const res = await createPaymentOrder({ points: paymentForm.points })
    if (res.code === 200) {
      ElMessage.success('订单创建成功，二维码生成中...')
    }
  } catch (error) {
    console.error('创建订单失败:', error)
  } finally {
    paymentLoading.value = false
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
  showPaymentDialog.value = true
}

defineExpose({ openPaymentDialog })

onMounted(() => {
  fetchTeamInfo()
})

watch(
  () => route.path,
  () => {
    fetchTeamInfo()
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

.balance-info {
  font-size: 15px;

  .label {
    color: var(--color-text-secondary);
  }

  .value {
    font-size: 20px;
    font-weight: 600;
    color: #e6a23c;
  }
}

.amount {
  font-size: 20px;
  font-weight: 600;
  color: #f56c6c;
}

.unregister-tip {
  padding: 8px 0;
}
</style>
