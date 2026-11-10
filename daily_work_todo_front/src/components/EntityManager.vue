<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  idKey: { type: String, required: true },
  fields: { type: Array, required: true },
  api: { type: Object, required: true },
})

const rows = ref([])
const form = ref(buildEmptyForm())
const editingId = ref('')
const editDraft = ref(null)
const loading = ref(false)
const message = ref({ type: '', text: '' })

function buildEmptyForm() {
  const result = {}
  props.fields.forEach((field) => {
    result[field.key] = field.type === 'number' ? null : ''
  })
  return result
}

function showMessage(type, text) {
  message.value = { type, text }
}

function validate(data) {
  const missing = props.fields.find(
    (field) => field.required && (data[field.key] === '' || data[field.key] === null),
  )
  if (missing) {
    showMessage('error', `请填写${missing.label}`)
    return false
  }
  return true
}

function handleInput(target, field, event) {
  const raw = event.target.value
  target[field.key] = field.type === 'number' ? (raw === '' ? null : Number(raw)) : raw
}

async function loadList() {
  loading.value = true
  try {
    rows.value = await props.api.list()
  } catch (e) {
    showMessage('error', `加载列表失败：${e.message}`)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.value = buildEmptyForm()
  showMessage('', '')
}

async function handleCreate() {
  if (!validate(form.value)) return
  try {
    const ok = await props.api.save({ ...form.value })
    if (!ok) {
      showMessage('error', '新增失败')
      return
    }
    showMessage('success', '新增成功')
    resetForm()
    await loadList()
  } catch (e) {
    showMessage('error', `新增失败：${e.message}`)
  }
}

function startEdit(row) {
  editingId.value = row[props.idKey]
  const draft = buildEmptyForm()
  props.fields.forEach((field) => {
    draft[field.key] = row[field.key] ?? draft[field.key]
  })
  editDraft.value = draft
  showMessage('', '')
}

function cancelEdit() {
  editingId.value = ''
  editDraft.value = null
  showMessage('', '')
}

async function saveEdit() {
  if (!editDraft.value || !validate(editDraft.value)) return
  try {
    const ok = await props.api.update({ [props.idKey]: editingId.value, ...editDraft.value })
    if (!ok) {
      showMessage('error', '修改失败')
      return
    }
    showMessage('success', '修改成功')
    cancelEdit()
    await loadList()
  } catch (e) {
    showMessage('error', `修改失败：${e.message}`)
  }
}

async function handleDelete(row) {
  if (!window.confirm('确定删除这条记录吗？')) return
  try {
    const ok = await props.api.remove(row[props.idKey])
    if (!ok) {
      showMessage('error', '删除失败')
      return
    }
    showMessage('success', '删除成功')
    if (editingId.value === row[props.idKey]) cancelEdit()
    await loadList()
  } catch (e) {
    showMessage('error', `删除失败：${e.message}`)
  }
}

onMounted(loadList)
</script>

<template>
  <div class="page">
    <h2 class="page-title">{{ title }}</h2>

    <section class="panel">
      <div class="panel-title">新增</div>
      <form class="form" @submit.prevent>
        <div v-for="field in fields" :key="field.key" class="field">
          <label class="field-label">
            {{ field.label }}
            <span v-if="field.required" class="required">*</span>
          </label>
          <input
            class="field-input"
            :type="field.type === 'number' ? 'number' : 'text'"
            :placeholder="`请输入${field.label}`"
            :value="form[field.key] ?? ''"
            @input="handleInput(form, field, $event)"
          />
        </div>

        <div class="actions">
          <button type="button" class="btn primary" @click="handleCreate">新增</button>
          <button type="button" class="btn ghost" @click="resetForm">重置</button>
        </div>
      </form>
    </section>

    <section class="panel">
      <div class="panel-title">
        数据列表
        <span v-if="loading" class="loading">加载中...</span>
        <span v-if="message.text" class="message" :class="message.type">{{ message.text }}</span>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th v-for="field in fields" :key="field.key">{{ field.label }}</th>
              <th class="op-col">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row[idKey]">
              <td class="id-cell">{{ row[idKey] }}</td>
              <template v-if="editingId === row[idKey]">
                <td v-for="field in fields" :key="field.key">
                  <input
                    class="row-input"
                    :type="field.type === 'number' ? 'number' : 'text'"
                    :value="editDraft[field.key] ?? ''"
                    @input="handleInput(editDraft, field, $event)"
                  />
                </td>
                <td class="op-cell">
                  <button type="button" class="link primary" @click="saveEdit">保存</button>
                  <button type="button" class="link" @click="cancelEdit">取消</button>
                </td>
              </template>
              <template v-else>
                <td v-for="field in fields" :key="field.key">{{ row[field.key] ?? '' }}</td>
                <td class="op-cell">
                  <button type="button" class="link primary" @click="startEdit(row)">修改</button>
                  <button type="button" class="link danger" @click="handleDelete(row)">删除</button>
                </td>
              </template>
            </tr>
            <tr v-if="!rows.length && !loading">
              <td class="empty" :colspan="fields.length + 2">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
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

.page-title {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.panel {
  background: #ffffff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
}

.loading {
  font-weight: 400;
  color: #6b7280;
}

.message {
  font-weight: 400;
  font-size: 13px;
}

.message.success {
  color: #16a34a;
}

.message.error {
  color: #dc2626;
}

.form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
}

.field-label {
  font-size: 13px;
  color: #4b5563;
}

.required {
  color: #ef4444;
}

.field-input,
.row-input {
  height: 34px;
  padding: 0 10px;
  font-size: 14px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.field-input:focus,
.row-input:focus {
  border-color: #3b82f6;
}

.row-input {
  width: 100%;
  min-width: 120px;
  height: 30px;
}

.actions {
  display: flex;
  gap: 8px;
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
}

.table th {
  background: #f9fafb;
  color: #4b5563;
  font-weight: 600;
  white-space: nowrap;
}

.table tbody tr:hover {
  background: #f9fafb;
}

.id-cell {
  color: #6b7280;
  font-family: Consolas, monospace;
  font-size: 12px;
}

.op-col {
  width: 120px;
}

.op-cell {
  white-space: nowrap;
}

.link {
  padding: 0;
  margin-right: 12px;
  font-size: 13px;
  border: none;
  background: none;
  color: #6b7280;
  cursor: pointer;
}

.link:hover {
  text-decoration: underline;
}

.link.primary {
  color: #3b82f6;
}

.link.danger {
  color: #ef4444;
}

.empty {
  text-align: center;
  color: #9ca3af;
  padding: 24px 0;
}
</style>
