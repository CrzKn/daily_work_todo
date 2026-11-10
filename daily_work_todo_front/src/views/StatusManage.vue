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
