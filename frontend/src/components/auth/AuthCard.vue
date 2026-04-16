<template>
  <div class="card">
    <slot />
  </div>
</template>

<style scoped>
.card {
  background-color: #FFFFFF;
  width: 100%;
  max-width: 400px;
  padding: 2.5rem;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(46, 125, 50, 0.08);
  border: 1px solid #E2E8F0;
}
</style>