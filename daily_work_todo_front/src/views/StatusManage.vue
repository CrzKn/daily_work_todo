<script setup>
import EntityManager from '../components/EntityManager.vue'
import { createCrudApi } from '../api/crud'

const api = createCrudApi('/ptStatus')

const fields = [
  { key: 'ptStatusName', label: '任务状态名称', type: 'text', required: true },
  {
    key: 'ptStatusOrder',
    label: '排序',
    type: 'number',
    required: true,
    tooltip: '数值越小越靠前，用于状态展示顺序。\n例：待办 1 < 进行中 2 < 已完成 3 < 已归档 4',
  },
  {
    key: 'isHistory',
    label: '计入历史回看',
    type: 'switch',
    tooltip: '开启后该状态下的任务进入历史任务回看列表。\n系统预置的「已完成」「已归档」默认开启；变更后立即影响历史列表范围，无需迁移任务数据。',
  },
]
</script>

<template>
  <EntityManager
    title="任务状态管理"
    id-key="ptStatusId"
    :fields="fields"
    :api="api"
    :show-create="false"
    :use-modal-for-validation="true"
  />
</template>
