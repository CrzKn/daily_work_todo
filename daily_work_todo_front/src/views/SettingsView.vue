<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import TagManage from './TagManage.vue'
import StatusManage from './StatusManage.vue'
import PriorityManage from './PriorityManage.vue'

const sections = [
  {
    id: 'tag',
    name: '标签管理',
    icon: 'M20.59 13.41 13.42 20.58a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82ZM7 7h.01',
  },
  {
    id: 'status',
    name: '任务状态管理',
    icon: 'M22 11.08V12a10 10 0 1 1-5.93-9.14M22 4 12 14.01l-3-3',
  },
  {
    id: 'priority',
    name: '任务优先级管理',
    icon: 'M6 20V10M12 20V4M18 20v-6',
  },
]

const sectionComponents = {
  tag: TagManage,
  status: StatusManage,
  priority: PriorityManage,
}

// 子页面记录在 hash 中（如 #/settings/status），刷新后仍停留在原页面
function sectionIdFromHash() {
  const id = window.location.hash.replace(/^#\/?/, '').split('/')[1]
  return sections.some((s) => s.id === id) ? id : sections[0].id
}

const activeSection = ref(sectionIdFromHash())
const activeComponent = computed(() => sectionComponents[activeSection.value] ?? null)

function selectSection(id) {
  if (activeSection.value !== id) {
    activeSection.value = id
    window.location.hash = `#/settings/${id}`
  }
}

function syncFromHash() {
  activeSection.value = sectionIdFromHash()
}

onMounted(() => {
  window.addEventListener('hashchange', syncFromHash)
  // 首次进入设置页时补齐子页面地址
  if (window.location.hash !== `#/settings/${activeSection.value}`) {
    window.history.replaceState(null, '', `#/settings/${activeSection.value}`)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('hashchange', syncFromHash)
})
</script>

<template>
  <div class="settings">
    <aside class="settings-nav">
      <div class="settings-nav-title">设置</div>
      <div
        v-for="section in sections"
        :key="section.id"
        class="settings-nav-item"
        :class="{ active: activeSection === section.id }"
        @click="selectSection(section.id)"
      >
        <svg
          class="settings-nav-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path :d="section.icon" />
        </svg>
        <span>{{ section.name }}</span>
      </div>
    </aside>

    <main class="settings-content">
      <component :is="activeComponent" v-if="activeComponent" />
    </main>
  </div>
</template>

<style scoped>
.settings {
  display: flex;
  height: 100%;
  background: #f3f4f6;
}

.settings-nav {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 8px;
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
  box-sizing: border-box;
}

.settings-nav-title {
  padding: 4px 12px 12px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.settings-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.settings-nav-item:hover {
  background: #f9fafb;
}

.settings-nav-item.active {
  background: #f3f4f6;
  color: #111827;
  font-weight: 600;
}

.settings-nav-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: #6b7280;
}

.settings-nav-item.active .settings-nav-icon {
  color: #111827;
}

.settings-content {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
</style>
