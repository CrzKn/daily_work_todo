<script setup>
/**
 * 通用警告/确认弹窗
 * 用法示例：
 *   <WarningConfirm v-model:open="titleWarnOpen"
 *     title="任务标题不能为空"
 *     message="请先填写任务标题再保存。"
 *     confirm-text="我知道了" />
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '提示' },
  message: { type: String, default: '' },
  confirmText: { type: String, default: '我知道了' },
})
const emit = defineEmits(['update:open', 'confirm'])

function close() {
  emit('update:open', false)
}

function onConfirm() {
  emit('update:open', false)
  emit('confirm')
}

function onKeydown(e) {
  if (e.key === 'Escape' && props.open) close()
}

import { onMounted, onBeforeUnmount } from 'vue'
onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="modal-mask warning-mask" @click.self="close">
      <div class="warning-dialog" role="alertdialog" aria-modal="true" :aria-label="title">
        <div class="warning-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
            <line x1="12" y1="9" x2="12" y2="13"></line>
            <line x1="12" y1="17" x2="12.01" y2="17"></line>
          </svg>
        </div>
        <div class="warning-content">
          <div class="warning-title">{{ title }}</div>
          <div v-if="message" class="warning-text">{{ message }}</div>
        </div>
        <div class="warning-actions">
          <button type="button" class="confirm-btn primary" @click="onConfirm">{{ confirmText }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 4000;
}

.warning-dialog {
  width: 360px;
  max-width: calc(100vw - 32px);
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.2);
  padding: 22px 20px 18px;
  display: flex;
  gap: 14px;
  align-items: flex-start;
  animation: dialogPop 0.18s ease-out;
}

.warning-icon {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #fef3c7;
  color: #d97706;
  display: flex;
  align-items: center;
  justify-content: center;
}

.warning-content {
  flex: 1;
  min-width: 0;
}

.warning-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 6px;
}

.warning-text {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}

.warning-actions {
  flex-shrink: 0;
  align-self: center;
}

.confirm-btn.primary {
  padding: 7px 18px;
  font-size: 13px;
  border-radius: 6px;
  border: none;
  background: #3b82f6;
  color: #ffffff;
  cursor: pointer;
  transition: background 0.15s;
}

.confirm-btn.primary:hover {
  background: #2563eb;
}

@keyframes dialogPop {
  0% { transform: scale(0.92); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
</style>
