<template>
  <div class="template-editor">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑模板' : '新建模板' }}</span>
          <div>
            <el-button @click="handleSave">保存草稿</el-button>
            <el-button type="primary" @click="handleSubmit">提交</el-button>
          </div>
        </div>
      </template>
      <el-alert
        v-if="draft?.hasConflict"
        title="模板冲突"
        type="warning"
        :description="draft?.conflictMessage"
        show-icon
        style="margin-bottom: 16px"
      />
      <div class="editor-content">
        <div class="config-panel">
          <el-form label-width="100px">
            <el-form-item label="模板名称">
              <el-input v-model="templateName" placeholder="请输入模板名称" />
            </el-form-item>
            <el-divider>底图配置</el-divider>
            <el-form-item label="宽度">
              <el-input-number v-model="baseConfig.width" :min="100" :max="5000" />
            </el-form-item>
            <el-form-item label="高度">
              <el-input-number v-model="baseConfig.height" :min="100" :max="5000" />
            </el-form-item>
            <el-form-item label="背景色">
              <el-color-picker v-model="baseConfig.backgroundColor" />
            </el-form-item>
            <el-divider>水印配置</el-divider>
            <div class="watermark-list">
              <draggable
                v-model="watermarks"
                item-key="id"
                handle=".drag-handle"
              >
                <template #item="{ element, index }">
                  <div class="watermark-item">
                    <div class="drag-handle">
                      <el-icon><Rank /></el-icon>
                    </div>
                    <div class="watermark-content">
                      <div class="watermark-type">
                        <el-tag size="small">{{ element.type === 'text' ? '文字水印' : '图片水印' }}</el-tag>
                      </div>
                      <div class="watermark-actions">
                        <el-button size="small" @click="editWatermark(index)">编辑</el-button>
                        <el-button size="small" type="danger" @click="removeWatermark(index)">删除</el-button>
                      </div>
                    </div>
                  </div>
                </template>
              </draggable>
            </div>
            <el-button type="success" plain @click="showAddDialog = true">添加水印</el-button>
          </el-form>
        </div>
        <div class="preview-panel">
          <div class="preview-canvas" :style="previewStyle">
            <div
              v-for="(wm, index) in watermarks"
              :key="index"
              class="watermark-overlay"
              :style="getWatermarkStyle(wm)"
            >
              {{ wm.type === 'text' ? wm.text : '' }}
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="showAddDialog" title="添加水印" width="500px">
      <el-form label-width="80px">
        <el-form-item label="类型">
          <el-radio-group v-model="newWatermark.type">
            <el-radio label="text">文字水印</el-radio>
            <el-radio label="image">图片水印</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddWatermark">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import draggable from 'vuedraggable'
import {
  getCurrentDraft,
  createNewDraft,
  createDraftFromTemplate,
  saveDraft,
  submitDraft,
  clearDraft
} from '@/api/template'
import type { DraftVO, WatermarkConfigDTO, BaseConfigDTO } from '@/types'
import { Rank } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const templateName = ref('')
const baseConfig = ref<BaseConfigDTO>({
  width: 800,
  height: 600,
  backgroundColor: '#ffffff'
})
const watermarks = ref<any[]>([])
const draft = ref<DraftVO | null>(null)
const showAddDialog = ref(false)
const newWatermark = ref({
  type: 'text' as 'text' | 'image'
})
const submitLoading = ref(false)
const saveLoading = ref(false)

const isEdit = computed(() => !!route.query.id)
const templateId = computed(() => Number(route.query.id))

const previewStyle = computed(() => ({
  width: `${baseConfig.value.width}px`,
  height: `${baseConfig.value.height}px`,
  backgroundColor: baseConfig.value.backgroundColor
}))

function getWatermarkStyle(wm: any) {
  return {
    left: `${wm.position?.x || 0}px`,
    top: `${wm.position?.y || 0}px`,
    color: wm.color || '#000000',
    fontSize: `${wm.fontSize || 16}px`,
    opacity: wm.opacity || 1
  }
}

async function loadDraft() {
  try {
    const res = await getCurrentDraft()
    if (res.code === 200 && res.data) {
      draft.value = res.data
      templateName.value = draft.value.name || ''
      if (draft.value.config) {
        baseConfig.value = draft.value.config.baseConfig || baseConfig.value
        watermarks.value = draft.value.config.watermarks || []
      }
    }
  } catch (error) {
    console.error('获取草稿失败:', error)
  }
}

async function initNewDraft() {
  try {
    await createNewDraft()
    await loadDraft()
  } catch (error) {
    console.error('创建草稿失败:', error)
  }
}

async function initFromTemplate(id: number) {
  try {
    await createDraftFromTemplate(id)
    await loadDraft()
  } catch (error) {
    console.error('从模板创建草稿失败:', error)
  }
}

async function handleSave() {
  saveLoading.value = true
  try {
    const config: WatermarkConfigDTO = {
      baseConfig: baseConfig.value,
      watermarks: watermarks.value
    }
    await saveDraft({
      sourceTemplateId: isEdit.value ? templateId.value : undefined,
      name: templateName.value,
      config
    })
    ElMessage.success('草稿已保存')
  } catch (error) {
    console.error('保存草稿失败:', error)
  } finally {
    saveLoading.value = false
  }
}

async function handleSubmit() {
  if (!templateName.value.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  submitLoading.value = true
  try {
    await handleSave()
    const res = await submitDraft({ forceCreateNew: draft.value?.hasConflict || false })
    if (res.code === 200) {
      ElMessage.success('模板提交成功')
      router.push('/template')
    }
  } catch (error) {
    console.error('提交模板失败:', error)
  } finally {
    submitLoading.value = false
  }
}

function handleAddWatermark() {
  if (newWatermark.value.type === 'text') {
    watermarks.value.push({
      type: 'text',
      text: '文字水印',
      position: { x: 100, y: 100 },
      fontSize: 24,
      color: '#000000',
      opacity: 1
    })
  } else {
    watermarks.value.push({
      type: 'image',
      imageUrl: '',
      position: { x: 100, y: 100 },
      scale: 1,
      opacity: 1
    })
  }
  showAddDialog.value = false
}

function editWatermark(index: number) {
  ElMessage.info('水印编辑功能开发中')
}

function removeWatermark(index: number) {
  watermarks.value.splice(index, 1)
}

onMounted(async () => {
  if (isEdit.value) {
    await initFromTemplate(templateId.value)
  } else {
    await initNewDraft()
  }
})
</script>

<style scoped lang="scss">
.template-editor {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .editor-content {
    display: flex;
    gap: 24px;
    min-height: 500px;

    .config-panel {
      width: 400px;
      flex-shrink: 0;
    }

    .preview-panel {
      flex: 1;
      display: flex;
      justify-content: center;
      align-items: flex-start;
      background-color: #f5f7fa;
      padding: 24px;
      border-radius: 8px;
      overflow: auto;

      .preview-canvas {
        position: relative;
        border: 1px solid #dcdfe6;
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      }
    }
  }

  .watermark-list {
    margin-bottom: 16px;

    .watermark-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      background-color: #f5f7fa;
      border-radius: 4px;
      margin-bottom: 8px;

      .drag-handle {
        cursor: move;
        color: #909399;
      }

      .watermark-content {
        flex: 1;
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
    }
  }

  .watermark-overlay {
    position: absolute;
    white-space: nowrap;
  }
}
</style>
