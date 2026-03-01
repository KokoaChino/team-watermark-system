<template>
  <div class="text-watermark-editor">
    <el-form :model="localConfig" label-width="70px" size="small">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="内容">
            <el-input v-model="localConfig.content" placeholder="水印文字" @input="handleUpdate" />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="颜色" label-width="60px">
            <el-color-picker v-model="localConfig.color" :show-alpha="true" @change="handleUpdate" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="基点" label-width="40px">
            <el-radio-group v-model="localConfig.align" @change="handleUpdate">
              <el-radio-button value="left">左</el-radio-button>
              <el-radio-button value="center">中</el-radio-button>
              <el-radio-button value="right">右</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="字体">
            <el-select
              v-model="localConfig.fontFamily"
              placeholder="选择字体"
              filterable
              @change="handleFontChange"
              class="font-select"
            >
              <el-option
                v-for="font in allFonts"
                :key="font.value"
                :label="font.label"
                :value="font.value"
              >
                <div class="font-option">
                  <span class="font-name" :style="{ fontFamily: font.previewFamily }">{{ font.label }}</span>
                  <el-tag v-if="font.isSystem" size="small" type="info">系统</el-tag>
                  <el-tag v-else size="small" type="success">用户</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="字号">
            <el-input-number v-model="localConfig.fontSize" :min="1" :max="10000" @input="handleUpdate" />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="字重">
            <el-slider v-model="localConfig.fontWeight" :min="100" :max="900" :step="1" @input="handleUpdate" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="斜度">
            <el-slider v-model="localConfig.italicAngle" :min="-60" :max="60" :step="1" @input="handleUpdate" class="right-padded-slider" />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="旋转">
            <el-slider v-model="localConfig.rotation" :min="-180" :max="180" :step="1" @input="handleUpdate" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="透明度">
            <el-slider v-model="localConfig.opacity" :min="0" :max="1" :step="0.01" @input="handleUpdate" class="right-padded-slider" />
          </el-form-item>
        </el-col>
      </el-row>
      
      <div class="inline-form-item">
        <span class="label">渐变</span>
        <el-switch v-model="localConfig.gradientEnabled" @change="handleGradientToggle" />
        <template v-if="localConfig.gradientEnabled">
          <span class="param-label">渐变色</span>
          <el-color-picker v-model="gradientColor1" @change="handleGradientChange" size="small" />
          <span class="gradient-arrow">→</span>
          <el-color-picker v-model="gradientColor2" @change="handleGradientChange" size="small" />
          <span class="param-label">方向</span>
          <el-slider v-model="localConfig.gradientAngle" :min="0" :max="360" :step="1" @input="handleUpdate" class="inline-slider" />
        </template>
      </div>
      
      <div class="inline-form-item">
        <span class="label">描边</span>
        <el-switch v-model="localConfig.strokeEnabled" @change="handleStrokeToggle" />
        <template v-if="localConfig.strokeEnabled">
          <span class="param-label">颜色</span>
          <el-color-picker v-model="localConfig.strokeColor" :show-alpha="true" @change="handleUpdate" size="small" />
          <span class="param-label">宽度</span>
          <el-input-number v-model="localConfig.strokeWidth" :min="0" :max="20" :step="0.5" @input="handleUpdate" size="small" />
        </template>
      </div>
      
      <div class="inline-form-item">
        <span class="label">阴影</span>
        <el-switch v-model="localConfig.shadowEnabled" @change="handleShadowToggle" />
        <template v-if="localConfig.shadowEnabled">
          <span class="param-label">颜色</span>
          <el-color-picker v-model="localConfig.shadowColor" :show-alpha="true" @change="handleUpdate" size="small" />
          <span class="param-label">模糊</span>
          <el-input-number v-model="localConfig.shadowBlur" :min="0" :max="50" @input="handleUpdate" size="small" />
          <span class="param-label">X</span>
          <el-input-number v-model="localConfig.shadowOffsetX" :min="-500" :max="500" @input="handleUpdate" size="small" />
          <span class="param-label">Y</span>
          <el-input-number v-model="localConfig.shadowOffsetY" :min="-500" :max="500" @input="handleUpdate" size="small" />
        </template>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { TextWatermarkConfig, FontVO } from '@/types'
import { getFontList } from '@/api/font'

interface FontOption {
  value: string
  label: string
  isSystem: boolean
  fontUrl?: string
  previewFamily: string
  fontFamily?: string
}

const props = defineProps<{
  config: TextWatermarkConfig
}>()

const emit = defineEmits<{
  update: [config: TextWatermarkConfig]
}>()

const localConfig = ref<TextWatermarkConfig>({ ...props.config })
const gradientColor1 = ref('#409eff')
const gradientColor2 = ref('#f56c6c')
const userFonts = ref<FontVO[]>([])
const loadedFonts = ref<Set<string>>(new Set())
const failedFonts = ref<Set<string>>(new Set())

const systemFonts: FontOption[] = [
  { value: 'Microsoft YaHei', label: '微软雅黑', isSystem: true, previewFamily: '"Microsoft YaHei", sans-serif' },
  { value: 'SimSun', label: '宋体', isSystem: true, previewFamily: '"SimSun", serif' },
  { value: 'SimHei', label: '黑体', isSystem: true, previewFamily: '"SimHei", sans-serif' },
  { value: 'KaiTi', label: '楷体', isSystem: true, previewFamily: '"KaiTi", serif' },
  { value: 'FangSong', label: '仿宋', isSystem: true, previewFamily: '"FangSong", serif' },
  { value: 'YouYuan', label: '幼圆', isSystem: true, previewFamily: '"YouYuan", sans-serif' },
  { value: 'STXingkai', label: '华文行楷', isSystem: true, previewFamily: '"STXingkai", sans-serif' },
  { value: 'STKaiti', label: '华文楷体', isSystem: true, previewFamily: '"STKaiti", sans-serif' },
  { value: 'STSong', label: '华文宋体', isSystem: true, previewFamily: '"STSong", sans-serif' },
  { value: 'STHeiti', label: '华文黑体', isSystem: true, previewFamily: '"STHeiti", sans-serif' },
  { value: 'STZhongsong', label: '华文中宋', isSystem: true, previewFamily: '"STZhongsong", sans-serif' },
  { value: 'STCaiyun', label: '华文彩云', isSystem: true, previewFamily: '"STCaiyun", sans-serif' },
  { value: 'STHupo', label: '华文琥珀', isSystem: true, previewFamily: '"STHupo", sans-serif' },
  { value: 'Alibaba PuHuiTi', label: '阿里巴巴普惠体', isSystem: true, previewFamily: '"Alibaba PuHuiTi", sans-serif' },
  { value: 'MiSans', label: '小米MiSans', isSystem: true, previewFamily: '"MiSans", sans-serif' },
  { value: 'HarmonyOS Sans', label: '华为鸿蒙体', isSystem: true, previewFamily: '"HarmonyOS Sans", sans-serif' },
  { value: 'Noto Sans SC', label: 'Noto思源黑体', isSystem: true, previewFamily: '"Noto Sans SC", sans-serif' },
  { value: 'Noto Serif SC', label: 'Noto思源宋体', isSystem: true, previewFamily: '"Noto Serif SC", serif' },
  { value: 'Arial', label: 'Arial', isSystem: true, previewFamily: 'Arial, sans-serif' },
  { value: 'Times New Roman', label: 'Times New Roman', isSystem: true, previewFamily: '"Times New Roman", serif' },
]

const allFonts = computed<FontOption[]>(() => {
  const userFontOptions: FontOption[] = userFonts.value.map(font => {
    const fontFamily = `custom-font-${font.id}`
    return {
      value: fontFamily,
      label: font.name,
      isSystem: false,
      fontUrl: font.fontUrl,
      previewFamily: `"${fontFamily}", sans-serif`,
      fontFamily: fontFamily
    }
  })
  return [...userFontOptions, ...systemFonts]
})

async function loadCustomFont(fontFamily: string, fontUrl: string): Promise<boolean> {
  if (loadedFonts.value.has(fontFamily)) return true
  if (failedFonts.value.has(fontFamily)) return false
  
  try {
    const style = document.createElement('style')
    style.id = `font-style-${fontFamily}`
    style.textContent = `
      @font-face {
        font-family: '${fontFamily}';
        src: url('${fontUrl}') format('truetype');
      }
    `
    document.head.appendChild(style)
    
    await document.fonts.load(`12px "${fontFamily}"`)
    loadedFonts.value.add(fontFamily)
    return true
  } catch (error) {
    console.error(`加载字体失败: ${fontFamily}`, error)
    if (!failedFonts.value.has(fontFamily)) {
      ElMessage.error(`字体加载失败`)
    }
    failedFonts.value.add(fontFamily)
    return false
  }
}

async function fetchUserFonts() {
  try {
    const res = await getFontList()
    if (res.code === 200) {
      userFonts.value = res.data || []
      userFonts.value.forEach(font => {
        if (font.fontUrl) {
          const fontFamily = `custom-font-${font.id}`
          loadCustomFont(fontFamily, font.fontUrl)
        }
      })
    }
  } catch (error) {
    console.error('获取字体列表失败:', error)
  }
}

function handleFontChange(value: string) {
  const font = allFonts.value.find(f => f.value === value)
  if (font) {
    if (font.isSystem) {
      localConfig.value.fontFamily = font.value
      localConfig.value.fontUrl = undefined
    } else {
      localConfig.value.fontFamily = font.fontFamily || font.value
      localConfig.value.fontUrl = font.fontUrl
      if (font.fontUrl && font.fontFamily) {
        loadCustomFont(font.fontFamily, font.fontUrl)
      }
    }
  }
  handleUpdate()
}

watch(() => props.config, (newConfig) => {
  if (!newConfig) return
  localConfig.value = { ...newConfig }
  if (newConfig.gradientColors && newConfig.gradientColors.length >= 2) {
    gradientColor1.value = newConfig.gradientColors[0]
    gradientColor2.value = newConfig.gradientColors[1]
  }
}, { deep: true, immediate: true })

function handleGradientToggle(enabled: boolean) {
  if (enabled) {
    gradientColor1.value = '#409eff'
    gradientColor2.value = '#f56c6c'
    localConfig.value.gradientColors = [gradientColor1.value, gradientColor2.value]
    if (localConfig.value.gradientAngle === undefined) {
      localConfig.value.gradientAngle = 0
    }
  }
  handleUpdate()
}

function handleGradientChange() {
  localConfig.value.gradientColors = [gradientColor1.value, gradientColor2.value]
  handleUpdate()
}

function handleStrokeToggle(enabled: boolean) {
  if (enabled) {
    if (!localConfig.value.strokeColor) {
      localConfig.value.strokeColor = '#000000'
    }
    if (localConfig.value.strokeWidth === undefined) {
      localConfig.value.strokeWidth = 1
    }
  }
  handleUpdate()
}

function handleShadowToggle(enabled: boolean) {
  if (enabled) {
    if (!localConfig.value.shadowColor) {
      localConfig.value.shadowColor = 'rgba(0,0,0,0.5)'
    }
    if (localConfig.value.shadowBlur === undefined) {
      localConfig.value.shadowBlur = 4
    }
    if (localConfig.value.shadowOffsetX === undefined) {
      localConfig.value.shadowOffsetX = 2
    }
    if (localConfig.value.shadowOffsetY === undefined) {
      localConfig.value.shadowOffsetY = 2
    }
  }
  handleUpdate()
}

function handleUpdate() {
  emit('update', { ...localConfig.value })
}

onMounted(() => {
  fetchUserFonts()
})
</script>

<style scoped lang="scss">
.text-watermark-editor {
  padding-right: 8px;
}

.font-select {
  width: 100%;
}

.font-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  
  .font-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.right-padded-slider {
  margin-right: 8px;
}

.inline-form-item {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
  min-height: 32px;
  
  .label {
    width: 70px;
    flex-shrink: 0;
    font-size: 12px;
    color: #606266;
    text-align: right;
    padding-right: 13px;
    box-sizing: border-box;
  }
  
  .param-label {
    font-size: 12px;
    color: #909399;
    margin-left: 8px;
  }
  
  .gradient-arrow {
    color: #909399;
    font-size: 14px;
  }
  
  .inline-slider {
    width: 100px;
  }
}

:deep(.el-color-picker__trigger) {
  .el-color-picker__empty,
  .el-icon-close {
    display: none;
  }
}
</style>
