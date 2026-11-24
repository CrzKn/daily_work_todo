<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import TaskBoard from './TaskBoard.vue'
import PeriodicTaskManage from './PeriodicTaskManage.vue'
import HistoryTaskView from './HistoryTaskView.vue'
import SettingsView from './SettingsView.vue'

const modules = [
  { id: 'board', name: '任务看板' },
  { id: 'periodic', name: '周期任务' },
  { id: 'history', name: '已归档任务' },
]

const moduleComponents = {
  board: TaskBoard,
  periodic: PeriodicTaskManage,
  history: HistoryTaskView,
  settings: SettingsView,
}

// 用 hash 记录当前模块，刷新后仍停留在原页面（如 #/history、#/settings/tag）
function moduleIdFromHash() {
  const id = window.location.hash.replace(/^#\/?/, '').split('/')[0]
  return modules.some((mod) => mod.id === id) || id === 'settings' ? id : modules[0].id
}

const activeModule = ref(moduleIdFromHash())
const activeComponent = computed(() => moduleComponents[activeModule.value] ?? null)

function selectModule(id) {
  if (activeModule.value !== id) {
    activeModule.value = id
    window.location.hash = `#/${id}`
  }
}

function syncFromHash() {
  activeModule.value = moduleIdFromHash()
}

onMounted(() => {
  window.addEventListener('hashchange', syncFromHash)
  // 首次进入时补齐 hash，保证地址栏与当前页面一致
  if (!window.location.hash) {
    window.history.replaceState(null, '', `#/${activeModule.value}`)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('hashchange', syncFromHash)
})
</script>

<template>
  <div class="home">
    <aside class="sidebar">
      <div class="sidebar-title">Daily Work Todo</div>
      <nav class="module-list">
        <div
          v-for="mod in modules"
          :key="mod.id"
          class="module-item"
          :class="{ active: activeModule === mod.id }"
          @click="selectModule(mod.id)"
        >
          {{ mod.name }}
        </div>
      </nav>

      <div class="sidebar-footer">
        <button
          type="button"
          class="settings-btn"
          :class="{ active: activeModule === 'settings' }"
          @click="selectModule('settings')"
        >
          <svg
            class="settings-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path
              d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"
            />
            <circle cx="12" cy="12" r="3" />
          </svg>
          <span>设置</span>
        </button>
      </div>
    </aside>

    <main class="content">
      <component :is="activeComponent" v-if="activeComponent" />
    </main>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #1f2937;
  color: #e5e7eb;
}

.sidebar-title {
  padding: 20px 16px;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #374151;
}

.module-list {
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.module-item {
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.module-item:hover {
  background: #374151;
}

.module-item.active {
  background: #3b82f6;
  color: #ffffff;
}

.sidebar-footer {
  margin-top: auto;
  padding: 8px;
  border-top: 1px solid #374151;
}

.settings-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  font-size: 14px;
  font-family: inherit;
  color: #e5e7eb;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  text-align: left;
  transition: background 0.2s;
}

.settings-btn:hover {
  background: #374151;
}

.settings-btn.active {
  background: #3b82f6;
  color: #ffffff;
}

.settings-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.content {
  flex: 1;
  min-width: 0;
  background: #f3f4f6;
  overflow-y: auto;
}
</style>
