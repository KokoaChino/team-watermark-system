<template>
  <div class="task-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h3>批量任务执行</h3>
            <p>这里先承接创建页暂存的任务数据，下一阶段再接真正的执行流程。</p>
          </div>
          <el-button type="primary" @click="router.push('/task/create')">
            创建批量任务
          </el-button>
        </div>
      </template>

      <template v-if="pendingDraft">
        <el-alert
          type="success"
          show-icon
          :closable="false"
          title="已接收到创建页暂存的任务配置，可以继续补充执行逻辑。"
          class="draft-alert"
        />

        <el-descriptions :column="4" border class="draft-summary">
          <el-descriptions-item label="模板名称">{{ pendingDraft.templateName }}</el-descriptions-item>
          <el-descriptions-item label="模板版本">v{{ pendingDraft.templateVersion }}</el-descriptions-item>
          <el-descriptions-item label="图片数量">{{ pendingDraft.items.length }}</el-descriptions-item>
          <el-descriptions-item label="暂存时间">{{ formatDateTime(pendingDraft.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <div class="draft-actions">
          <el-button @click="router.push('/task/create')">返回继续编辑</el-button>
          <el-button type="danger" plain @click="clearPendingDraft">清空暂存</el-button>
        </div>

        <el-table :data="pendingDraft.items" border stripe class="draft-table">
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="imageId" label="图片 ID" min-width="140" />
          <el-table-column prop="sourceFileName" label="原文件名" min-width="220" />
          <el-table-column label="已配置水印" width="140">
            <template #default="{ row }">
              {{ countConfiguredWatermarks(row) }}/{{ row.watermarkInputs.length }}
            </template>
          </el-table-column>
          <el-table-column prop="targetDirectory" label="新的路径" min-width="180">
            <template #default="{ row }">
              {{ row.targetDirectory || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="outputFileName" label="新的文件名" min-width="160">
            <template #default="{ row }">
              {{ row.outputFileName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="outputExtension" label="新的拓展名" width="120">
            <template #default="{ row }">
              {{ row.outputExtension || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </template>

      <el-empty v-else description="当前还没有暂存的批量任务配置，请先前往创建页完成步骤二。" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useBatchTaskStore } from '@/stores/batchTask'
import { countConfiguredWatermarks } from '@/utils/batchTask'

const router = useRouter()
const batchTaskStore = useBatchTaskStore()

const pendingDraft = computed(() => batchTaskStore.pendingDraft)

function clearPendingDraft() {
  batchTaskStore.clearPendingDraft()
  ElMessage.success('已清空暂存的批量任务配置')
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped lang="scss">
.task-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;

    h3 {
      margin: 0;
      font-size: 18px;
      color: var(--color-text-primary);
    }

    p {
      margin: 6px 0 0;
      color: var(--color-text-secondary);
      font-size: 13px;
    }
  }

  .draft-alert {
    margin-bottom: 16px;
  }

  .draft-summary {
    margin-bottom: 16px;
  }

  .draft-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-bottom: 16px;
  }
}
</style>
