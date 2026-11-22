<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { periodicApi } from '../api/periodic'
import { createCrudApi } from '../api/crud'
import PrioritySelect from '../components/PrioritySelect.vue'

const priorityApi = createCrudApi('/prioritys')
const tagApi = createCrudApi('/tags')

const rules = ref([])
const priorities = ref([])
const tags = ref([])
const loading = ref(false)
const message = ref('')

const showForm = ref(false)
const editingId = ref('')
const submitting = ref(false)
const form = ref(defaultForm())

const deleteTarget = ref(null)
const formMaskDown = ref(false)

const WEEK_TEXT = ['一', '二', '三', '四', '五', '六', '日']

function pad(n) {
  return String(n).padStart(2, '0')
}

function todayDate() {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate())
}

function toDateStr(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function fromDateStr(value) {
  if (!value) return null
  const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (!match) return null
  return new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]))
}

function addDays(date, n) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + n)
}

function sameDay(a, b) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

function isoWeekday(date) {
  return date.getDay() === 0 ? 7 : date.getDay()
}

function nextMonthly(base, day) {
  let cursor = new Date(base.getFullYear(), base.getMonth(), 1)
  for (let i = 0; i < 24; i++) {
    const length = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 0).getDate()
    if (day <= length) {
      const date = new Date(cursor.getFullYear(), cursor.getMonth(), day)
      if (date >= base) return date
    }
    cursor = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 1)
  }
  return null
}

function calcNextPlanDate(rule) {
  const today = todayDate()
  let base = fromDateStr(rule.startDate) || today
  if (base < today) base = today
  let candidate = null
  if (rule.cycleType === 'DAILY') {
    candidate = base
  } else if (rule.cycleType === 'WEEKLY') {
    const weekday = Number(rule.weeklyDay)
    if (!weekday) return null
    candidate = addDays(base, (weekday - isoWeekday(base) + 7) % 7)
  } else if (rule.cycleType === 'MONTHLY') {
    const day = Number(rule.monthlyDay)
    if (!day) return null
    candidate = nextMonthly(base, day)
  }
  if (!candidate) return null
  if (sameDay(candidate, today) && rule.lastGenerateDate === toDateStr(today)) {
    if (rule.cycleType === 'DAILY') candidate = addDays(candidate, 1)
    else if (rule.cycleType === 'WEEKLY') candidate = addDays(candidate, 7)
    else candidate = nextMonthly(addDays(candidate, 1), Number(rule.monthlyDay))
  }
  return candidate
}

function cycleLabel(rule) {
  if (rule.cycleType === 'DAILY') return '每天'
  if (rule.cycleType === 'WEEKLY') return `每周${WEEK_TEXT[Number(rule.weeklyDay) - 1] || ''}`
  if (rule.cycleType === 'MONTHLY') return `每月 ${Number(rule.monthlyDay)} 日`
  return '—'
}

function formatDateOnly(value) {
  if (!value) return '—'
  if (typeof value === 'string') {
    const match = value.match(/^(\d{4})-(\d{2})-(\d{2})/)
    if (match) return `${match[1]}-${match[2]}-${match[3]}`
  }
  const date = new Date(value)
  if (isNaN(date.getTime())) return String(value)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (isNaN(date.getTime())) return String(value)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function defaultForm() {
  const today = todayDate()
  return {
    title: '',
    content: '',
    priorityId: '',
    tagId: '',
    cycleType: 'DAILY',
    startDate: toDateStr(today),
    weeklyDay: isoWeekday(today),
    monthlyDay: today.getDate(),
  }
}

const formPreview = computed(() => {
  const rule = { ...form.value, enableStatus: 1 }
  const next = calcNextPlanDate(rule)
  return {
    cycle: cycleLabel(rule),
    next: next ? toDateStr(next) : '—',
  }
})

function showMessage(text) {
  message.value = text
  window.setTimeout(() => {
    if (message.value === text) message.value = ''
  }, 2200)
}

async function load() {
  loading.value = true
  try {
    try {
      await periodicApi.generate()
    } catch (e) {
      // 生成失败不阻塞列表展示，由列表标记失败并允许重试
    }
    rules.value = await periodicApi.list()
  } catch (e) {
    showMessage(`加载失败：${e.message}`)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = ''
  form.value = defaultForm()
  showForm.value = true
}

function openEdit(rule) {
  editingId.value = rule.periodicTaskId
  form.value = {
    title: rule.title || '',
    content: rule.content || '',
    priorityId: rule.priorityId || '',
    tagId: rule.tagId || '',
    cycleType: rule.cycleType || 'DAILY',
    startDate: formatDateOnly(rule.startDate),
    weeklyDay: rule.weeklyDay || isoWeekday(todayDate()),
    monthlyDay: rule.monthlyDay || todayDate().getDate(),
  }
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editingId.value = ''
  submitting.value = false
  formMaskDown.value = false
}

function onFormMaskUp() {
  if (formMaskDown.value) closeForm()
  formMaskDown.value = false
}

async function submit() {
  if (submitting.value) return
  const title = form.value.title.trim()
  const content = form.value.content.trim()
  if (!title) return showMessage('请填写任务标题')
  if (!content) return showMessage('请填写任务内容')
  if (!form.value.startDate) return showMessage('请选择开始日期')
  if (form.value.cycleType === 'WEEKLY' && !form.value.weeklyDay) {
    return showMessage('请选择每周执行日')
  }
  if (form.value.cycleType === 'MONTHLY') {
    const day = Number(form.value.monthlyDay)
    if (!Number.isInteger(day) || day < 1 || day > 31) {
      return showMessage('每月日期需为 1～31 的整数')
    }
  }
  const payload = {
    title,
    content,
    priorityId: form.value.priorityId || null,
    tagId: form.value.tagId || null,
    cycleType: form.value.cycleType,
    startDate: form.value.startDate,
    weeklyDay: form.value.cycleType === 'WEEKLY' ? Number(form.value.weeklyDay) : null,
    monthlyDay: form.value.cycleType === 'MONTHLY' ? Number(form.value.monthlyDay) : null,
  }
  submitting.value = true
  try {
    const ok = editingId.value
      ? await periodicApi.update({ ...payload, periodicTaskId: editingId.value })
      : await periodicApi.save(payload)
    if (!ok) throw new Error('后端返回失败')
    showMessage(editingId.value ? '规则已保存' : '规则已创建')
    closeForm()
    await load()
  } catch (e) {
    showMessage(`保存失败：${e.message}`)
  } finally {
    submitting.value = false
  }
}

async function toggleEnable(rule) {
  const enable = rule.enableStatus === 1 ? 0 : 1
  try {
    const ok = await periodicApi.setEnable(rule.periodicTaskId, enable)
    if (!ok) throw new Error('后端返回失败')
    showMessage(enable === 1 ? '已启用' : '已停用')
    await load()
  } catch (e) {
    showMessage(`操作失败：${e.message}`)
  }
}

function isFailToday(rule) {
  return rule.lastFailDate && rule.lastFailDate === toDateStr(todayDate())
}

async function retry(rule) {
  try {
    const ok = await periodicApi.retry(rule.periodicTaskId)
    if (!ok) throw new Error('本期任务仍生成失败')
    showMessage('已重新生成本期任务')
    await load()
  } catch (e) {
    showMessage(`重试失败：${e.message}`)
  }
}

function askDelete(rule) {
  deleteTarget.value = rule
}

function cancelDelete() {
  deleteTarget.value = null
}

async function confirmDelete() {
  const rule = deleteTarget.value
  deleteTarget.value = null
  if (!rule) return
  try {
    const ok = await periodicApi.remove(rule.periodicTaskId)
    if (!ok) throw new Error('后端返回失败')
    showMessage('规则已删除')
    await load()
  } catch (e) {
    showMessage(`删除失败：${e.message}`)
  }
}

function onKeydown(e) {
  if (e.key !== 'Escape') return
  if (deleteTarget.value) cancelDelete()
  else if (showForm.value) closeForm()
}

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  try {
    const [priorityList, tagList] = await Promise.all([priorityApi.list(), tagApi.list()])
    priorities.value = priorityList
    tags.value = tagList
  } catch (e) {
    showMessage(`基础数据加载失败：${e.message}`)
  }
  await load()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div class="periodic">
    <header class="page-header">
      <h2 class="page-title">周期任务</h2>
      <span v-if="message" class="page-message">{{ message }}</span>
      <button type="button" class="btn-new" @click="openCreate">+ 新建周期任务</button>
    </header>

    <div class="table-wrap">
      <table class="rule-table">
        <thead>
          <tr>
            <th>任务标题</th>
            <th>周期规则</th>
            <th>开始日期</th>
            <th>下次计划</th>
            <th>状态</th>
            <th>创建时间</th>
            <th class="ops-col">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="rule in rules" :key="rule.periodicTaskId">
            <td>
              <div class="rule-title">{{ rule.title || '未命名任务' }}</div>
              <div v-if="isFailToday(rule)" class="fail-hint">
                本期生成失败
                <button type="button" class="link-btn" @click="retry(rule)">重试</button>
              </div>
            </td>
            <td>{{ cycleLabel(rule) }}</td>
            <td>{{ formatDateOnly(rule.startDate) }}</td>
            <td>{{ rule.enableStatus === 1 ? formatDateOnly(rule.nextPlanDate) : '—' }}</td>
            <td>
              <span class="status-pill" :class="{ off: rule.enableStatus !== 1 }">
                {{ rule.enableStatus === 1 ? '启用中' : '已停用' }}
              </span>
            </td>
            <td>{{ formatDate(rule.createdTime) }}</td>
            <td class="ops-col">
              <button type="button" class="link-btn" @click="openEdit(rule)">修改</button>
              <button type="button" class="link-btn" @click="toggleEnable(rule)">
                {{ rule.enableStatus === 1 ? '停用' : '启用' }}
              </button>
              <button type="button" class="link-btn danger" @click="askDelete(rule)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!loading && !rules.length" class="empty">暂无周期任务，点击右上角新建</p>
      <p v-else-if="loading" class="empty">加载中…</p>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="modal-mask" @mousedown.self="formMaskDown = true" @mouseup.self="onFormMaskUp">
        <form
          class="dialog"
          role="dialog"
          aria-modal="true"
          aria-label="周期任务规则"
          @submit.prevent="submit"
          @mousedown="formMaskDown = false"
        >
          <header class="dialog-header">
            <h3 class="dialog-title">{{ editingId ? '修改周期任务' : '新建周期任务' }}</h3>
            <button type="button" class="dialog-close" aria-label="关闭" @click="closeForm">×</button>
          </header>

          <div class="dialog-body">
            <section class="field">
              <label class="field-label" for="pt-title">任务标题</label>
              <input id="pt-title" v-model="form.title" class="form-input" maxlength="200" placeholder="请输入任务标题" />
            </section>

            <section class="field">
              <label class="field-label" for="pt-content">任务内容</label>
              <textarea id="pt-content" v-model="form.content" class="form-textarea" rows="4" placeholder="请输入任务内容"></textarea>
            </section>

            <div class="field-row">
              <section class="field">
                <label class="field-label" for="pt-priority">优先级</label>
                <PrioritySelect v-model="form.priorityId" :priorities="priorities" input-id="pt-priority" />
              </section>

              <section class="field">
                <label class="field-label" for="pt-tag">标签</label>
                <select id="pt-tag" v-model="form.tagId" class="form-input">
                  <option value="">不设置</option>
                  <option v-for="t in tags" :key="t.tagId" :value="t.tagId">{{ t.tagName }}</option>
                </select>
              </section>
            </div>

            <section class="field">
              <div class="field-label">周期类型</div>
              <div class="radio-group">
                <label class="radio-item">
                  <input v-model="form.cycleType" type="radio" value="DAILY" />
                  <span>每日</span>
                </label>
                <label class="radio-item">
                  <input v-model="form.cycleType" type="radio" value="WEEKLY" />
                  <span>每周</span>
                </label>
                <label class="radio-item">
                  <input v-model="form.cycleType" type="radio" value="MONTHLY" />
                  <span>每月</span>
                </label>
              </div>
            </section>

            <div class="field-row">
              <section class="field">
                <label class="field-label" for="pt-start">开始日期</label>
                <input id="pt-start" v-model="form.startDate" type="date" class="form-input" />
              </section>

              <section v-if="form.cycleType === 'WEEKLY'" class="field">
                <label class="field-label" for="pt-weekday">每周执行日</label>
                <select id="pt-weekday" v-model="form.weeklyDay" class="form-input">
                  <option v-for="day in 7" :key="day" :value="day">周{{ WEEK_TEXT[day - 1] }}</option>
                </select>
              </section>

              <section v-if="form.cycleType === 'MONTHLY'" class="field">
                <label class="field-label" for="pt-monthday">每月执行日</label>
                <input
                  id="pt-monthday"
                  v-model="form.monthlyDay"
                  type="number"
                  min="1"
                  max="31"
                  class="form-input"
                  placeholder="1～31"
                />
              </section>
            </div>

            <div class="preview">
              <span>规则预览：{{ formPreview.cycle }}</span>
              <span>下次计划：{{ formPreview.next }}</span>
            </div>
          </div>

          <footer class="dialog-footer">
            <button type="button" class="btn cancel" :disabled="submitting" @click="closeForm">取消</button>
            <button type="submit" class="btn primary" :disabled="submitting">
              {{ submitting ? '保存中…' : '保存' }}
            </button>
          </footer>
        </form>
      </div>

      <div v-if="deleteTarget" class="modal-mask confirm-mask" @click.self="cancelDelete">
        <div class="confirm-dialog" role="alertdialog" aria-modal="true" aria-label="删除确认">
          <div class="confirm-title">删除周期任务</div>
          <div class="confirm-text">
            确定删除规则「{{ deleteTarget.title || '未命名任务' }}」吗？仅停止后续生成，已生成任务将保留。
          </div>
          <div class="confirm-actions">
            <button type="button" class="confirm-btn cancel" @click="cancelDelete">取消</button>
            <button type="button" class="confirm-btn danger" @click="confirmDelete">确定删除</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.periodic {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 24px;
  box-sizing: border-box;
  min-height: 100%;
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

.table-wrap {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow-x: auto;
}

.rule-table {
  width: 100%;
  min-width: 860px;
  border-collapse: collapse;
  font-size: 13px;
  color: #374151;
}

.rule-table th,
.rule-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid #f3f4f6;
  vertical-align: top;
}

.rule-table th {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  background: #f9fafb;
}

.rule-table tbody tr:last-child td {
  border-bottom: none;
}

.rule-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.fail-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #dc2626;
}

.ops-col {
  white-space: nowrap;
}

.link-btn {
  padding: 0;
  margin-right: 10px;
  font-size: 13px;
  color: #3b82f6;
  background: transparent;
  border: none;
  cursor: pointer;
}

.link-btn:hover {
  text-decoration: underline;
}

.link-btn.danger {
  margin-right: 0;
  color: #ef4444;
}

.status-pill {
  padding: 2px 9px;
  font-size: 12px;
  color: #065f46;
  background: #d1fae5;
  border-radius: 10px;
  white-space: nowrap;
}

.status-pill.off {
  color: #6b7280;
  background: #f3f4f6;
}

.empty {
  margin: 0;
  padding: 32px 0;
  text-align: center;
  font-size: 13px;
  color: #9ca3af;
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

.dialog {
  width: 560px;
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

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.dialog-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.dialog-close {
  width: 32px;
  height: 32px;
  padding: 0;
  font-size: 22px;
  line-height: 32px;
  color: #9ca3af;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.dialog-close:hover {
  color: #374151;
  background: #f3f4f6;
}

.dialog-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.field-row {
  display: flex;
  gap: 12px;
}

.field-label {
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
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
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.form-input:focus,
.form-textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.radio-group {
  display: flex;
  gap: 16px;
  padding: 8px 0;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
}

.radio-item input {
  accent-color: #3b82f6;
}

.preview {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 10px 12px;
  font-size: 12px;
  color: #4b5563;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
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

.confirm-mask {
  z-index: 3000;
}

.confirm-dialog {
  width: 380px;
  max-width: calc(100vw - 32px);
  padding: 20px;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.2);
  animation: dialogPop 0.18s ease-out;
}

.confirm-title {
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.confirm-text {
  margin-bottom: 16px;
  font-size: 14px;
  line-height: 1.6;
  color: #4b5563;
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
</style>
