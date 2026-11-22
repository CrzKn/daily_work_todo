<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  priorities: { type: Array, default: () => [] },
  inputId: { type: String, default: '' },
  placeholder: { type: String, default: '不设置' },
})

const emit = defineEmits(['update:modelValue'])

const open = ref(false)
const triggerRef = ref(null)
const panelStyle = ref({})

const selectedName = computed(() => {
  const id = props.modelValue
  if (id === '' || id === null || id === undefined) return ''
  const item = props.priorities.find((p) => String(p.priorityId) === String(id))
  return item ? item.priorityName : ''
})

// 与优先级管理页圆点保持一致：排序值越小越红，越大越绿
const styleMap = computed(() => {
  const list = props.priorities.filter(
    (item) => item.priorityOrder !== null && item.priorityOrder !== undefined && item.priorityOrder !== '',
  )
  const nums = list.map((item) => Number(item.priorityOrder)).filter((n) => Number.isFinite(n))
  const map = {}
  if (!nums.length) return map
  const min = Math.min(...nums)
  const max = Math.max(...nums)
  list.forEach((item) => {
    const current = Number(item.priorityOrder)
    if (!Number.isFinite(current)) return
    const ratio = max === min ? 1 : (current - min) / (max - min)
    const hue = Math.round(ratio * 120)
    map[item.priorityId] = {
      color: `hsl(${hue}, 72%, 45%)`,
      background: `hsl(${hue}, 80%, 94%)`,
    }
  })
  return map
})

function toggle() {
  if (open.value) {
    open.value = false
    return
  }
  const trigger = triggerRef.value
  if (trigger) {
    const rect = trigger.getBoundingClientRect()
    panelStyle.value = {
      top: `${rect.bottom + 4}px`,
      left: `${rect.left}px`,
      width: `${rect.width}px`,
    }
  }
  open.value = true
}

function select(id) {
  emit('update:modelValue', id)
  open.value = false
}

function onKeydown(e) {
  if (e.key === 'Escape' && open.value) {
    open.value = false
    e.stopPropagation()
  }
}

function onScroll() {
  if (open.value) open.value = false
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  window.addEventListener('scroll', onScroll, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  window.removeEventListener('scroll', onScroll, true)
})
</script>

<template>
  <div class="priority-select" :class="{ open }">
    <button
      :id="inputId || undefined"
      ref="triggerRef"
      type="button"
      class="priority-trigger"
      aria-haspopup="listbox"
      :aria-expanded="open"
      @click="toggle"
    >
      <span class="priority-trigger-text">{{ selectedName || placeholder }}</span>
      <span class="priority-arrow" aria-hidden="true"></span>
    </button>

    <Teleport to="body">
      <div v-if="open" class="priority-backdrop" @mousedown="open = false"></div>
      <div v-if="open" class="priority-panel" role="listbox" :style="panelStyle">
        <button
          type="button"
          class="priority-option"
          :class="{ active: !selectedName }"
          @click="select('')"
        >
          <span class="priority-empty">{{ placeholder }}</span>
        </button>
        <button
          v-for="p in priorities"
          :key="p.priorityId"
          type="button"
          class="priority-option"
          :class="{ active: String(p.priorityId) === String(modelValue) }"
          @click="select(p.priorityId)"
        >
          <span class="priority-chip" :style="styleMap[p.priorityId]">{{ p.priorityName }}</span>
        </button>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.priority-select {
  position: relative;
}

.priority-trigger {
  width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  font-size: 14px;
  font-family: inherit;
  color: #111827;
  text-align: left;
  background: #ffffff;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  outline: none;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.priority-trigger:focus,
.priority-select.open .priority-trigger {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.priority-trigger-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.priority-arrow {
  width: 0;
  height: 0;
  flex-shrink: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid #9ca3af;
  transition: transform 0.15s;
}

.priority-select.open .priority-arrow {
  transform: rotate(180deg);
}

.priority-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2400;
}

.priority-panel {
  position: fixed;
  z-index: 2402;
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 220px;
  overflow-y: auto;
  padding: 6px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  animation: priorityFadeIn 0.12s ease-out;
}

@keyframes priorityFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.priority-option {
  display: flex;
  align-items: center;
  padding: 5px 8px;
  background: transparent;
  border: none;
  border-radius: 6px;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s;
}

.priority-option:hover {
  background: #f3f4f6;
}

.priority-option.active {
  background: #eff6ff;
}

.priority-chip {
  padding: 1px 8px;
  font-size: 12px;
  line-height: 18px;
  border-radius: 10px;
  white-space: nowrap;
}

.priority-empty {
  font-size: 13px;
  color: #6b7280;
}
</style>
