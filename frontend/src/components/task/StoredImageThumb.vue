<template>
  <div class="stored-image-thumb">
    <img v-if="previewUrl" :src="previewUrl" :alt="alt" class="thumb-image" />
    <div v-else class="thumb-placeholder">{{ loading ? '加载中' : '无预览' }}</div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { useBatchTaskStore } from '@/stores/batchTask'

interface Props {
  fileKey?: string
  kind: 'source' | 'result' | 'artifact'
  alt?: string
}

const props = withDefaults(defineProps<Props>(), {
  fileKey: '',
  alt: '图片预览'
})

const batchTaskStore = useBatchTaskStore()
const loading = ref(false)
const previewUrl = ref('')

watch(
  () => [props.fileKey, props.kind],
  async () => {
    await loadPreview()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  revokePreviewUrl()
})

async function loadPreview() {
  revokePreviewUrl()

  if (!props.fileKey) {
    return
  }

  loading.value = true

  try {
    const file = await resolveFile()
    if (!file) {
      return
    }

    previewUrl.value = URL.createObjectURL(file)
  } catch (error) {
    console.error('加载图片预览失败:', error)
  } finally {
    loading.value = false
  }
}

async function resolveFile() {
  if (props.kind === 'source') {
    return batchTaskStore.loadSourceFile(props.fileKey)
  }

  if (props.kind === 'result') {
    return batchTaskStore.loadResultFile(props.fileKey)
  }

  return batchTaskStore.loadArtifact(props.fileKey)
}

function revokePreviewUrl() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}
</script>

<style scoped lang="scss">
.stored-image-thumb {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(180deg, #fbfcfe 0%, #eef3f8 100%);
  border: 1px solid #e6edf5;
}

.thumb-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.thumb-placeholder {
  padding: 6px;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.3;
  text-align: center;
}
</style>
