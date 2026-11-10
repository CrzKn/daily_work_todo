<script setup>
import { ref, computed } from 'vue'
import TagManage from './TagManage.vue'
import StatusManage from './StatusManage.vue'
import PriorityManage from './PriorityManage.vue'

const modules = [
  { id: 'board', name: '任务看板' },
  { id: 'tag', name: '标签管理' },
  { id: 'status', name: '任务状态管理' },
  { id: 'priority', name: '任务优先级管理' },
]

const moduleComponents = {
  tag: TagManage,
  status: StatusManage,
  priority: PriorityManage,
}

const activeModule = ref(modules[0].id)
const activeComponent = computed(() => moduleComponents[activeModule.value] ?? null)
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
          @click="activeModule = mod.id"
        >
          {{ mod.name }}
        </div>
      </nav>
    </aside>

    <main class="content">
      <component :is="activeComponent" v-if="activeComponent" />
      <!-- 任务看板内容暂为空 -->
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

.content {
  flex: 1;
  min-width: 0;
  background: #f3f4f6;
  overflow-y: auto;
}
</style>
