<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { createCrudApi } from '../api/crud'
import { historyApi } from '../api/history'

const taskApi = createCrudApi('/personalTask')
const statusApi = createCrudApi('/ptStatus')
const priorityApi = createCrudApi('/prioritys')
const tagApi = createCrudApi('/tags')

const historyTasks = ref([])
const statuses = ref([])
const priorities = ref([])
const tags = ref([])
const loading = ref(false)
const message = ref('')

// 筛选条件
const keyword = ref('')
const keywordEffective = ref('')
let kwTimer = null
watch(keyword, (v) => {
  if (kwTimer) clearTimeout(kwTimer)
  kwTimer = setTimeout(() => {
    keywordEffective.value = (v || '').trim().toLowerCase()
  }, 300)
})

const filters = reactive({
  statusId: '',
  priorityId: '',
  tagId: '',
  archivedStart: '',
  archivedEnd: '',
})

// 仅已标记为历史的状态进入状态筛选下拉
const historyStatuses = computed(() => statuses.value.filter((s) => s.isHistory === 1))

const statusNameMap = computed(() => {
  const map = {}
  statuses.value.forEach((s) => { map[s.ptStatusId] = s.ptStatusName })
  return map
})
const priorityNameMap = computed(() => {
  const map = {}
  priorities.value.forEach((p) => { map[p.priorityId] = p.priorityName })
  return map
})
const tagNameMap = computed(() => {
  const map = {}
  tags.value.forEach((t) => { map[t.tagId] = t.tagName })
  return map
})

// 任务标签以逗号分隔的标签id存储
function tagIdsOf(task) {
  return (task?.tagId || '')
    .split(',')
    .map((id) => id.trim())
    .filter(Boolean)
}

function tagNamesOf(task) {
  return tagIdsOf(task)
    .map((id) => tagNameMap.value[id])
    .filter(Boolean)
}

// 优先级颜色：与看板、优先级管理页保持一致（排序值越小越红，越大越绿）
const priorityStyleMap = computed(() => {
  const list = priorities.value.filter(
    (p) => p.priorityOrder !== null && p.priorityOrder !== undefined && p.priorityOrder !== '',
  )
  const nums = list.map((p) => Number(p.priorityOrder)).filter((n) => Number.isFinite(n))
  const map = {}
  if (!nums.length) return map
  const min = Math.min(...nums)
  const max = Math.max(...nums)
  list.forEach((p) => {
    const current = Number(p.priorityOrder)
    if (!Number.isFinite(current)) return
    const ratio = max === min ? 1 : (current - min) / (max - min)
    const hue = Math.round(ratio * 120)
    map[p.priorityId] = {
      color: `hsl(${hue}, 72%, 45%)`,
      background: `hsl(${hue}, 80%, 94%)`,
    }
  })
  return map
})

function showMessage(text) {
  message.value = text
  window.setTimeout(() => { if (message.value === text) message.value = '' }, 2000)
}

async function loadData() {
  loading.value = true
  try {
    const [taskList, statusList, priorityList, tagList] = await Promise.all([
      historyApi.list(),
      statusApi.list(),
      priorityApi.list(),
      tagApi.list(),
    ])
    historyTasks.value = taskList || []
    statuses.value = [...(statusList || [])].sort((a, b) => (a.ptStatusOrder ?? 0) - (b.ptStatusOrder ?? 0))
    priorities.value = priorityList || []
    tags.value = tagList || []
  } catch (e) {
    showMessage(`加载失败：${e.message}`)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  keywordEffective.value = ''
  filters.statusId = ''
  filters.priorityId = ''
  filters.tagId = ''
  filters.archivedStart = ''
  filters.archivedEnd = ''
}

const filteredTasks = computed(() => {
  const kw = keywordEffective.value
  return historyTasks.value.filter((t) => {
    if (kw) {
      const title = (t.title || '').toLowerCase()
      const content = (t.content || '').toLowerCase()
      if (!title.includes(kw) && !content.includes(kw)) return false
    }
    if (filters.statusId && t.ptStatusId !== filters.statusId) return false
    if (filters.priorityId && t.priorityId !== filters.priorityId) return false
    if (filters.tagId && !tagIdsOf(t).includes(filters.tagId)) return false
    if (filters.archivedStart || filters.archivedEnd) {
      const refTime = t.archivedTime || t.updatedTime
      const d = refTime ? new Date(refTime) : null
      if (!d || isNaN(d.getTime())) return false
      if (filters.archivedStart) {
        const start = new Date(`${filters.archivedStart}T00:00:00`)
        if (d < start) return false
      }
      if (filters.archivedEnd) {
        const end = new Date(`${filters.archivedEnd}T23:59:59`)
        if (d > end) return false
      }
    }
    return true
  })
})

// 排序：归档时间降序（空值最后）→ 创建时间降序 → 更新时间降序
const sortedTasks = computed(() => {
  return [...filteredTasks.value].sort((a, b) => {
    const ta = a.archivedTime ? new Date(a.archivedTime) : null
    const tb = b.archivedTime ? new Date(b.archivedTime) : null
    if (ta && tb) {
      const diff = tb - ta
      if (diff !== 0) return diff
    } else if (ta) {
      return -1
    } else if (tb) {
      return 1
    }
    const ca = a.createdTme ? new Date(a.createdTme).getTime() : 0
    const cb = b.createdTme ? new Date(b.createdTme).getTime() : 0
    if (cb - ca !== 0) return cb - ca
    const ua = a.updatedTime ? new Date(a.updatedTime).getTime() : 0
    const ub = b.updatedTime ? new Date(b.updatedTime).getTime() : 0
    return ub - ua
  })
})

function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (isNaN(d.getTime())) return String(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function truncate(text, max = 60) {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '…' : text
}

// 归档时间展示：为空时用更新时间兜底
function archivedDisplay(t) {
  return t.archivedTime ? t.archivedTime : t.updatedTime
}

// 只读详情
const selectedTask = ref(null)
const maskDown = ref(false)

function openDetail(task) {
  selectedTask.value = task
}
function closeDetail() {
  selectedTask.value = null
}
function onMaskUp() {
  if (maskDown.value) closeDetail()
  maskDown.value = false
}

function onKeydown(e) {
  if (e.key === 'Escape' && selectedTask.value) {
    closeDetail()
    e.stopPropagation()
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => {
  if (kwTimer) clearTimeout(kwTimer)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div class="page">
    <header class="page-header">
      <h2 class="page-title">历史任务回看</h2>
      <span v-if="message" class="page-message">{{ message }}</span>
      <span class="result-count" v-if="!loading">共 {{ sortedTasks.length }} 条</span>
    </header>

    <section class="panel filter-panel">
      <div class="filter-row">
        <div class="filter-item keyword">
          <label class="filter-label">关键字</label>
          <input
            v-model="keyword"
            class="filter-input"
            type="text"
            placeholder="匹配任务标题或内容"
          />
        </div>
        <div class="filter-item">
          <label class="filter-label">状态</label>
          <select v-model="filters.statusId" class="filter-input">
            <option value="">全部</option>
            <option v-for="s in historyStatuses" :key="s.ptStatusId" :value="s.ptStatusId">
              {{ s.ptStatusName }}
            </option>
          </select>
        </div>
        <div class="filter-item">
          <label class="filter-label">优先级</label>
          <select v-model="filters.priorityId" class="filter-input">
            <option value="">全部</option>
            <option v-for="p in priorities" :key="p.priorityId" :value="p.priorityId">
              {{ p.priorityName }}
            </option>
          </select>
        </div>
        <div class="filter-item">
          <label class="filter-label">标签</label>
          <select v-model="filters.tagId" class="filter-input">
            <option value="">全部</option>
            <option v-for="t in tags" :key="t.tagId" :value="t.tagId">{{ t.tagName }}</option>
          </select>
        </div>
        <div class="filter-item date-range">
          <label class="filter-label">归档时间</label>
          <div class="date-range-inputs">
            <input v-model="filters.archivedStart" class="filter-input" type="date" />
            <span class="date-sep">至</span>
            <input v-model="filters.archivedEnd" class="filter-input" type="date" />
          </div>
        </div>
        <div class="filter-item actions">
          <button type="button" class="btn ghost" @click="resetFilters">重置</button>
          <button type="button" class="btn primary" @click="loadData">刷新</button>
        </div>
      </div>
    </section>

    <section class="panel">
      <div v-if="loading" class="empty">加载中…</div>
      <div v-else-if="!historyTasks.length" class="empty">
        暂无历史任务，任务流转到历史状态后会出现在这里
      </div>
      <div v-else-if="!sortedTasks.length" class="empty">暂无符合条件的历史任务</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>任务标题</th>
              <th>内容摘要</th>
              <th>状态</th>
              <th>优先级</th>
              <th>标签</th>
              <th>归档时间</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="task in sortedTasks"
              :key="task.ptId"
              class="task-row"
              @click="openDetail(task)"
            >
              <td class="title-cell">{{ task.title || '未命名任务' }}</td>
              <td class="content-cell">{{ truncate(task.content) }}</td>
              <td>{{ statusNameMap[task.ptStatusId] || '未设置' }}</td>
              <td>
                <span
                  v-if="task.priorityId && priorityNameMap[task.priorityId]"
                  class="chip priority"
                  :style="priorityStyleMap[task.priorityId]"
                >
                  {{ priorityNameMap[task.priorityId] }}
                </span>
                <span v-else class="muted">未设置</span>
              </td>
              <td>{{ tagNamesOf(task).join('、') || '未设置' }}</td>
              <td>
                {{ formatDateTime(archivedDisplay(task)) }}
                <span v-if="!task.archivedTime" class="muted note">（更新时间兜底）</span>
              </td>
              <td class="muted">{{ formatDateTime(task.createdTme) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <Teleport to="body">
      <div
        v-if="selectedTask"
        class="modal-mask"
        @mousedown.self="maskDown = true"
        @mouseup.self="onMaskUp"
      >
        <div class="detail-dialog" role="dialog" aria-modal="true" aria-label="历史任务详情" @mousedown="maskDown = false">
          <header class="detail-header">
            <h3 class="detail-title">历史任务详情</h3>
            <button type="button" class="detail-close" @click="closeDetail" aria-label="关闭">×</button>
          </header>
          <div class="detail-body">
            <section class="detail-section">
              <div class="detail-label">任务标题</div>
              <div class="detail-value">{{ selectedTask.title || '未命名任务' }}</div>
            </section>
            <section class="detail-section">
              <div class="detail-label">任务内容</div>
              <div class="detail-value content">{{ selectedTask.content || '-' }}</div>
            </section>
            <section class="detail-section">
              <div class="detail-label">状态</div>
              <div class="detail-value">{{ statusNameMap[selectedTask.ptStatusId] || '未设置' }}</div>
            </section>
            <section class="detail-section">
              <div class="detail-label">优先级</div>
              <div class="detail-value">
                <span
                  v-if="selectedTask.priorityId && priorityNameMap[selectedTask.priorityId]"
                  class="chip priority"
                  :style="priorityStyleMap[selectedTask.priorityId]"
                >
                  {{ priorityNameMap[selectedTask.priorityId] }}
                </span>
                <span v-else>未设置</span>
              </div>
            </section>
            <section class="detail-section">
              <div class="detail-label">标签</div>
              <div class="detail-value">{{ tagNamesOf(selectedTask).join('、') || '未设置' }}</div>
            </section>
            <section class="detail-section">
              <div class="detail-label">创建时间</div>
              <div class="detail-value">{{ formatDateTime(selectedTask.createdTme) }}</div>
            </section>
            <section class="detail-section">
              <div class="detail-label">更新时间</div>
              <div class="detail-value">{{ formatDateTime(selectedTask.updatedTime) }}</div>
            </section>
            <section class="detail-section">
              <div class="detail-label">归档时间</div>
              <div class="detail-value">
                {{ formatDateTime(archivedDisplay(selectedTask)) }}
                <span v-if="!selectedTask.archivedTime" class="muted note">（更新时间兜底）</span>
              </div>
            </section>
          </div>
          <footer class="detail-footer">
            <button type="button" class="btn primary" @click="closeDetail">关闭</button>
          </footer>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  box-sizing: border-box;
  overflow-y: auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.page-message {
  font-size: 13px;
  color: #3b82f6;
}

.result-count {
  margin-left: auto;
  font-size: 13px;
  color: #6b7280;
}

.panel {
  background: #ffffff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 16px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-item.keyword {
  min-width: 240px;
}

.filter-item.date-range {
  min-width: 320px;
}

.filter-label {
  font-size: 13px;
  color: #4b5563;
}

.filter-input {
  height: 34px;
  padding: 0 10px;
  font-size: 14px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  outline: none;
  box-sizing: border-box;
  background: #ffffff;
}

.filter-input:focus {
  border-color: #3b82f6;
}

.date-range-inputs {
  display: flex;
  align-items: center;
  gap: 6px;
}

.date-range-inputs .filter-input {
  flex: 1;
  min-width: 0;
}

.date-sep {
  font-size: 13px;
  color: #6b7280;
}

.filter-item.actions {
  flex-direction: row;
  align-items: flex-end;
  gap: 8px;
  margin-left: auto;
}

.btn {
  height: 34px;
  padding: 0 16px;
  font-size: 14px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #ffffff;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.btn:hover {
  border-color: #9ca3af;
  background: #f9fafb;
}

.btn.primary {
  background: #3b82f6;
  border-color: #3b82f6;
  color: #ffffff;
}

.btn.primary:hover {
  background: #2563eb;
  border-color: #2563eb;
}

.btn.ghost {
  color: #6b7280;
}

.table-wrap {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.table th,
.table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #f3f4f6;
  vertical-align: top;
}

.table th {
  background: #f9fafb;
  color: #4b5563;
  font-weight: 600;
  white-space: nowrap;
}

.task-row {
  cursor: pointer;
  transition: background 0.15s;
}

.task-row:hover {
  background: #f9fafb;
}

.title-cell {
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
}

.content-cell {
  max-width: 320px;
  color: #4b5563;
}

.muted {
  color: #9ca3af;
}

.note {
  font-size: 12px;
}

.chip {
  display: inline-block;
  padding: 2px 10px;
  font-size: 12px;
  border-radius: 999px;
  white-space: nowrap;
}

.chip.priority {
  font-weight: 500;
}

.empty {
  text-align: center;
  color: #9ca3af;
  padding: 32px 0;
  font-size: 14px;
}

/* 只读详情弹框 */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.detail-dialog {
  width: 560px;
  max-width: calc(100vw - 40px);
  max-height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.detail-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f3f4f6;
}

.detail-title {
  margin: 0;
  font-size: 16px;
  color: #111827;
}

.detail-close {
  margin-left: auto;
  border: none;
  background: none;
  font-size: 22px;
  line-height: 1;
  color: #9ca3af;
  cursor: pointer;
}

.detail-close:hover {
  color: #374151;
}

.detail-body {
  padding: 16px 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  font-size: 12px;
  color: #6b7280;
}

.detail-value {
  font-size: 14px;
  color: #111827;
  word-break: break-word;
}

.detail-value.content {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #374151;
}

.detail-footer {
  padding: 14px 20px;
  border-top: 1px solid #f3f4f6;
  display: flex;
  justify-content: flex-end;
}
</style>
