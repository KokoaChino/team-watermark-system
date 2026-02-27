<template>
  <div class="task-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>批量任务</span>
          <el-button type="primary" @click="router.push('/task/create')">
            创建任务
          </el-button>
        </div>
      </template>
      <el-empty description="暂无任务记录" />
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
  }
}
</style>
