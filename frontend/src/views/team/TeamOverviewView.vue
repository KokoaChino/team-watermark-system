<template>
  <div class="team-overview">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="team-info-card">
          <template #header>
            <div class="card-header">
              <span>团队信息</span>
              <div class="header-actions">
                <el-button
                  v-if="isLeader"
                  type="primary"
                  @click="showEditNameDialog = true"
                >
                  修改团队名称
                </el-button>
                <el-button
                  type="success"
                  @click="handleRecharge"
                >
                  充值点数
                </el-button>
              </div>
            </div>
          </template>
          <div class="team-info">
            <div class="info-item">
              <span class="label">团队名称</span>
              <span class="value">{{ teamInfo?.teamName }}</span>
            </div>
            <div class="info-item">
              <span class="label">团队点数</span>
              <span class="value points">{{ teamInfo?.pointBalance || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="label">团队人数</span>
              <span class="value">{{ teamInfo?.members?.length || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="label">队长</span>
              <span class="value">{{ teamInfo?.leaderName }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card class="members-card">
          <template #header>
            <div class="card-header">
              <span>团队成员</span>
              <el-button
                v-if="isLeader"
                type="success"
                @click="router.push('/team/invite')"
              >
                邀请成员
              </el-button>
            </div>
          </template>
          <el-table :data="teamInfo?.members" style="width: 100%">
            <el-table-column prop="username" label="用户名" min-width="150" />
            <el-table-column prop="email" label="邮箱" min-width="200" />
            <el-table-column label="角色" min-width="80">
              <template #default="{ row }">
                <el-tag :type="row.role === 'leader' ? 'primary' : 'info'" size="small">
                  {{ row.role === 'leader' ? '队长' : '成员' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="加入时间" min-width="160">
              <template #default="{ row }">
                {{ formatDate(row.joinedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="180">
              <template #default="{ row }">
                <template v-if="row.id === userStore.userInfo?.id">
                  <el-button
                    v-if="!isLeader"
                    type="warning"
                    size="small"
                    @click="handleLeaveTeam"
                  >
                    退出团队
                  </el-button>
                  <el-button
                    type="info"
                    size="small"
                    @click="showJoinTeamDialog = true"
                  >
                    加入其他团队
                  </el-button>
                </template>
                <template v-else-if="isLeader">
                  <el-button
                    type="danger"
                    size="small"
                    @click="handleKick(row)"
                  >
                    踢出
                  </el-button>
                  <el-button
                    type="warning"
                    size="small"
                    @click="handleTransferLeader(row)"
                  >
                    转让队长
                  </el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="showEditNameDialog"
      title="修改团队名称"
      width="400px"
    >
      <el-form :model="editNameForm" label-width="80px">
        <el-form-item label="团队名称">
          <el-input v-model="editNameForm.name" placeholder="请输入新团队名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditNameDialog = false">取消</el-button>
        <el-button type="primary" :loading="editNameLoading" @click="handleUpdateName">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showJoinTeamDialog"
      title="加入团队"
      width="450px"
    >
      <el-form :model="joinForm" label-width="100px">
        <el-form-item label="邀请码文本">
          <el-input
            v-model="joinForm.inviteCodeText"
            type="textarea"
            :rows="3"
            placeholder="请粘贴邀请码文本，支持【】包装的邀请码"
          />
        </el-form-item>
        <el-form-item label="转移点数">
          <el-checkbox v-model="joinForm.transferPoints">
            退出原团队时，将剩余点数转移到新团队
          </el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showJoinTeamDialog = false">取消</el-button>
        <el-button type="primary" :loading="joinLoading" @click="handleJoinTeam">加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getTeamInfo,
  updateTeamName,
  kickMember,
  transferLeader,
  leaveTeam,
  joinTeam
} from '@/api/team'
import type { TeamMemberVO, UserVO } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const teamInfo = ref<TeamMemberVO | null>(null)
const showEditNameDialog = ref(false)
const showJoinTeamDialog = ref(false)
const editNameLoading = ref(false)
const joinLoading = ref(false)
const editNameForm = ref({
  name: ''
})
const joinForm = ref({
  inviteCodeText: '',
  transferPoints: true
})

const isLeader = computed(() => teamInfo.value?.role === 'leader')

function formatDate(dateStr: string | undefined) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
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

async function handleUpdateName() {
  if (!editNameForm.value.name.trim()) {
    ElMessage.warning('请输入团队名称')
    return
  }
  editNameLoading.value = true
  try {
    const res = await updateTeamName({ name: editNameForm.value.name })
    if (res.code === 200) {
      ElMessage.success('团队名称已更新')
      showEditNameDialog.value = false
      teamInfo.value = res.data
    }
  } catch (error) {
    console.error('更新团队名称失败:', error)
  } finally {
    editNameLoading.value = false
  }
}

async function handleKick(member: UserVO) {
  try {
    await ElMessageBox.confirm(
      `确定要踢出成员 “${member.username}” 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await kickMember({ userId: member.id })
    ElMessage.success('已踢出成员')
    fetchTeamInfo()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('踢出成员失败:', error)
    }
  }
}

async function handleTransferLeader(member: UserVO) {
  try {
    await ElMessageBox.confirm(
      `确定要将队长身份转让给 “${member.username}” 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await transferLeader({ newLeaderId: member.id })
    ElMessage.success('已转让队长身份')
    fetchTeamInfo()
    setTimeout(() => {
      router.go(0)
    }, 500)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('转让队长失败:', error)
    }
  }
}

async function handleLeaveTeam() {
  try {
    await ElMessageBox.confirm(
      '确定要退出当前团队吗？退出后将自动创建个人团队。',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await leaveTeam()
    ElMessage.success('已退出团队')
    fetchTeamInfo()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出团队失败:', error)
    }
  }
}

async function handleJoinTeam() {
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
      showJoinTeamDialog.value = false
      joinForm.value.inviteCodeText = ''
      fetchTeamInfo()
    }
  } catch (error) {
    console.error('加入团队失败:', error)
  } finally {
    joinLoading.value = false
  }
}

function handleRecharge() {
  const layout = document.querySelector('.main-layout')
  if (layout) {
    const openPaymentDialog = (layout as any).__vueParentComponent?.exposed?.openPaymentDialog
    if (openPaymentDialog) {
      openPaymentDialog()
    }
  }
}

onMounted(() => {
  fetchTeamInfo()
})
</script>

<style scoped lang="scss">
.team-overview {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .team-info {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;

    .info-item {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .label {
        font-size: 13px;
        color: var(--color-text-secondary);
      }

      .value {
        font-size: 16px;
        font-weight: 500;
        color: var(--color-text-primary);

        &.points {
          color: #e6a23c;
        }
      }
    }
  }
}
</style>
