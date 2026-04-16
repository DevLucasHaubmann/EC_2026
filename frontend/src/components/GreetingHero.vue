<template>
  <section class="hero">
    <h1 class="greeting">Olá, {{ userName }}! 👋</h1>
    <p class="subtitle">Aqui está o resumo da sua saúde para hoje. Vamos manter o foco!</p>
  </section>
</template>

<script setup>
defineProps({ userName: String })
</script>

<style scoped>
.hero { display: flex; flex-direction: column; gap: 0.5rem; }
.greeting { font-size: 1.75rem; font-weight: 700; margin: 0; }
.subtitle { font-size: 1rem; color: var(--color-text-muted); margin: 0; }
</style>