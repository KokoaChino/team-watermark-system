<template>
  <div class="template-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>水印模板</span>
          <el-button type="primary" @click="handleCreate">
            新建模板
          </el-button>
        </div>
      </template>
      <el-empty v-if="templates.length === 0" description="暂无模板，点击新建模板开始创建" />
      <el-table v-else :data="templates" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="模板名称" />
        <el-table-column prop="createdByUsername" label="创建者" />
        <el-table-column prop="createdAt" label="创建时间">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="row.createdById === userStore.userInfo?.id || isLeader"
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getTeamInfo } from '@/api/team'
import { getTemplateList, deleteTemplate } from '@/api/template'
import type { WatermarkTemplateVO, TeamMemberVO } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const templates = ref<WatermarkTemplateVO[]>([])
const teamInfo = ref<TeamMemberVO | null>(null)

const isLeader = computed(() => teamInfo.value?.role === 'leader')

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function fetchTemplates() {
  try {
    const res = await getTemplateList()
    if (res.code === 200) {
      templates.value = res.data || []
    }
  } catch (error) {
    console.error('获取模板列表失败:', error)
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

function handleCreate() {
  router.push('/template/editor')
}

function handleEdit(template: WatermarkTemplateVO) {
  router.push(`/template/editor?id=${template.id}`)
}

async function handleDelete(template: WatermarkTemplateVO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板 "${template.name}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteTemplate(template.id)
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
