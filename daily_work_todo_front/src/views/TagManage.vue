<script setup>
import EntityManager from '../components/EntityManager.vue'
import { createCrudApi } from '../api/crud'

const api = createCrudApi('/tags')

const fields = [{ key: 'tagName', label: '标签名称', type: 'text', required: true }]
</script>

<template>
  <EntityManager
    title="标签管理"
    id-key="tagId"
    :fields="fields"
    :api="api"
    :use-modal-for-validation="true"
  />
</template>
