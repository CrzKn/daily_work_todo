<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { createCrudApi } from '../api/crud'

const taskApi = createCrudApi('/personalTask')
const statusApi = createCrudApi('/ptStatus')
const priorityApi = createCrudApi('/prioritys')
const tagApi = createCrudApi('/tags')

const statuses = ref([])
const priorities = ref([])
const tags = ref([])
const tasks = ref([])
const draggedId = ref('')
const dragOverId = ref('')
const selectedTaskId = ref('')
const selectedTask = ref(null)
const detailLoading = ref(false)
const message = ref('')
const collapsed = ref({})
const showCreate = ref(false)
const submitting = ref(false)
const createForm = ref({ title: '', content: '', tagId: '', priorityId: '' })

const COLUMN_COLORS = [
  '#9ca3af',
  '#3b82f6',
  '#f59e0b',
  '#10b981',
  '#ef4444',
  '#8b5cf6',
  '#06b6d4',
  '#ec4899',
]

const columns = computed(() => {
  const knownIds = new Set(statuses.value.map((status) => status.ptStatusId))
  const result = statuses.value.map((status, index) => ({
    id: status.ptStatusId,
    name: status.ptStatusName,
    color: COLUMN_COLORS[index % COLUMN_COLORS.length],
    tasks: tasks.value.filter((task) => task.ptStatusId === status.ptStatusId),
  }))
  const uncategorized = tasks.value.filter((task) => !knownIds.has(task.ptStatusId))
  if (uncategorized.length) {
    result.push({ id: '', name: '未分类', color: '#d1d5db', tasks: uncategorized })
  }
  return result
})

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (isNaN(date.getTime())) return String(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatDateOnly(value) {
  if (!value) return ''
  const date = new Date(value)
  if (isNaN(date.getTime())) return String(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function truncate(text, maxLen = 60) {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '…' : text
}

function toggleColumn(id) {
  collapsed.value[id] = !collapsed.value[id]
}

function showMessage(text) {
  message.value = text
  window.setTimeout(() => {
    if (message.value === text) message.value = ''
  }, 2000)
}

async function loadData() {
  try {
    const [statusList, taskList, priorityList, tagList] = await Promise.all([
      statusApi.list(),
      taskApi.list(),
      priorityApi.list(),
      tagApi.list(),
    ])
    statuses.value = [...statusList].sort(
      (a, b) => (a.ptStatusOrder ?? 0) - (b.ptStatusOrder ?? 0),
    )
    priorities.value = priorityList
    tags.value = tagList
    tasks.value = taskList
  } catch (e) {
    showMessage(`加载失败：${e.message}`)
  }
}

async function openDetail(task) {
  selectedTaskId.value = task.ptId
  detailLoading.value = true
  selectedTask.value = null
  try {
    selectedTask.value = await taskApi.getById(task.ptId)
  } catch (e) {
    showMessage(`加载详情失败：${e.message}`)
    selectedTaskId.value = ''
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  selectedTaskId.value = ''
  selectedTask.value = null
  deleteConfirmOpen.value = false
}

async function saveTaskField(field) {
  const task = tasks.value.find((t) => t.ptId === selectedTaskId.value)
  if (!task || !selectedTask.value) return
  if ((task[field] || '') === (selectedTask.value[field] || '')) return
  const oldValue = task[field]
  // 下拉框“不设置”为空字符串，提交时转为 null
  const payload = { ...selectedTask.value }
  if (payload.priorityId === '') payload.priorityId = null
  if (payload.tagId === '') payload.tagId = null
  try {
    const ok = await taskApi.update(payload)
    if (!ok) throw new Error('后端返回失败')
    task[field] = selectedTask.value[field] || null
    showMessage('已保存')
  } catch (e) {
    selectedTask.value[field] = oldValue || ''
    showMessage(`保存失败：${e.message}`)
  }
}

const deleteConfirmOpen = ref(false)

function askDelete() {
  deleteConfirmOpen.value = true
}

function cancelDelete() {
  deleteConfirmOpen.value = false
}

async function confirmDelete() {
  const task = selectedTask.value
  deleteConfirmOpen.value = false
  if (!task) return
  try {
    const ok = await taskApi.remove(task.ptId)
    if (!ok) throw new Error('后端返回失败')
    tasks.value = tasks.value.filter((t) => t.ptId !== task.ptId)
    showMessage(`【${task.title || '未命名任务'}】已删除`)
    closeDetail()
  } catch (e) {
    showMessage(`删除失败：${e.message}`)
  }
}

function openCreate() {
  createForm.value = { title: '', content: '', tagId: '', priorityId: '' }
  showCreate.value = true
}

function closeCreate() {
  showCreate.value = false
  submitting.value = false
}

async function submitCreate() {
  const { title, content, tagId, priorityId } = createForm.value
  if (!title.trim()) {
    showMessage('请填写任务标题')
    return
  }
  if (!content.trim()) {
    showMessage('请填写任务内容')
    return
  }
  submitting.value = true
  try {
    const ok = await taskApi.save({
      title: title.trim(),
      content: content.trim(),
      tagId: tagId || null,
      priorityId: priorityId || null,
    })
    if (!ok) throw new Error('后端返回失败')
    showMessage(`【${title.trim()}】已创建`)
    closeCreate()
    await loadData()
  } catch (e) {
    showMessage(`创建失败：${e.message}`)
  } finally {
    submitting.value = false
  }
}

function onKeydown(e) {
  if (e.key === 'Escape') {
    if (deleteConfirmOpen.value) cancelDelete()
    else if (showCreate.value) closeCreate()
    else if (selectedTaskId.value) closeDetail()
  }
}

function onDragStart(task, event) {
  draggedId.value = task.ptId
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', task.ptId)
}

function onDragEnd() {
  draggedId.value = ''
  dragOverId.value = ''
}

function onDragOver(column) {
  if (column.id) dragOverId.value = column.id
}

function onDragLeave(column) {
  if (dragOverId.value === column.id) dragOverId.value = ''
}

async function onDrop(column) {
  const taskId = draggedId.value
  draggedId.value = ''
  dragOverId.value = ''
  if (!taskId || !column.id) return

  const task = tasks.value.find((item) => item.ptId === taskId)
  if (!task || task.ptStatusId === column.id) return

  const previousStatusId = task.ptStatusId
  task.ptStatusId = column.id
  try {
    const ok = await taskApi.update({ ptId: taskId, ptStatusId: column.id })
    if (!ok) throw new Error('后端返回失败')
    showMessage(`【${task.title || '未命名任务'}】已移动到${column.name}`)
    if (selectedTaskId.value === taskId && selectedTask.value) {
      selectedTask.value = { ...selectedTask.value, ptStatusId: column.id }
    }
  } catch (e) {
    task.ptStatusId = previousStatusId
    showMessage(`移动失败：${e.message}`)
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div class="board">
    <header class="board-header">
      <h2 class="board-title">任务看板</h2>
      <span v-if="message" class="board-message">{{ message }}</span>
      <button type="button" class="btn-new" @click="openCreate">+ 新建任务</button>
    </header>

    <div class="columns">
      <section
        v-for="column in columns"
        :key="column.name"
        class="column"
        :class="{ 'drop-active': dragOverId === column.id }"
        @dragover.prevent="onDragOver(column)"
        @dragleave="onDragLeave(column)"
        @drop.prevent="onDrop(column)"
      >
        <header class="column-header">
          <span class="dot" :style="{ background: column.color }"></span>
          <span class="column-name">{{ column.name }}</span>
          <span class="count">{{ column.tasks.length }}</span>
          <button
            type="button"
            class="col-toggle"
            :title="collapsed[column.id] ? '展开任务内容' : '收起任务内容'"
            @click.stop="toggleColumn(column.id)"
          >
            {{ collapsed[column.id] ? '展开' : '收起' }}
          </button>
        </header>

        <div class="cards">
          <article
            v-for="task in column.tasks"
            :key="task.ptId"
            class="card"
            :class="{ dragging: draggedId === task.ptId, selected: selectedTaskId === task.ptId }"
            draggable="true"
            @click="openDetail(task)"
            @dragstart="onDragStart(task, $event)"
            @dragend="onDragEnd"
          >
            <div v-if="task.title" class="card-title">{{ task.title }}</div>
            <p v-if="task.content && !collapsed[column.id]" class="card-content">{{ truncate(task.content) }}</p>
            <p class="card-meta">创建于 {{ formatDateOnly(task.createdTme) }}</p>
          </article>

          <p v-if="!column.tasks.length" class="empty">这个状态还没有任务</p>
        </div>
      </section>
    </div>
  </div>

  <Teleport to="body">
    <div v-if="selectedTaskId" class="modal-mask" @click.self="closeDetail">
      <div class="detail-dialog" role="dialog" aria-modal="true" aria-label="任务详情">
        <header class="detail-header">
          <h3 class="detail-title">任务详情</h3>
          <button type="button" class="detail-close" @click="closeDetail" aria-label="关闭">×</button>
        </header>

        <div v-if="detailLoading" class="detail-loading">加载中…</div>

        <div v-else-if="selectedTask" class="detail-body">
          <section class="detail-section">
            <label class="detail-label" for="dt-title">任务标题</label>
            <input
              id="dt-title"
              v-model="selectedTask.title"
              class="form-input"
              placeholder="请输入任务标题"
              maxlength="200"
              @blur="saveTaskField('title')"
            />
          </section>

          <section class="detail-section">
            <label class="detail-label" for="dt-content">任务内容</label>
            <textarea
              id="dt-content"
              v-model="selectedTask.content"
              class="form-textarea"
              placeholder="请输入任务内容"
              rows="5"
              @blur="saveTaskField('content')"
            ></textarea>
          </section>

          <section class="detail-section">
            <label class="detail-label" for="dt-priority">优先级</label>
            <select
              id="dt-priority"
              v-model="selectedTask.priorityId"
              class="form-input"
              @change="saveTaskField('priorityId')"
            >
              <option value="">不设置</option>
              <option v-for="p in priorities" :key="p.priorityId" :value="p.priorityId">{{ p.priorityName }}</option>
            </select>
          </section>

          <section class="detail-section">
            <label class="detail-label" for="dt-tag">标签</label>
            <select
              id="dt-tag"
              v-model="selectedTask.tagId"
              class="form-input"
              @change="saveTaskField('tagId')"
            >
              <option value="">不设置</option>
              <option v-for="t in tags" :key="t.tagId" :value="t.tagId">{{ t.tagName }}</option>
            </select>
          </section>

          <section class="detail-section">
            <div class="detail-label">创建时间</div>
            <div class="detail-value">{{ formatDate(selectedTask.createdTme) }}</div>
          </section>

          <section class="detail-section">
            <div class="detail-label">更新时间</div>
            <div class="detail-value">{{ formatDate(selectedTask.updatedTime) }}</div>
          </section>
        </div>

        <footer v-if="selectedTask && !detailLoading" class="detail-footer">
          <button type="button" class="btn-delete" @click="askDelete">删除</button>
        </footer>
      </div>
    </div>

    <div v-if="deleteConfirmOpen" class="modal-mask confirm-mask" @click.self="cancelDelete">
      <div class="confirm-dialog" role="alertdialog" aria-modal="true" aria-label="删除确认">
        <div class="confirm-title">删除任务</div>
        <div class="confirm-text">确定删除任务「{{ selectedTask?.title || '未命名任务' }}」吗？删除后不可恢复。</div>
        <div class="confirm-actions">
          <button type="button" class="confirm-btn cancel" @click="cancelDelete">取消</button>
          <button type="button" class="confirm-btn danger" @click="confirmDelete">确定删除</button>
        </div>
      </div>
    </div>

    <div v-if="showCreate" class="modal-mask" @click.self="closeCreate">
      <form class="detail-dialog" role="dialog" aria-modal="true" aria-label="新建任务" @submit.prevent="submitCreate">
        <header class="detail-header">
          <h3 class="detail-title">新建任务</h3>
          <button type="button" class="detail-close" @click="closeCreate" aria-label="关闭">×</button>
        </header>

        <div class="detail-body">
          <section class="detail-section">
            <label class="detail-label" for="nc-title">任务标题</label>
            <input id="nc-title" v-model="createForm.title" class="form-input" placeholder="请输入任务标题" maxlength="200" />
          </section>

          <section class="detail-section">
            <label class="detail-label" for="nc-content">任务内容</label>
            <textarea
              id="nc-content"
              v-model="createForm.content"
              class="form-textarea"
              placeholder="请输入任务内容"
              rows="5"
            ></textarea>
          </section>

          <section class="detail-section">
            <label class="detail-label" for="nc-priority">优先级</label>
            <select id="nc-priority" v-model="createForm.priorityId" class="form-input">
              <option value="">不设置</option>
              <option v-for="p in priorities" :key="p.priorityId" :value="p.priorityId">{{ p.priorityName }}</option>
            </select>
          </section>

          <section class="detail-section">
            <label class="detail-label" for="nc-tag">标签</label>
            <select id="nc-tag" v-model="createForm.tagId" class="form-input">
              <option value="">不设置</option>
              <option v-for="t in tags" :key="t.tagId" :value="t.tagId">{{ t.tagName }}</option>
            </select>
          </section>
        </div>

        <footer class="dialog-footer">
          <button type="button" class="btn cancel" :disabled="submitting" @click="closeCreate">取消</button>
          <button type="submit" class="btn primary" :disabled="submitting">{{ submitting ? '创建中…' : '创建' }}</button>
        </footer>
      </form>
    </div>
  </Teleport>
</template>

<style scoped>
.board {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px 24px;
  box-sizing: border-box;
  gap: 16px;
}

.board-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.board-title {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.board-message {
  font-size: 13px;
  color: #3b82f6;
}

.btn-new {
  margin-left: auto;
  padding: 7px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #ffffff;
  background: #3b82f6;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s, box-shadow 0.15s;
}

.btn-new:hover {
  background: #2563eb;
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.3);
}

.columns {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 12px;
  overflow-x: auto;
}

.column {
  flex: 1 0 240px;
  max-width: 340px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: border-color 0.2s, background 0.2s;
}

.column.drop-active {
  border-color: #3b82f6;
  background: #eff6ff;
}

.column-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 14px 10px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.column-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.count {
  margin-left: auto;
  min-width: 22px;
  padding: 1px 7px;
  font-size: 12px;
  text-align: center;
  color: #6b7280;
  background: #f3f4f6;
  border-radius: 10px;
}

.col-toggle {
  margin-left: 6px;
  padding: 2px 8px;
  font-size: 12px;
  color: #3b82f6;
  background: transparent;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
}

.col-toggle:hover {
  background: #eff6ff;
}

.cards {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card {
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  cursor: grab;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.card:hover {
  border-color: #d1d5db;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.card.selected {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

.card.dragging {
  opacity: 0.5;
}

.card-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  color: #111827;
}

.card-content {
  margin: 0 0 8px;
  font-size: 13px;
  line-height: 1.5;
  color: #6b7280;
  white-space: pre-wrap;
  word-break: break-word;
}

.card-meta {
  margin: 0;
  font-size: 12px;
  color: #9ca3af;
}

.empty {
  margin: 24px 0;
  text-align: center;
  font-size: 13px;
  color: #d1d5db;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.15s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.detail-dialog {
  width: 480px;
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: dialogPop 0.18s ease-out;
}

@keyframes dialogPop {
  from {
    opacity: 0;
    transform: scale(0.94) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.detail-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.detail-close {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  font-size: 22px;
  line-height: 32px;
  color: #9ca3af;
  cursor: pointer;
  border-radius: 6px;
  padding: 0;
  transition: background 0.15s, color 0.15s;
}

.detail-close:hover {
  color: #374151;
  background: #f3f4f6;
}

.detail-loading {
  padding: 48px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
}

.detail-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.detail-value {
  font-size: 14px;
  color: #111827;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  padding: 12px 20px;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.btn-delete {
  padding: 8px 16px;
  font-size: 14px;
  color: #ffffff;
  background: #ef4444;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-delete:hover {
  background: #dc2626;
}

.confirm-mask {
  z-index: 3000;
}

.confirm-dialog {
  width: 360px;
  max-width: calc(100vw - 32px);
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.2);
  padding: 20px;
  animation: dialogPop 0.18s ease-out;
}

.confirm-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}

.confirm-text {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
  margin-bottom: 16px;
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.confirm-btn {
  padding: 7px 14px;
  font-size: 13px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.confirm-btn.cancel {
  color: #374151;
  background: #ffffff;
  border: 1px solid #d1d5db;
}

.confirm-btn.cancel:hover {
  background: #f3f4f6;
}

.confirm-btn.danger {
  color: #ffffff;
  background: #ef4444;
  border: none;
}

.confirm-btn.danger:hover {
  background: #dc2626;
}

.form-input,
.form-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 8px 10px;
  font-size: 14px;
  color: #111827;
  background: #ffffff;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  font-family: inherit;
}

.form-input:focus,
.form-textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.form-textarea {
  resize: vertical;
  min-height: 90px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px 16px;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.btn {
  padding: 7px 16px;
  font-size: 13px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.btn.cancel {
  color: #374151;
  background: #f3f4f6;
}

.btn.cancel:hover:not(:disabled) {
  background: #e5e7eb;
}

.btn.primary {
  color: #ffffff;
  background: #3b82f6;
}

.btn.primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
