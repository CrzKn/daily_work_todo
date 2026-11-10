<script setup>
import EntityManager from '../components/EntityManager.vue'
import { createCrudApi } from '../api/crud'

const api = createCrudApi('/prioritys')

const fields = [
  { key: 'priorityName', label: '优先级名称', type: 'text', required: true },
  { key: 'priorityOrder', label: '排序', type: 'number', required: true },
]
</script>

<template>
  <EntityManager title="任务优先级管理" id-key="priorityId" :fields="fields" :api="api" />
</template>
