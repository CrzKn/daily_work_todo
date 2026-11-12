<script setup>
import EntityManager from '../components/EntityManager.vue'
import { createCrudApi } from '../api/crud'

const api = createCrudApi('/prioritys')

const fields = [
  {
    key: 'priorityName',
    label: '优先级名称',
    type: 'text',
    required: true,
    dotColorKey: 'priorityOrder',
  },
  {
    key: 'priorityOrder',
    label: '排序',
    type: 'number',
    required: true,
    tooltip: '数值越小越靠前，用于状态展示顺序。\n例：紧急 1 < 高 2 <  中 3 < 低 4',
  },
]
</script>

<template>
  <EntityManager
    title="任务优先级管理"
    id-key="priorityId"
    :fields="fields"
    :api="api"
    :show-create="false"
    :use-modal-for-validation="true"
  />
</template>
