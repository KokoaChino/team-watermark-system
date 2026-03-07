<template>
  <div class="task-create">
    <el-card class="task-card">
      <template #header>
        <div class="card-header">
          <div>
            <h2>创建批量任务</h2>
            <p>先选择一个水印模板，后续步骤会基于该模板配置任务。</p>
          </div>
        </div>
      </template>

      <el-steps :active="currentStep" finish-status="success" class="task-steps">
        <el-step title="选择模板" description="复用模板列表能力，完成单模板选择" />
        <el-step title="水印配置" description="第二步基础架构已预留" />
      </el-steps>

      <section v-show="currentStep === 0" class="step-panel">
        <TemplateBrowser
          v-model="selectedTemplateId"
          title="步骤一：选择模板"
          empty-description="暂无可用模板，请先创建水印模板"
          @select="handleTemplateSelect"
        >
          <template #header-actions>
            <div v-if="selectedTemplate" class="selected-summary">
              <span class="summary-label">当前已选</span>
              <el-tag type="primary" effect="light">{{ selectedTemplate.name }}</el-tag>
            </div>
          </template>
        </TemplateBrowser>
      </section>

      <section v-show="currentStep === 1" class="step-panel step-placeholder">
        <el-card shadow="never" class="placeholder-card">
          <template #header>
            <div class="placeholder-header">
              <span>步骤二：水印配置</span>
              <el-tag type="warning" effect="light">待开发</el-tag>
            </div>
          </template>

          <el-alert
            type="info"
            :closable="false"
            title="当前只完成步骤一交互逻辑，步骤二暂时保留页面骨架。"
            show-icon
          />

          <el-descriptions v-if="selectedTemplate" :column="2" border class="template-summary">
            <el-descriptions-item label="已选模板">{{ selectedTemplate.name }}</el-descriptions-item>
            <el-descriptions-item label="创建者">{{ selectedTemplate.createdByUsername }}</el-descriptions-item>
            <el-descriptions-item label="模板版本">v{{ selectedTemplate.version }}</el-descriptions-item>
            <el-descriptions-item label="水印数量">
              {{ selectedTemplate.config?.watermarks?.length || 0 }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </section>

      <div class="step-actions">
        <el-button v-if="currentStep > 0" @click="currentStep -= 1">上一步</el-button>
        <el-button
          v-if="currentStep === 0"
          type="primary"
          :disabled="!selectedTemplateId"
          @click="currentStep = 1"
        >
          下一步
        </el-button>
        <el-button v-else type="primary" disabled>
          提交任务（待开发）
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TemplateBrowser from '@/components/template/TemplateBrowser.vue'
import type { WatermarkTemplateVO } from '@/types'

const currentStep = ref(0)
const selectedTemplateId = ref<number | null>(null)
const selectedTemplate = ref<WatermarkTemplateVO | null>(null)

function handleTemplateSelect(template: WatermarkTemplateVO | null) {
  selectedTemplate.value = template
}
</script>

<style scoped lang="scss">
.task-create {
  .task-card {
    :deep(.el-card__header) {
      padding: 20px 24px 16px;
    }
  }

  .card-header {
    h2 {
      margin: 0;
      font-size: 22px;
      color: var(--color-text-primary);
    }

    p {
      margin: 8px 0 0;
      color: var(--color-text-secondary);
      font-size: 14px;
    }
  }

  .task-steps {
    margin-bottom: 24px;
    padding: 0 8px;
  }

  .step-panel {
    min-height: 320px;
  }

  .selected-summary {
    display: flex;
    align-items: center;
    gap: 8px;

    .summary-label {
      color: var(--color-text-secondary);
      font-size: 13px;
    }
  }

  .step-placeholder {
    .placeholder-card {
      border-radius: 12px;
      background: linear-gradient(180deg, #fbfcfe 0%, #f5f7fa 100%);
    }

    .placeholder-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }

    .template-summary {
      margin-top: 20px;
    }
  }

  .step-actions {
    display: flex;
    justify-content: center;
    gap: 12px;
    margin-top: 24px;
  }
}
</style>
