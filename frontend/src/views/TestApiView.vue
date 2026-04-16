<script setup lang="ts">
import { ref } from 'vue'
import { api } from '../services/http/api'

const loading = ref(false)
const result = ref('')
const error = ref('')

async function testEndpoint() {
  loading.value = true
  result.value = ''
  error.value = ''

  try {
    const response = await api.get('/users')
    result.value = JSON.stringify(response.data, null, 2)
  } catch (err: any) {
    error.value =
      err?.response?.data
        ? JSON.stringify(err.response.data, null, 2)
        : err.message || 'Erro ao chamar endpoint.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="padding: 24px">
    <h1>Teste de API</h1>

    <button @click="testEndpoint" :disabled="loading">
      {{ loading ? 'Testando...' : 'Testar endpoint' }}
    </button>

    <h2>Resposta</h2>
    <pre v-if="result">{{ result }}</pre>

    <h2 v-if="error">Erro</h2>
    <pre v-if="error">{{ error }}</pre>
  </div>
</template>