<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <h1 class="auth-title">创建账户</h1>
        <p class="auth-subtitle">注册新账户开始使用</p>
      </div>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        class="auth-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名（4-16位）"
            :prefix-icon="User"
            size="large"
            clearable
            maxlength="16"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="formData.email"
            placeholder="请输入邮箱地址"
            :prefix-icon="Message"
            size="large"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码（6-16位）"
            :prefix-icon="Lock"
            size="large"
            show-password
            maxlength="16"
            @input="checkPasswordStrength"
          />
        </el-form-item>

        <div v-if="formData.password" class="password-strength">
          <div class="strength-bar">
            <div class="bar" :class="passwordStrength >= 1 ? strengthClass : ''"></div>
            <div class="bar" :class="passwordStrength >= 2 ? strengthClass : ''"></div>
            <div class="bar" :class="passwordStrength >= 3 ? strengthClass : ''"></div>
            <div class="bar" :class="passwordStrength >= 4 ? strengthClass : ''"></div>
          </div>
          <span class="strength-text">密码强度：{{ strengthText }}</span>
        </div>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="formData.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            maxlength="16"
          />
        </el-form-item>

        <div class="captcha-row">
          <el-form-item prop="captcha">
            <el-input
              v-model="formData.captcha"
              placeholder="请输入图形验证码"
              :prefix-icon="Picture"
              size="large"
              maxlength="4"
              clearable
            />
          </el-form-item>
          <div class="captcha-image" @click="refreshCaptcha">
            <img v-if="captchaBase64" :src="captchaBase64" alt="验证码" />
            <el-icon v-else class="is-loading"><Loading /></el-icon>
          </div>
        </div>

        <div class="code-row">
          <el-form-item prop="emailCode">
            <el-input
              v-model="formData.emailCode"
              placeholder="请输入邮箱验证码"
              :prefix-icon="Key"
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
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        已有账户？<router-link to="/auth/login">立即登录</router-link>
      </div>

      <div class="form-tips">
        <p>新用户注册将获得 10000 点免费操作点数</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Picture, Loading, Key } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { register, sendCode, getCaptcha } from '@/api/auth'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const sendCodeLoading = ref(false)
const sendCodeCountdown = ref(0)
const captchaKey = ref('')
const captchaBase64 = ref('')
const passwordStrength = ref(0)

const formData = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  captcha: '',
  emailCode: ''
})

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const usernameRegex = /^[a-zA-Z0-9_\u4e00-\u9fa5]{4,16}$/

const validateUsername = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入用户名'))
  } else if (!usernameRegex.test(value)) {
    callback(new Error('用户名为4-16位字母、数字、下划线或中文'))
  } else {
    callback()
  }
}

const validateEmail = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入邮箱地址'))
  } else if (!emailRegex.test(value)) {
    callback(new Error('请输入有效的邮箱地址'))
  } else {
    callback()
  }
}

const validatePassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (value.length < 6 || value.length > 16) {
    callback(new Error('密码长度需在6-16位之间'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== formData.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateCaptcha = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入图形验证码'))
  } else if (value.length < 4 || value.length > 6) {
    callback(new Error('验证码为4-6位'))
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
  username: [{ validator: validateUsername, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  captcha: [{ validator: validateCaptcha, trigger: 'blur' }],
  emailCode: [{ validator: validateEmailCode, trigger: 'blur' }]
}

const isFormValid = computed(() => {
  return (
    usernameRegex.test(formData.username) &&
    emailRegex.test(formData.email) &&
    formData.password.length >= 6 &&
    formData.password.length <= 16 &&
    formData.password === formData.confirmPassword &&
    formData.captcha.length >= 4 &&
    /^\d{6}$/.test(formData.emailCode)
  )
})

const sendCodeText = computed(() => {
  if (sendCodeCountdown.value > 0) {
    return `${sendCodeCountdown.value}s后重发`
  }
  return '发送验证码'
})

const sendCodeDisabled = computed(() => {
  if (sendCodeCountdown.value > 0) return true
  if (!emailRegex.test(formData.email)) return true
  if (!formData.captcha || formData.captcha.length < 4) return true
  return false
})

const strengthClass = computed(() => {
  if (passwordStrength.value <= 1) return 'weak'
  if (passwordStrength.value <= 2) return 'medium'
  return 'strong'
})

const strengthText = computed(() => {
  if (passwordStrength.value <= 1) return '弱'
  if (passwordStrength.value <= 2) return '中'
  if (passwordStrength.value <= 3) return '较强'
  return '强'
})

function checkPasswordStrength() {
  let strength = 0
  const password = formData.password
  
  if (password.length >= 6) strength++
  if (password.length >= 10) strength++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++
  if (/\d/.test(password)) strength++
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) strength++
  
  passwordStrength.value = Math.min(4, strength)
}

async function refreshCaptcha() {
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data.key
    captchaBase64.value = res.data.base64
  } catch {
    ElMessage.error('获取验证码失败，请重试')
  }
}

async function handleSendCode() {
  if (!emailRegex.test(formData.email)) {
    ElMessage.warning('请输入有效的邮箱地址')
    return
  }

  if (!formData.captcha || formData.captcha.length < 4) {
    ElMessage.warning('请先输入图形验证码')
    return
  }

  sendCodeLoading.value = true
  try {
    await sendCode({
      email: formData.email,
      type: 'register'
    })
    ElMessage.success('验证码已发送到您的邮箱')
    startCountdown()
  } catch {
    refreshCaptcha()
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

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      username: formData.username,
      password: formData.password,
      email: formData.email,
      captcha: formData.captcha,
      captchaKey: captchaKey.value,
      emailCode: formData.emailCode
    })

    ElMessage.success('注册成功，请登录')
    router.push('/auth/login')
  } catch {
    refreshCaptcha()
    formData.captcha = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped lang="scss">
@use '@/assets/styles/auth.scss';
</style>
