<template>
  <div class="team-join">
    <el-card>
      <template #header>
        <span>加入团队</span>
      </template>
      <el-form :model="joinForm" label-width="120px">
        <el-form-item label="邀请码文本">
          <el-input
            v-model="joinForm.inviteCodeText"
            type="textarea"
            :rows="3"
            placeholder="请粘贴邀请码文本，支持【】包装的邀请码，如：快来加入XX团队【ABC123】"
          />
        </el-form-item>
        <el-form-item label="转移点数">
          <el-checkbox v-model="joinForm.transferPoints">
            退出原团队时，将剩余点数转移到新团队
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="joinLoading"
            @click="handleJoin"
          >
            加入团队
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { joinTeam } from '@/api/team'

const router = useRouter()

const joinLoading = ref(false)
const joinForm = ref({
  inviteCodeText: '',
  transferPoints: true
})

async function handleJoin() {
  if (!joinForm.value.inviteCodeText.trim()) {
    ElMessage.warning('请输入邀请码文本')
    return
  }
  joinLoading.value = true
  try {
    const res = await joinTeam({
      inviteCodeText: joinForm.value.inviteCodeText,
      transferPoints: joinForm.value.transferPoints
    })
    if (res.code === 200) {
      ElMessage.success('加入团队成功')
      router.push('/team')
    }
  } catch (error) {
    console.error('加入团队失败:', error)
  } finally {
    joinLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.team-join {
  max-width: 600px;
}
</style>
