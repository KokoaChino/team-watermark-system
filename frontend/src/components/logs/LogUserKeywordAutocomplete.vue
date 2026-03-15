<template>
  <el-autocomplete
    :model-value="modelValue"
    :fetch-suggestions="fetchSuggestions"
    :placeholder="placeholder"
    :trigger-on-focus="true"
    :debounce="200"
    clearable
    :style="{ width }"
    @update:model-value="handleChange"
  />
</template>

<script setup lang="ts">
interface SuggestionItem {
  value: string
}

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  width?: string
  fetchOptions: (keyword: string) => Promise<string[]>
}>(), {
  modelValue: '',
  placeholder: '按人名搜索',
  width: '220px'
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

let requestId = 0

async function fetchSuggestions(queryString: string, callback: (items: SuggestionItem[]) => void) {
  const currentRequestId = ++requestId
  const keyword = queryString.trim()

  try {
    const options = await props.fetchOptions(keyword)
    if (currentRequestId !== requestId) {
      return
    }

    callback(
      Array.from(new Set(options))
        .filter((item) => item)
        .map((item) => ({ value: item }))
    )
  } catch {
    if (currentRequestId === requestId) {
      callback([])
    }
  }
}

function handleChange(value: string) {
  emit('update:modelValue', value || '')
}
</script>
