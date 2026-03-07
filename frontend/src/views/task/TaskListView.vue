<template>
  <div class="task-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h3>批量任务执行</h3>
            <p>执行记录、进度追踪和结果下载将在这一页继续完善。</p>
          </div>
          <el-button type="primary" @click="router.push('/task/create')">
            创建批量任务
          </el-button>
        </div>
      </template>

      <el-empty description="批量任务执行页基础架构已预留，后续在此接入任务列表与执行状态。" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
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
}
</style>
