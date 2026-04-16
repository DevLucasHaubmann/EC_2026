<template>
  <button class="auth-btn" v-bind="$attrs">
    <slot />
  </button>
</template>

<style scoped>
.auth-btn {
  background-color: #2E7D32;
  color: white;
  border: none;
  padding: 0.875rem;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.1s;
  margin-top: 0.5rem;
}

.auth-btn:hover {
  background-color: #256320;
}

.auth-btn:active {
  transform: scale(0.98);
}

.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
</style>