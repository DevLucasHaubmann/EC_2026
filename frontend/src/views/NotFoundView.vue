<template>
  <div class="not-found">
    <h1>404</h1>
    <p>Página não encontrada.</p>
    <router-link :to="{ name: 'home' }">Voltar ao início</router-link>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #0f172a;
  color: #f8fafc;
  font-family: 'Inter', sans-serif;
  gap: 1rem;
}
.not-found h1 { font-size: 5rem; font-weight: 800; color: #10b981; margin: 0; }
.not-found p { color: #94a3b8; font-size: 1.1rem; }
.not-found a { color: #10b981; text-decoration: none; font-weight: 600; }
.not-found a:hover { text-decoration: underline; }
</style>
