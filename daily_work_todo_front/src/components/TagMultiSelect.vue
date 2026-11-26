<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { createCrudApi } from '../api/crud'

// 下拉多选标签：modelValue 为逗号分隔的标签id字符串，未选择时为 ''
// 面板 Teleport 到 body，避免被弹窗的 overflow 裁剪
const props = defineProps({
  modelValue: { type: String, default: '' },
  tags: { type: Array, default: () => [] },
  inputId: { type: String, default: '' },
  placeholder: { type: String, default: '请选择标签' },
  allowCreate: { type: Boolean, default: true },
})

const emit = defineEmits(['update:modelValue', 'created'])

const tagApi = createCrudApi('/tags')

const open = ref(false)
const keyword = ref('')
const triggerRef = ref(null)
const panelRef = ref(null)
const panelStyle = ref({})
const creating = ref(false)
const newTagName = ref('')
const saving = ref(false)
const createError = ref('')
const createInputRef = ref(null)

const selectedIds = computed(() =>
  (props.modelValue || '')
    .split(',')
    .map((id) => id.trim())
    .filter(Boolean),
)

const selectedTags = computed(() =>
  selectedIds.value
    .map((id) => props.tags.find((t) => t.tagId === id))
    .filter(Boolean),
)

const visibleChips = computed(() => selectedTags.value.slice(0, 2))
const hiddenCount = computed(() => Math.max(selectedTags.value.length - 2, 0))

const filteredTags = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return props.tags
  return props.tags.filter((t) => (t.tagName || '').toLowerCase().includes(kw))
})

function updatePosition() {
  const el = triggerRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const gap = 6
  const maxPanel = 280
  const spaceBelow = window.innerHeight - rect.bottom - gap - 12
  const spaceAbove = rect.top - gap - 12
  const openUp = spaceBelow < maxPanel && spaceAbove > spaceBelow
  const maxHeight = Math.min(maxPanel, Math.max(openUp ? spaceAbove : spaceBelow, 120))
  panelStyle.value = openUp
    ? {
        left: `${rect.left}px`,
        width: `${rect.width}px`,
        bottom: `${window.innerHeight - rect.top + gap}px`,
        maxHeight: `${maxHeight}px`,
      }
    : {
        left: `${rect.left}px`,
        width: `${rect.width}px`,
        top: `${rect.bottom + gap}px`,
        maxHeight: `${maxHeight}px`,
      }
}

async function toggleOpen() {
  open.value = !open.value
  if (open.value) {
    keyword.value = ''
    resetCreate()
    await nextTick()
    updatePosition()
  }
}

function close() {
  open.value = false
  resetCreate()
}

function toggleTag(id) {
  const next = selectedIds.value.includes(id)
    ? selectedIds.value.filter((item) => item !== id)
    : [...selectedIds.value, id]
  emit('update:modelValue', next.join(','))
}

function clearAll() {
  emit('update:modelValue', '')
}

function resetCreate() {
  creating.value = false
  newTagName.value = ''
  createError.value = ''
  saving.value = false
}

async function startCreate() {
  creating.value = true
  newTagName.value = ''
  createError.value = ''
  await nextTick()
  createInputRef.value?.focus()
}

function selectTag(id) {
  if (selectedIds.value.includes(id)) return
  emit('update:modelValue', [...selectedIds.value, id].join(','))
}

async function confirmCreate() {
  const name = newTagName.value.trim()
  if (!name || saving.value) return
  const existing = props.tags.find((t) => (t.tagName || '').toLowerCase() === name.toLowerCase())
  if (existing) {
    selectTag(existing.tagId)
    resetCreate()
    return
  }
  saving.value = true
  createError.value = ''
  try {
    const ok = await tagApi.save({ tagName: name })
    if (!ok) throw new Error('新增标签失败')
    const list = await tagApi.list()
    const createdTag = list.find((t) => (t.tagName || '').toLowerCase() === name.toLowerCase())
    emit('created', list)
    if (createdTag) selectTag(createdTag.tagId)
    resetCreate()
  } catch (e) {
    createError.value = e.message || '新增标签失败'
  } finally {
    saving.value = false
  }
}

function onDocumentPointerDown(e) {
  if (!open.value) return
  const target = e.target
  if (triggerRef.value?.contains(target)) return
  if (panelRef.value?.contains(target)) return
  close()
}

function onDocumentKeydown(e) {
  if (e.key === 'Escape' && open.value) close()
}

function onViewportChange() {
  if (open.value) updatePosition()
}

onMounted(() => {
  document.addEventListener('mousedown', onDocumentPointerDown, true)
  document.addEventListener('keydown', onDocumentKeydown)
  window.addEventListener('resize', onViewportChange)
  window.addEventListener('scroll', onViewportChange, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDocumentPointerDown, true)
  document.removeEventListener('keydown', onDocumentKeydown)
  window.removeEventListener('resize', onViewportChange)
  window.removeEventListener('scroll', onViewportChange, true)
})
</script>

<template>
  <button
    :id="inputId"
    ref="triggerRef"
    type="button"
    class="tag-select-trigger"
    :class="{ open, empty: !selectedTags.length }"
    :aria-expanded="open"
    @click="toggleOpen"
  >
    <template v-if="selectedTags.length">
      <span v-for="t in visibleChips" :key="t.tagId" class="chip">{{ t.tagName }}</span>
      <span v-if="hiddenCount" class="chip more">+{{ hiddenCount }}</span>
    </template>
    <span v-else class="placeholder">{{ placeholder }}</span>
    <span class="caret" aria-hidden="true"></span>
  </button>

  <Teleport to="body">
    <div v-if="open" ref="panelRef" class="tag-select-panel" :style="panelStyle">
      <div class="panel-search">
        <input v-model="keyword" type="text" class="search-input" placeholder="搜索标签" />
      </div>
      <div class="panel-list" role="listbox" aria-multiselectable="true">
        <button
          v-for="t in filteredTags"
          :key="t.tagId"
          type="button"
          class="option"
          role="option"
          :aria-selected="selectedIds.includes(t.tagId)"
          @click="toggleTag(t.tagId)"
        >
          <span class="checkbox" :class="{ checked: selectedIds.includes(t.tagId) }"></span>
          <span class="option-name">{{ t.tagName }}</span>
        </button>
        <p v-if="!filteredTags.length" class="panel-empty">没有匹配的标签</p>
      </div>
      <div class="panel-footer" :class="{ creating }">
        <template v-if="creating">
          <div class="create-row">
            <input
              ref="createInputRef"
              v-model="newTagName"
              type="text"
              class="create-input"
              placeholder="输入新标签名称"
              :disabled="saving"
              @keydown.enter.prevent="confirmCreate"
              @keydown.esc.stop.prevent="resetCreate"
            />
            <button type="button" class="link-btn" :disabled="!newTagName.trim() || saving" @click="confirmCreate">
              {{ saving ? '保存中…' : '确定' }}
            </button>
            <button type="button" class="link-btn muted" :disabled="saving" @click="resetCreate">取消</button>
          </div>
          <p v-if="createError" class="create-error">{{ createError }}</p>
        </template>
        <template v-else>
          <span class="count">已选 {{ selectedTags.length }} 个</span>
          <div class="footer-actions">
            <button v-if="allowCreate" type="button" class="link-btn" @click="startCreate">新增标签</button>
            <button type="button" class="link-btn" :disabled="!selectedTags.length" @click="clearAll">
              清空
            </button>
          </div>
        </template>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.tag-select-trigger {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-height: 34px;
  padding: 4px 30px 4px 8px;
  box-sizing: border-box;
  font-size: 14px;
  text-align: left;
  background: #ffffff;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  transition: border-color 0.15s;
}

.tag-select-trigger:hover {
  border-color: #9ca3af;
}

.tag-select-trigger.open,
.tag-select-trigger:focus-visible {
  border-color: #3b82f6;
  outline: none;
}

.tag-select-trigger .placeholder {
  color: #9ca3af;
}

.chip {
  display: inline-block;
  padding: 1px 8px;
  font-size: 12px;
  color: #1d4ed8;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  white-space: nowrap;
}

.chip.more {
  color: #6b7280;
  background: #f3f4f6;
  border-color: #e5e7eb;
}

.caret {
  position: absolute;
  right: 12px;
  top: 50%;
  width: 7px;
  height: 7px;
  border-right: 2px solid #9ca3af;
  border-bottom: 2px solid #9ca3af;
  transform: translateY(-70%) rotate(45deg);
  transition: transform 0.15s;
}

.tag-select-trigger.open .caret {
  transform: translateY(-30%) rotate(225deg);
}

.tag-select-panel {
  position: fixed;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.16);
  overflow: hidden;
}

.panel-search {
  padding: 8px;
  border-bottom: 1px solid #f3f4f6;
}

.search-input {
  width: 100%;
  height: 30px;
  padding: 0 8px;
  box-sizing: border-box;
  font-size: 13px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  outline: none;
}

.search-input:focus {
  border-color: #3b82f6;
}

.panel-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 4px;
}

.option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 7px 8px;
  font-size: 13px;
  color: #374151;
  text-align: left;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.option:hover {
  background: #f3f4f6;
}

.checkbox {
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  position: relative;
}

.checkbox.checked {
  background: #3b82f6;
  border-color: #3b82f6;
}

.checkbox.checked::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 1px;
  width: 4px;
  height: 7px;
  border-right: 2px solid #ffffff;
  border-bottom: 2px solid #ffffff;
  transform: rotate(45deg);
}

.option-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-empty {
  margin: 0;
  padding: 16px 8px;
  font-size: 13px;
  color: #9ca3af;
  text-align: center;
}

.panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 10px;
  border-top: 1px solid #f3f4f6;
}

.panel-footer.creating {
  flex-direction: column;
  align-items: stretch;
  gap: 4px;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.create-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.create-input {
  flex: 1;
  min-width: 0;
  height: 28px;
  padding: 0 8px;
  box-sizing: border-box;
  font-size: 13px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  outline: none;
}

.create-input:focus {
  border-color: #3b82f6;
}

.create-error {
  margin: 0;
  font-size: 12px;
  color: #dc2626;
}

.count {
  font-size: 12px;
  color: #6b7280;
}

.link-btn {
  padding: 0;
  font-size: 12px;
  color: #3b82f6;
  background: none;
  border: none;
  cursor: pointer;
}

.link-btn:disabled {
  color: #d1d5db;
  cursor: not-allowed;
}

.link-btn.muted {
  color: #6b7280;
}
</style>
