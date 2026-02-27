<template>
  <div class="team-invite">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>邀请码管理</span>
          <el-button type="primary" @click="showGenerateDialog = true">
            生成邀请码
          </el-button>
        </div>
      </template>
      <el-table :data="inviteCodes" v-loading="loading" style="width: 100%">
        <el-table-column prop="code" label="邀请码" min-width="120" />
        <el-table-column label="分享文本" min-width="300">
          <template #default="{ row }">
            <span 
              class="share-text" 
              @click="handleCopy(row.shareText)"
              title="点击复制"
            >
              {{ row.shareText }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="140">
          <template #default="{ row }">
            {{ row.validUntil ? formatDate(row.validUntil) : '永久有效' }}
          </template>
        </el-table-column>
        <el-table-column label="使用次数" min-width="100">
          <template #default="{ row }">
            {{ row.usesCount }} / {{ row.maxUses || '∞' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
              {{ row.status === 'active' ? '有效' : '已失效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180">
          <template #default="{ row }">
            <el-button
              type="warning"
              size="small"
              @click="showRecordsDialog = true; fetchRecords(row.id)"
            >
              记录
            </el-button>
            <el-button
              v-if="row.status === 'active'"
              type="danger"
              size="small"
              @click="handleDeactivate(row)"
            >
              失效
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="showGenerateDialog"
      title="生成邀请码"
      width="450px"
      @close="handleGenerateDialogClose"
    >
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="有效期">
          <el-date-picker
            v-model="generateForm.validUntil"
            type="datetime"
            placeholder="选择有效期（不选则永久有效）"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            :shortcuts="dateShortcuts"
            :show-now="false"
          />
        </el-form-item>
        <el-form-item label="最大使用次数">
          <el-input-number
            v-model="generateForm.maxUses"
            :min="1"
            :max="100"
            placeholder="不填则无限制"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGenerateDialog = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="handleGenerate">
          生成
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="showRecordsDialog"
      title="邀请记录"
      size="400px"
    >
      <template v-if="inviteRecords.length > 0">
        <el-table :data="inviteRecords" v-loading="recordsLoading" style="width: 100%">
          <el-table-column prop="username" label="被邀请人" />
          <el-table-column label="加入时间">
            <template #default="{ row }">
              {{ formatDate(row.joinedAt) }}
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else description="暂无邀请记录" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getInviteCodes,
  generateInviteCode,
  deactivateInviteCode,
  getInviteRecords
} from '@/api/team'
import type { InviteCodeVO, InviteRecordVO } from '@/types'

const loading = ref(false)
const generateLoading = ref(false)
const recordsLoading = ref(false)
const inviteCodes = ref<InviteCodeVO[]>([])
const inviteRecords = ref<InviteRecordVO[]>([])
const showGenerateDialog = ref(false)
const showRecordsDialog = ref(false)

const generateForm = ref({
  validUntil: '',
  maxUses: undefined as number | undefined
})

const dateShortcuts = [
  { text: '1 小时', value: () => { const d = new Date(); d.setHours(d.getHours() + 1); return d } },
  { text: '3 小时', value: () => { const d = new Date(); d.setHours(d.getHours() + 3); return d } },
  { text: '6 小时', value: () => { const d = new Date(); d.setHours(d.getHours() + 6); return d } },
  { text: '12 小时', value: () => { const d = new Date(); d.setHours(d.getHours() + 12); return d } },
  { text: '1 天', value: () => { const d = new Date(); d.setDate(d.getDate() + 1); return d } },
  { text: '7 天', value: () => { const d = new Date(); d.setDate(d.getDate() + 7); return d } },
  { text: '30 天', value: () => { const d = new Date(); d.setDate(d.getDate() + 30); return d } }
]

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

async function fetchInviteCodes() {
  loading.value = true
  try {
    const res = await getInviteCodes()
    if (res.code === 200) {
      inviteCodes.value = res.data || []
    }
  } catch (error) {
    console.error('获取邀请码列表失败:', error)
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  generateLoading.value = true
  try {
    const data: { validUntil?: string; maxUses?: number } = {}
    if (generateForm.value.validUntil) {
      data.validUntil = generateForm.value.validUntil
    }
    if (generateForm.value.maxUses) {
      data.maxUses = generateForm.value.maxUses
    }
    const res = await generateInviteCode(data)
    if (res.code === 200) {
      ElMessage.success('邀请码生成成功')
      showGenerateDialog.value = false
      fetchInviteCodes()
    }
  } catch (error) {
    console.error('生成邀请码失败:', error)
  } finally {
    generateLoading.value = false
  }
}

function handleGenerateDialogClose() {
  generateForm.value = { validUntil: '', maxUses: undefined }
}

function handleCopy(text: string) {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
}

async function fetchRecords(codeId: number) {
  recordsLoading.value = true
  inviteRecords.value = []
  try {
    const res = await getInviteRecords(codeId)
    if (res.code === 200) {
      inviteRecords.value = res.data || []
    }
  } catch (error) {
    console.error('获取邀请记录失败:', error)
  } finally {
    recordsLoading.value = false
  }
}

async function handleDeactivate(code: InviteCodeVO) {
  try {
    await ElMessageBox.confirm(
      '确定要让该邀请码失效吗？失效后他人将无法使用。',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deactivateInviteCode(code.id)
    ElMessage.success('邀请码已失效')
    fetchInviteCodes()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('失效邀请码失败:', error)
    }
  }
}

onMounted(() => {
  fetchInviteCodes()
})
</script>

<style scoped lang="scss">
.team-invite {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .share-text {
    cursor: pointer;
    color: #409eff;
    transition: color 0.3s;

    &:hover {
      color: #66b1ff;
      text-decoration: underline;
    }
  }
}
</style>
