<script setup>
import { ref, computed, onMounted } from 'vue'
import { createCrudApi } from '../api/crud'

const taskApi = createCrudApi('/personalTask')
const statusApi = createCrudApi('/ptStatus')

const statuses = ref([])
const tasks = ref([])
const draggedId = ref('')
const dragOverId = ref('')
const message = ref('')

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
  if (!value) return ''
  const date = new Date(value)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function showMessage(text) {
  message.value = text
  window.setTimeout(() => {
    if (message.value === text) message.value = ''
  }, 2000)
}

async function loadData() {
  try {
    const [statusList, taskList] = await Promise.all([statusApi.list(), taskApi.list()])
    statuses.value = [...statusList].sort(
      (a, b) => (a.ptStatusOrder ?? 0) - (b.ptStatusOrder ?? 0),
    )
    tasks.value = taskList
  } catch (e) {
    showMessage(`加载失败：${e.message}`)
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
    showMessage(`已移动到「${column.name}」`)
  } catch (e) {
    task.ptStatusId = previousStatusId
    showMessage(`移动失败：${e.message}`)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="board">
    <header class="board-header">
      <h2 class="board-title">任务看板</h2>
      <span v-if="message" class="board-message">{{ message }}</span>
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
        </header>

        <div class="cards">
          <article
            v-for="task in column.tasks"
            :key="task.ptId"
            class="card"
            :class="{ dragging: draggedId === task.ptId }"
            draggable="true"
            @dragstart="onDragStart(task, $event)"
            @dragend="onDragEnd"
          >
            <p class="card-content">{{ task.content }}</p>
            <p class="card-meta">创建于 {{ formatDate(task.createdTme) }}</p>
          </article>

          <p v-if="!column.tasks.length" class="empty">这个状态还没有任务</p>
        </div>
      </section>
    </div>
  </div>
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

.card.dragging {
  opacity: 0.5;
}

.card-content {
  margin: 0 0 8px;
  font-size: 14px;
  line-height: 1.5;
  color: #111827;
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
</style>
