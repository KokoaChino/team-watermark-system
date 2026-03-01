<template>
  <div class="template-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>模板列表</span>
          <el-button type="primary" @click="handleCreateNew">新建模板</el-button>
        </div>
      </template>
      
      <el-table :data="templates" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="模板名称" min-width="150" />
        <el-table-column label="尺寸" width="120">
          <template #default="{ row }">
            {{ row.config?.baseConfig?.width }} × {{ row.config?.baseConfig?.height }}
          </template>
        </el-table-column>
        <el-table-column label="水印数" width="80">
          <template #default="{ row }">
            {{ row.config?.watermarks?.length || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="createdByUsername" label="创建人" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="canDelete(row)"
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="!loading && templates.length === 0" description="暂无模板" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTemplateList, deleteTemplate, createDraftFromTemplate, createNewDraft } from '@/api/template'
import { getTeamInfo } from '@/api/team'
import { useUserStore } from '@/stores/user'
import type { WatermarkTemplateVO } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const templates = ref<WatermarkTemplateVO[]>([])
const loading = ref(false)
const teamInfo = ref<{ role?: string } | null>(null)

const isLeader = computed(() => teamInfo.value?.role === 'leader')

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

function canDelete(row: WatermarkTemplateVO) {
  return isLeader.value || row.createdById === userStore.userInfo?.id
}

async function fetchTemplates() {
  loading.value = true
  try {
    const res = await getTemplateList()
    if (res.code === 200) {
      templates.value = res.data || []
    }
  } catch (error) {
    console.error('获取模板列表失败:', error)
  } finally {
    loading.value = false
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

async function handleCreateNew() {
  try {
    const res = await createNewDraft()
    if (res.code === 200) {
      router.push('/template/draft')
    }
  } catch (error) {
    console.error('创建草稿失败:', error)
  }
}

async function handleEdit(row: WatermarkTemplateVO) {
  try {
    const res = await createDraftFromTemplate(row.id)
    if (res.code === 200) {
      router.push('/template/draft')
    }
  } catch (error) {
    console.error('创建草稿失败:', error)
  }
}

async function handleDelete(row: WatermarkTemplateVO) {
  try {
    await ElMessageBox.confirm(`确定要删除模板 "${row.name}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTemplate(row.id)
    ElMessage.success('模板已删除')
    fetchTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除模板失败:', error)
    }
  }
}

onMounted(() => {
  fetchTemplates()
  fetchTeamInfo()
})
</script>

<style scoped lang="scss">
.template-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
