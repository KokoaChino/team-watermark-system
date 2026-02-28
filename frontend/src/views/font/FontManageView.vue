<template>
  <div class="font-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>字体管理</span>
          <el-button type="primary" @click="handleOpenUpload">
            上传字体
          </el-button>
        </div>
      </template>
      
      <div class="filter-bar">
        <el-input
          v-model="filterName"
          placeholder="搜索字体名称"
          clearable
          style="width: 200px"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-radio-group v-model="filterType" style="margin-left: 16px">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="system">系统字体</el-radio-button>
          <el-radio-button label="user">用户字体</el-radio-button>
        </el-radio-group>
        <el-select 
          v-if="filterType === 'user'" 
          v-model="filterSource" 
          style="margin-left: 8px; width: 120px"
          placeholder="来源"
        >
          <el-option label="全部" value="all" />
          <el-option label="我上传的" value="mine" />
          <el-option label="团队成员" value="team" />
        </el-select>
      </div>

      <el-table :data="displayFonts" v-loading="loading" style="width: 100%; margin-top: 16px">
        <el-table-column prop="name" label="字体名称" min-width="200" />
        <el-table-column label="字体预览" min-width="700">
          <template #default="{ row }">
            <span 
              :style="getPreviewStyle(row)" 
              class="font-preview"
            >
              0123456789 abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ 中国智造，慧及全球。 !@#$%^&*()_+-=[]{}|;:',.<>?/
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类型" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.isSystemFont ? 'info' : 'success'" size="small">
              {{ row.isSystemFont ? '系统字体' : '用户上传' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" min-width="100">
          <template #default="{ row }">
            {{ getSourceText(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="200">
          <template #default="{ row }">
            {{ row.createdAt ? formatDate(row.createdAt) : '' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="100">
          <template #default="{ row }">
            <el-button
              v-if="!row.isSystemFont && canDelete(row)"
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

    <el-dialog
      v-model="showUploadDialog"
      title="上传字体"
      width="450px"
      @close="handleDialogClose"
    >
      <el-form :model="uploadForm" :rules="uploadRules" ref="uploadFormRef" label-width="80px">
        <el-form-item label="字体名称" prop="name">
          <el-input v-model="uploadForm.name" placeholder="不填则使用文件名" />
        </el-form-item>
        <el-form-item label="字体文件" prop="fontFile">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".ttf,.otf"
            :before-upload="handleBeforeUpload"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            :on-remove="handleRemove"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .ttf 或 .otf 格式，文件大小不超过 10MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type UploadRawFile, type UploadInstance, type UploadProps } from 'element-plus'
import { Search, UploadFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getFontList, uploadFont, deleteFont } from '@/api/font'
import { getTeamInfo } from '@/api/team'
import type { FontVO } from '@/types'

const systemFonts = [
  { name: 'KaiTi', alias: '楷体', category: 'chinese' },
  { name: 'FangSong', alias: '仿宋', category: 'chinese' },
  { name: 'YouYuan', alias: '幼圆', category: 'chinese' },
  { name: 'STXingkai', alias: '华文行楷', category: 'chinese' },
  { name: 'STKaiti', alias: '华文楷体', category: 'chinese' },
  { name: 'STSong', alias: '华文宋体', category: 'chinese' },
  { name: 'STHeiti', alias: '华文黑体', category: 'chinese' },
  { name: 'STZhongsong', alias: '华文中宋', category: 'chinese' },
  { name: 'STCaiyun', alias: '华文彩云', category: 'chinese' },
  { name: 'STHupo', alias: '华文琥珀', category: 'chinese' },
  { name: 'Alibaba PuHuiTi', alias: '阿里巴巴普惠体', category: 'chinese' },
  { name: 'MiSans', alias: '小米MiSans', category: 'chinese' },
  { name: 'HarmonyOS Sans', alias: '华为鸿蒙体', category: 'chinese' },
  { name: 'Noto Sans SC', alias: 'Noto思源黑体', category: 'chinese' },
  { name: 'Noto Serif SC', alias: 'Noto思源宋体', category: 'chinese' },
]

const userStore = useUserStore()

const fonts = ref<FontVO[]>([])
const loading = ref(false)
const filterName = ref('')
const filterType = ref<'all' | 'system' | 'user'>('all')
const filterSource = ref<'all' | 'mine' | 'team'>('all')
const teamInfo = ref<{ role?: string } | null>(null)

const showUploadDialog = ref(false)
const uploadFormRef = ref<FormInstance>()
const uploadRef = ref<UploadInstance>()
const uploading = ref(false)
const uploadForm = ref({
  name: '',
  fontFile: null as UploadRawFile | null
})

const uploadRules = {
  name: [
    { required: true, message: '请输入字体名称', trigger: 'blur' }
  ],
  fontFile: [
    { required: true, message: '请上传字体文件', trigger: 'change' }
  ]
}

const isLeader = computed(() => teamInfo.value?.role === 'leader')

const displayFonts = computed(() => {
  const searchLower = filterName.value.toLowerCase()
  
  const systemFontVOs = systemFonts
    .filter(f => {
      if (filterType.value === 'user') return false
      if (filterName.value) {
        return f.alias.toLowerCase().includes(searchLower)
      }
      return true
    })
    .map((f, index) => ({
      id: -(index + 1),
      name: f.alias,
      fontUrl: '',
      isSystemFont: true,
      createdAt: ''
    } as FontVO))
    .sort((a, b) => a.name.localeCompare(b.name))
  
  if (filterType.value === 'system') {
    return systemFontVOs
  }
  
  let userFonts = [...fonts.value]
  
  if (filterSource.value === 'mine') {
    userFonts = userFonts.filter(f => f.uploadedBy === userStore.userInfo?.id)
  } else if (filterSource.value === 'team') {
    userFonts = userFonts.filter(f => f.uploadedBy !== userStore.userInfo?.id)
  }
  
  if (filterName.value) {
    userFonts = userFonts.filter(f => f.name.toLowerCase().includes(searchLower))
  }
  
  const mineFonts = userFonts
    .filter(f => f.uploadedBy === userStore.userInfo?.id)
    .sort((a, b) => a.name.localeCompare(b.name))
  
  const teamFonts = userFonts
    .filter(f => f.uploadedBy !== userStore.userInfo?.id)
    .sort((a, b) => a.name.localeCompare(b.name))
  
  return [...mineFonts, ...teamFonts, ...systemFontVOs]
})

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

function getSourceText(row: FontVO) {
  if (row.isSystemFont) return '系统内置'
  if (row.uploadedBy === userStore.userInfo?.id) return '我上传的'
  return '团队成员'
}

function canDelete(row: FontVO) {
  return !row.isSystemFont && (isLeader.value || row.uploadedBy === userStore.userInfo?.id)
}

function getPreviewStyle(row: FontVO) {
  if (row.isSystemFont) {
    const font = systemFonts.find(f => f.alias === row.name)
    if (font) {
      return { fontFamily: `"${font.name}", sans-serif` }
    }
    return { fontFamily: 'sans-serif' }
  }
  
  const fontFamily = `custom-font-${row.id}`
  
  if (row.fontUrl && !document.getElementById(`font-${row.id}`)) {
    const style = document.createElement('style')
    style.id = `font-${row.id}`
    style.textContent = `
      @font-face {
        font-family: '${fontFamily}';
        src: url('${row.fontUrl}');
      }
    `
    document.head.appendChild(style)
  }
  
  return { fontFamily: fontFamily }
}

async function fetchFonts() {
  loading.value = true
  try {
    const res = await getFontList()
    if (res.code === 200) {
      fonts.value = res.data || []
    }
  } catch (error) {
    console.error('获取字体列表失败:', error)
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

function handleSearch() {
}

const handleBeforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isValidType = file.type === 'font/ttf' || file.type === 'font/otf' || 
                     file.name.toLowerCase().endsWith('.ttf') || file.name.toLowerCase().endsWith('.otf')
  const isLt10M = file.size / 1024 / 1024 < 10
  
  if (!isValidType) {
    ElMessage.error('只能上传 .ttf 或 .otf 格式的字体文件')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('字体文件大小不能超过 10MB')
    return false
  }
  return true
}

function handleFileChange(file: any) {
  const isValidType = file.name.toLowerCase().endsWith('.ttf') || file.name.toLowerCase().endsWith('.otf')
  const isLt10M = file.size / 1024 / 1024 < 10
  
  if (!isValidType) {
    ElMessage.error('只能上传 .ttf 或 .otf 格式的字体文件')
    uploadRef.value?.clearFiles()
    return
  }
  if (!isLt10M) {
    ElMessage.error('字体文件大小不能超过 10MB')
    uploadRef.value?.clearFiles()
    return
  }
  
  uploadForm.value.fontFile = file.raw
  
  if (!uploadForm.value.name) {
    uploadForm.value.name = file.name.replace(/\.(ttf|otf)$/i, '')
  }
}

function handleExceed() {
  ElMessage.warning('只能上传一个字体文件')
}

function handleRemove() {
  uploadForm.value.fontFile = null
}

function handleOpenUpload() {
  uploadForm.value = { name: '', fontFile: null }
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  showUploadDialog.value = true
}

function handleDialogClose() {
  uploadForm.value = { name: '', fontFile: null }
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  uploadFormRef.value?.clearValidate()
}

async function handleUpload() {
  if (!uploadFormRef.value) return
  
  await uploadFormRef.value.validate(async (valid) => {
    if (!valid) return
    if (!uploadForm.value.fontFile) {
      ElMessage.warning('请上传字体文件')
      return
    }
    
    const fontName = uploadForm.value.name.trim() || uploadForm.value.fontFile.name.replace(/\.(ttf|otf)$/i, '')
    
    uploading.value = true
    try {
      const res = await uploadFont(fontName, uploadForm.value.fontFile)
      if (res.code === 200) {
        ElMessage.success('字体上传成功')
        showUploadDialog.value = false
        fetchFonts()
      }
    } catch (error) {
      console.error('上传字体失败:', error)
    } finally {
      uploading.value = false
    }
  })
}

async function handleDelete(font: FontVO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除字体 "${font.name}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteFont(font.id)
    ElMessage.success('字体已删除')
    fetchFonts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除字体失败:', error)
    }
  }
}

onMounted(() => {
  fetchFonts()
  fetchTeamInfo()
})

watch(filterType, (newVal) => {
  if (newVal !== 'user') {
    filterSource.value = 'all'
  }
})
</script>

<style scoped lang="scss">
.font-manage {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .filter-bar {
    display: flex;
    align-items: center;
  }

  .text-muted {
    color: var(--color-text-placeholder);
  }

  .el-upload__tip {
    color: #909399;
    font-size: 12px;
    margin-top: 7px;
  }

  .font-preview {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
