<template>
  <div class="home-container">
    <div class="home-header">
      <h1>批量图片水印协作平台</h1>
      <p>欢迎使用，{{ userStore.userInfo?.username }}</p>
      <div class="button-group">
        <el-button type="primary" @click="handleLogout">退出登录</el-button>
        <el-button type="danger" plain @click="handleUnregister">注销账户</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { logout, unregister } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

async function handleLogout() {
  try {
    await logout()
  } catch {
  } finally {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/auth/login')
  }
}

async function handleUnregister() {
  try {
    await ElMessageBox.confirm(
      '注销账户将永久删除您的账户及所有关联数据，此操作不可恢复。是否继续？',
      '警告',
      {
        confirmButtonText: '确认注销',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await ElMessageBox.confirm(
      '请再次确认：您确定要注销账户吗？',
      {
        confirmButtonText: '确定注销',
        cancelButtonText: '取消',
        type: 'error'
      }
    )
    
    await unregister()
    userStore.logout()
    ElMessage.success('账户已注销')
    router.push('/auth/login')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('注销失败:', error)
    }
  }
}
</script>

<style scoped lang="scss">
.home-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-page);
}

.home-header {
  text-align: center;
  
  h1 {
    font-size: 28px;
    color: var(--color-text-primary);
    margin-bottom: 16px;
  }
  
  p {
    font-size: 16px;
    color: var(--color-text-secondary);
    margin-bottom: 24px;
  }
  
  .button-group {
    display: flex;
    gap: 12px;
    justify-content: center;
  }
}
</style>
