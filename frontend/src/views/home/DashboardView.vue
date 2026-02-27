<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon points">
              <el-icon><Coin /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ teamInfo?.pointBalance || 0 }}</div>
              <div class="stat-label">团队点数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon members">
              <el-icon><UserFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ teamInfo?.members?.length || 0 }}</div>
              <div class="stat-label">团队成员</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon role">
              <el-icon><Medal /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ teamInfo?.role === 'leader' ? '队长' : '成员' }}</div>
              <div class="stat-label">我的角色</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon team">
              <el-icon><OfficeBuilding /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ teamInfo?.teamName }}</div>
              <div class="stat-label">团队名称</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="quick-actions">
            <div class="action-item" @click="router.push('/template')">
              <div class="action-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="action-text">水印模板</div>
            </div>
            <div class="action-item" @click="router.push('/task/create')">
              <div class="action-icon">
                <el-icon><Files /></el-icon>
              </div>
              <div class="action-text">创建任务</div>
            </div>
            <div class="action-item" @click="router.push('/team/invite')">
              <div class="action-icon">
                <el-icon><Share /></el-icon>
              </div>
              <div class="action-text">邀请成员</div>
            </div>
            <div class="action-item" @click="router.push('/payment')">
              <div class="action-icon">
                <el-icon><Wallet /></el-icon>
              </div>
              <div class="action-text">点数充值</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>团队成员</span>
              <el-button
                v-if="teamInfo?.role === 'leader'"
                type="primary"
                size="small"
                @click="router.push('/team/invite')"
              >
                邀请成员
              </el-button>
            </div>
          </template>
          <el-table :data="teamInfo?.members" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag :type="row.id === teamInfo?.leaderId ? 'primary' : 'info'">
                  {{ row.id === teamInfo?.leaderId ? '队长' : '成员' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTeamInfo } from '@/api/team'
import type { TeamMemberVO } from '@/types'
import {
  Coin,
  UserFilled,
  Medal,
  OfficeBuilding,
  Document,
  Files,
  Share,
  Wallet
} from '@element-plus/icons-vue'

const router = useRouter()
const teamInfo = ref<TeamMemberVO | null>(null)

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

onMounted(() => {
  fetchTeamInfo()
})
</script>

<style scoped lang="scss">
.dashboard {
  .stat-card {
    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;

      .stat-icon {
        width: 56px;
        height: 56px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;

        &.points {
          background-color: #fff7e6;
          color: #e6a23c;
        }

        &.members {
          background-color: #e6f7ff;
          color: #1890ff;
        }

        &.role {
          background-color: #f6ffed;
          color: #52c41a;
        }

        &.team {
          background-color: #f3e8ff;
          color: #722ed1;
        }
      }

      .stat-info {
        .stat-value {
          font-size: 24px;
          font-weight: 600;
          color: var(--color-text-primary);
        }

        .stat-label {
          font-size: 13px;
          color: var(--color-text-secondary);
          margin-top: 4px;
        }
      }
    }
  }

  .quick-actions {
    display: flex;
    gap: 24px;

    .action-item {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 24px;
      border-radius: 12px;
      background-color: var(--color-bg-page);
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        background-color: var(--color-primary-lighter);
        transform: translateY(-4px);
      }

      .action-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        background-color: var(--color-primary);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
      }

      .action-text {
        font-size: 14px;
        color: var(--color-text-primary);
      }
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
