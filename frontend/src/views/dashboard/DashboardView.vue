<template>
  <div class="dashboard-placeholder">
    <h1>Dashboard</h1>
    <p>Em construção.</p>
    <button @click="handleLogout">Sair</button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores'

const router = useRouter()
const authStore = useAuthStore()

async function handleLogout() {
  await authStore.logout()
  router.push({ name: 'auth' })
}
</script>
