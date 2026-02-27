<template>
  <div class="task-create">
    <el-card>
      <template #header>
        <span>创建批量任务</span>
      </template>
      <el-steps :active="currentStep" finish-status="success" style="margin-bottom: 32px">
        <el-step title="选择模板" />
        <el-step title="上传图片" />
        <el-step title="配置Excel" />
        <el-step title="确认提交" />
      </el-steps>

      <div v-show="currentStep === 0" class="step-content">
        <h3>选择水印模板</h3>
        <el-radio-group v-model="selectedTemplateId">
          <el-radio
            v-for="template in templates"
            :key="template.id"
            :label="template.id"
            border
            class="template-radio"
          >
            <div class="template-option">
              <span>{{ template.name }}</span>
              <span class="template-info">创建者: {{ template.createdByUsername }}</span>
            </div>
          </el-radio>
        </el-radio-group>
        <div v-if="templates.length === 0" class="empty-tip">
          暂无可用模板，请先创建水印模板
        </div>
      </div>

      <div v-show="currentStep === 1" class="step-content">
        <h3>上传图片</h3>
        <el-upload
          v-model:file-list="imageList"
          action="/api/upload"
          list-type="picture-card"
          :auto-upload="false"
          :limit="100"
          accept="image/*"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <div class="upload-tip">最多上传100张图片，支持jpg、png、jpeg格式</div>
      </div>

      <div v-show="currentStep === 2" class="step-content">
        <h3>上传Excel配置文件</h3>
        <el-upload
          v-model:file-list="excelFile"
          :auto-upload="false"
          accept=".xlsx,.xls"
          :limit="1"
        >
          <el-button type="primary">选择文件</el-button>
        </el-upload>
        <div class="upload-tip">
          Excel第一行为表头，需包含图片ID列和对应的文字水印内容列
        </div>
      </div>

      <div v-show="currentStep === 3" class="step-content">
        <h3>确认提交</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="已选模板">
            {{ selectedTemplate?.name }}
          </el-descriptions-item>
          <el-descriptions-item label="图片数量">
            {{ imageList.length }} 张
          </el-descriptions-item>
          <el-descriptions-item label="Excel文件">
            {{ excelFile[0]?.name || '未上传' }}
          </el-descriptions-item>
          <el-descriptions-item label="预估消耗点数">
            {{ imageList.length }} 点
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="step-actions">
        <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
        <el-button v-if="currentStep < 3" type="primary" @click="handleNext">
          下一步
        </el-button>
        <el-button v-if="currentStep === 3" type="success" :loading="submitting" @click="handleSubmit">
          提交任务
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTemplateList } from '@/api/template'
import type { WatermarkTemplateVO } from '@/types'
import { Plus } from '@element-plus/icons-vue'

const router = useRouter()

const currentStep = ref(0)
const templates = ref<WatermarkTemplateVO[]>([])
const selectedTemplateId = ref<number | null>(null)
const imageList = ref<any[]>([])
const excelFile = ref<any[]>([])
const submitting = ref(false)

const selectedTemplate = computed(() =>
  templates.value.find(t => t.id === selectedTemplateId.value)
)

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

function handleNext() {
  if (currentStep.value === 0) {
    if (!selectedTemplateId.value) {
      ElMessage.warning('请选择水印模板')
      return
    }
  } else if (currentStep.value === 1) {
    if (imageList.value.length === 0) {
      ElMessage.warning('请上传至少一张图片')
      return
    }
  }
  currentStep.value++
}

async function handleSubmit() {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请选择水印模板')
    return
  }
  if (imageList.value.length === 0) {
    ElMessage.warning('请上传图片')
    return
  }
  submitting.value = true
  try {
    ElMessage.info('任务提交功能开发中')
  } catch (error) {
    console.error('提交任务失败:', error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchTemplates()
})
</script>

<style scoped lang="scss">
.task-create {
  .step-content {
    min-height: 300px;

    h3 {
      margin-bottom: 20px;
      color: var(--color-text-primary);
    }
  }

  .template-radio {
    margin: 0 12px 12px 0;

    .template-option {
      display: flex;
      flex-direction: column;
      padding: 8px 0;

      .template-info {
        font-size: 12px;
        color: var(--color-text-secondary);
      }
    }
  }

  .upload-tip {
    margin-top: 12px;
    font-size: 13px;
    color: var(--color-text-secondary);
  }

  .empty-tip {
    padding: 40px;
    text-align: center;
    color: var(--color-text-secondary);
  }

  .step-actions {
    margin-top: 24px;
    display: flex;
    justify-content: center;
    gap: 12px;
  }
}
</style>
