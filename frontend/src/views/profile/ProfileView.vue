<template>
  <div class="profile-view">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>个人信息</span>
          </template>
          <el-form :model="profileForm" label-width="80px">
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input :model-value="userStore.userInfo?.email" disabled />
              <el-button size="small" style="margin-top: 8px" @click="showEmailDialog = true">
                修改邮箱
              </el-button>
            </el-form-item>
            <el-form-item label="密码">
              <el-button type="primary" plain @click="showPasswordDialog = true">
                修改密码
              </el-button>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSaveProfile">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>团队信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="团队名称">
              {{ teamInfo?.teamName }}
            </el-descriptions-item>
            <el-descriptions-item label="团队点数">
              <span style="color: #e6a23c; font-weight: 600">{{ teamInfo?.pointBalance }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="我的角色">
              <el-tag :type="teamInfo?.role === 'leader' ? 'primary' : 'info'">
                {{ teamInfo?.role === 'leader' ? '队长' : '成员' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="队长">
              {{ teamInfo?.leaderName }}
            </el-descriptions-item>
          </el-descriptions>
          <div style="margin-top: 16px">
            <el-button type="primary" @click="router.push('/team')">查看团队详情</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showEmailDialog" title="修改邮箱" width="400px">
      <el-form :model="emailForm" label-width="80px">
        <el-form-item label="新邮箱">
          <el-input v-model="emailForm.email" placeholder="请输入新邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div style="display: flex; gap: 8px">
            <el-input v-model="emailForm.code" placeholder="请输入验证码" style="flex: 1" />
            <el-button :disabled="emailCountdown > 0" @click="handleSendEmailCode">
              {{ emailCountdown > 0 ? `${emailCountdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEmailDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateEmail">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showPasswordDialog" title="修改密码" width="400px">
      <el-form :model="passwordForm" label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="验证码">
          <div style="display: flex; gap: 8px">
            <el-input v-model="passwordForm.code" placeholder="请输入验证码" style="flex: 1" />
            <el-button :disabled="passwordCountdown > 0" @click="handleSendPasswordCode">
              {{ passwordCountdown > 0 ? `${passwordCountdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdatePassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getTeamInfo } from '@/api/team'
import { updateProfile } from '@/api/user'
import { sendCode } from '@/api/auth'
import type { TeamMemberVO } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const teamInfo = ref<TeamMemberVO | null>(null)
const profileForm = ref({
  username: ''
})
const saving = ref(false)
const showEmailDialog = ref(false)
const showPasswordDialog = ref(false)
const emailCountdown = ref(0)
const passwordCountdown = ref(0)
const emailForm = ref({
  email: '',
  code: ''
})
const passwordForm = ref({
  newPassword: '',
  code: ''
})

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

async function handleSaveProfile() {
  if (!profileForm.value.username.trim()) {
    ElMessage.warning('用户名不能为空')
    return
  }
  saving.value = true
  try {
    await updateProfile({ username: profileForm.value.username })
    userStore.userInfo!.username = profileForm.value.username
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}

async function handleSendEmailCode() {
  if (!emailForm.value.email) {
    ElMessage.warning('请输入新邮箱')
    return
  }
  await sendCode({ email: emailForm.value.email, type: 'update_email' })
  ElMessage.success('验证码已发送')
  emailCountdown.value = 60
  const timer = setInterval(() => {
    emailCountdown.value--
    if (emailCountdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

async function handleUpdateEmail() {
  if (!emailForm.value.email || !emailForm.value.code) {
    ElMessage.warning('请填写完整信息')
    return
  }
  ElMessage.info('修改邮箱功能开发中')
}

async function handleSendPasswordCode() {
  if (!userStore.userInfo?.email) {
    ElMessage.warning('未获取到用户邮箱')
    return
  }
  await sendCode({ email: userStore.userInfo.email, type: 'update_password' })
  ElMessage.success('验证码已发送')
  passwordCountdown.value = 60
  const timer = setInterval(() => {
    passwordCountdown.value--
    if (passwordCountdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

async function handleUpdatePassword() {
  if (!passwordForm.value.newPassword || !passwordForm.value.code) {
    ElMessage.warning('请填写完整信息')
    return
  }
  ElMessage.info('修改密码功能开发中')
}

onMounted(() => {
  if (userStore.userInfo) {
    profileForm.value.username = userStore.userInfo.username
  }
  fetchTeamInfo()
})
</script>

<style scoped lang="scss">
.profile-view {
}
</style>
