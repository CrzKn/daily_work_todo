<script setup>
import EntityManager from '../components/EntityManager.vue'
import { createCrudApi } from '../api/crud'

const api = createCrudApi('/ptStatus')

const fields = [
  { key: 'ptStatusName', label: '任务状态名称', type: 'text', required: true },
  { key: 'ptStatusOrder', label: '排序', type: 'number', required: true },
]
</script>

<template>
  <EntityManager title="任务状态管理" id-key="ptStatusId" :fields="fields" :api="api" />
</template>
