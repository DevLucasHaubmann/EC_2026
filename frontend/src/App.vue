<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppSidebar from './components/AppSidebar.vue'

const route = useRoute()
const showSidebar = computed(() => !!route.meta.requiresAuth && !route.meta.noSidebar)
</script>

<template>
  <div class="app-root" :class="{ 'has-sidebar': showSidebar }">
    <AppSidebar v-if="showSidebar" />
    <div class="app-content">
      <RouterView />
    </div>
  </div>
</template>

<style>
*, *::before, *::after { box-sizing: border-box; }
html, body { margin: 0; padding: 0; background: #0f172a; }

.app-root {
  display: flex;
  min-height: 100vh;
}

.app-content {
  flex: 1;
  min-width: 0;
}

@media (max-width: 768px) {
  .app-root.has-sidebar {
    flex-direction: column;
  }
}
</style>
