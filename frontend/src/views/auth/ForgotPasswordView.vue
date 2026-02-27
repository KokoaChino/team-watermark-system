<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <h1 class="auth-title">找回密码</h1>
        <p class="auth-subtitle">通过邮箱验证重置您的密码</p>
      </div>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        class="auth-form"
        @submit.prevent="handleResetPassword"
      >
        <el-form-item prop="email">
          <el-input
            v-model="formData.email"
            placeholder="请输入注册时绑定的邮箱"
            :prefix-icon="Message"
            size="large"
            clearable
          />
        </el-form-item>

        <div class="code-row">
          <el-form-item prop="code">
            <el-input
              v-model="formData.code"
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

        <el-form-item prop="newPassword">
          <el-input
            v-model="formData.newPassword"
            type="password"
            placeholder="请输入新密码（6-16位）"
            :prefix-icon="Lock"
            size="large"
            show-password
            maxlength="16"
            @input="checkPasswordStrength"
          />
        </el-form-item>

        <div v-if="formData.newPassword" class="password-strength">
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
            placeholder="请再次输入新密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            maxlength="16"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            :disabled="!isFormValid"
            @click="handleResetPassword"
          >
            重置密码
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        <router-link to="/auth/login">返回登录</router-link>
      </div>

      <div class="form-tips">
        <p>我们将向您的邮箱发送验证码，用于验证身份</p>
        <p>验证码有效期为 5 分钟，请及时查收</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Message, Lock, Key } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { forgotPassword, sendCode } from '@/api/auth'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const sendCodeLoading = ref(false)
const sendCodeCountdown = ref(0)
const passwordStrength = ref(0)

const formData = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const validateEmail = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入邮箱地址'))
  } else if (!emailRegex.test(value)) {
    callback(new Error('请输入有效的邮箱地址'))
  } else {
    callback()
  }
}

const validateCode = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入验证码'))
  } else if (!/^\d{6}$/.test(value)) {
    callback(new Error('验证码为6位数字'))
  } else {
    callback()
  }
}

const validateNewPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入新密码'))
  } else if (value.length < 6 || value.length > 16) {
    callback(new Error('密码长度需在6-16位之间'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== formData.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const formRules: FormRules = {
  email: [{ validator: validateEmail, trigger: 'blur' }],
  code: [{ validator: validateCode, trigger: 'blur' }],
  newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const isFormValid = computed(() => {
  return (
    emailRegex.test(formData.email) &&
    /^\d{6}$/.test(formData.code) &&
    formData.newPassword.length >= 6 &&
    formData.newPassword.length <= 16 &&
    formData.newPassword === formData.confirmPassword
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
  const password = formData.newPassword
  
  if (password.length >= 6) strength++
  if (password.length >= 10) strength++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++
  if (/\d/.test(password)) strength++
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) strength++
  
  passwordStrength.value = Math.min(4, strength)
}

async function handleSendCode() {
  if (!emailRegex.test(formData.email)) {
    ElMessage.warning('请输入有效的邮箱地址')
    return
  }

  sendCodeLoading.value = true
  try {
    await sendCode({
      email: formData.email,
      type: 'forgot_password'
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

async function handleResetPassword() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await forgotPassword({
      email: formData.email,
      code: formData.code,
      newPassword: formData.newPassword
    })

    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/auth/login')
  } catch {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
@use '@/assets/styles/auth.scss';
</style>
