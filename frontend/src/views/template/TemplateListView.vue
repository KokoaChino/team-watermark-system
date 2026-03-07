<template>
  <div class="template-list">
    <TemplateBrowser ref="browserRef" title="模板列表">
      <template #header-actions>
        <el-button type="primary" @click="handleCreateNew">
          <el-icon><Plus /></el-icon>
          新建模板
        </el-button>
      </template>

      <template #item-actions="{ item }">
        <el-button type="primary" size="small" @click.stop="handleEdit(item)">
          <el-icon><Edit /></el-icon>
          修改
        </el-button>
        <el-button
          v-if="canDelete(item)"
          type="danger"
          size="small"
          @click.stop="handleDelete(item)"
        >
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
      </template>
    </TemplateBrowser>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import TemplateBrowser from '@/components/template/TemplateBrowser.vue'
import { createDraftFromTemplate, createNewDraft, deleteTemplate } from '@/api/template'
import { getTeamInfo } from '@/api/team'
import { useUserStore } from '@/stores/user'
import type { WatermarkTemplateVO } from '@/types'
import { useRouter } from 'vue-router'

const router = useRouter()
const userStore = useUserStore()
const browserRef = ref<InstanceType<typeof TemplateBrowser> | null>(null)
const teamInfo = ref<{ role?: string } | null>(null)

const isLeader = computed(() => teamInfo.value?.role === 'leader')

function canDelete(row: WatermarkTemplateVO) {
  return isLeader.value || row.createdById === userStore.userInfo?.id
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
    const res = await createNewDraft(false)
    if (res.code === 200) {
      if (res.data.hasConflict) {
        try {
          await ElMessageBox.confirm(
            res.data.conflictMessage || '当前存在未提交的草稿，继续将覆盖之前的编辑内容。',
            '提示',
            {
              confirmButtonText: '继续',
              cancelButtonText: '取消',
              type: 'warning'
            }
          )
          await createNewDraft(true)
          router.push('/template/draft')
        } catch {
          // 用户取消
        }
      } else {
        router.push('/template/draft')
      }
    }
  } catch (error) {
    console.error('创建草稿失败:', error)
  }
}

async function handleEdit(row: WatermarkTemplateVO) {
  try {
    const res = await createDraftFromTemplate(row.id, false)
    if (res.code === 200) {
      if (res.data.hasConflict) {
        try {
          await ElMessageBox.confirm(
            res.data.conflictMessage || '当前存在未提交的草稿，继续将覆盖之前的编辑内容。',
            '提示',
            {
              confirmButtonText: '继续',
              cancelButtonText: '取消',
              type: 'warning'
            }
          )
          await createDraftFromTemplate(row.id, true)
          router.push('/template/draft')
        } catch {
          // 用户取消
        }
      } else {
        router.push('/template/draft')
      }
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
    await browserRef.value?.refreshTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除模板失败:', error)
    }
  }
}

onMounted(() => {
  fetchTeamInfo()
})
</script>

<style scoped lang="scss">
.template-list {
  display: flex;
  flex-direction: column;
}
</style>
