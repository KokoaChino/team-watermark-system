<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <h1 class="auth-title">批量图片水印协作平台</h1>
        <p class="auth-subtitle">登录您的账户继续使用</p>
      </div>

      <div class="auth-tabs">
        <div
          class="auth-tab"
          :class="{ active: loginType === 'password' }"
          @click="switchLoginType('password')"
        >
          密码登录
        </div>
        <div
          class="auth-tab"
          :class="{ active: loginType === 'code' }"
          @click="switchLoginType('code')"
        >
          验证码登录
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        class="auth-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="account">
          <el-input
            v-model="formData.account"
            placeholder="请输入用户名或邮箱"
            :prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>

        <el-form-item v-if="loginType === 'password'" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <div v-if="loginType === 'code'" class="code-row">
          <el-form-item prop="emailCode">
            <el-input
              v-model="formData.emailCode"
              placeholder="请输入邮箱验证码"
              :prefix-icon="Message"
              size="large"
              maxlength="6"
            />
          </el-form-item>
          <el-button
            class="send-code-btn"
            :disabled="sendCodeDisabled"
            :loading="sendCodeLoading"
            @click="handleSendCode"
          >
            {{ sendCodeText }}
          </el-button>
        </div>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            :disabled="!isFormValid"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-links">
        <router-link to="/auth/forgot-password">忘记密码？</router-link>
        <router-link to="/auth/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { login, sendCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const loginType = ref<'password' | 'code'>('password')
const sendCodeLoading = ref(false)
const sendCodeCountdown = ref(0)

const formData = reactive({
  account: '',
  password: '',
  emailCode: ''
})

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const usernameRegex = /^[a-zA-Z0-9_\u4e00-\u9fa5]{2,16}$/

const validateAccount = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入用户名或邮箱'))
  } else if (emailRegex.test(value) || usernameRegex.test(value)) {
    callback()
  } else {
    callback(new Error('请输入有效的用户名（2-16位字母数字下划线中文）或邮箱'))
  }
}

const validatePassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (value.length < 6) {
    callback(new Error('密码长度不能少于6位'))
  } else {
    callback()
  }
}

const validateEmailCode = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入邮箱验证码'))
  } else if (!/^\d{6}$/.test(value)) {
    callback(new Error('验证码为6位数字'))
  } else {
    callback()
  }
}

const formRules: FormRules = {
  account: [{ validator: validateAccount, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  emailCode: [{ validator: validateEmailCode, trigger: 'blur' }]
}

const isFormValid = computed(() => {
  if (!formData.account) return false
  if (loginType.value === 'password') {
    return formData.password.length >= 6
  } else {
    return /^\d{6}$/.test(formData.emailCode)
  }
})

const sendCodeText = computed(() => {
  if (sendCodeCountdown.value > 0) {
    return `${sendCodeCountdown.value}s后重发`
  }
  return '发送验证码'
})

const sendCodeDisabled = computed(() => {
  if (sendCodeCountdown.value > 0) return true
  if (!formData.account) return true
  if (!emailRegex.test(formData.account)) return true
  return false
})

function switchLoginType(type: 'password' | 'code') {
  loginType.value = type
  formData.password = ''
  formData.emailCode = ''
  formRef.value?.clearValidate()
}

async function handleSendCode() {
  if (!emailRegex.test(formData.account)) {
    ElMessage.warning('请输入有效的邮箱地址')
    return
  }

  sendCodeLoading.value = true
  try {
    await sendCode({
      email: formData.account,
      type: 'login'
    })
    ElMessage.success('验证码已发送到您的邮箱')
    startCountdown()
  } catch {
  } finally {
    sendCodeLoading.value = false
  }
}

function startCountdown() {
  sendCodeCountdown.value = 60
  const timer = setInterval(() => {
    sendCodeCountdown.value--
    if (sendCodeCountdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login({
      account: formData.account,
      password: loginType.value === 'password' ? formData.password : undefined,
      emailCode: loginType.value === 'code' ? formData.emailCode : undefined,
      loginType: loginType.value === 'code' ? 'captcha' : 'password'
    })

    userStore.setUserInfo(res.data)
    
    if (res.data && res.data.token) {
      userStore.setToken(res.data.token)
    }

    ElMessage.success('登录成功')

    const redirect = route.query.redirect as string
    router.push(redirect || '/')
  } catch {
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const savedAccount = localStorage.getItem('lastLoginAccount')
  if (savedAccount) {
    formData.account = savedAccount
  }
})
</script>

<style scoped lang="scss">
@use '@/assets/styles/auth.scss';
</style>
