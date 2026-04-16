<template>
  <header class="topbar">
    <div class="logo-area">
      <div class="logo-mark">T</div>
      <span class="logo-text">Tukan</span>
    </div>
    <nav class="user-area">
      <button class="icon-button">🔔</button>
      <div class="avatar">L</div>
    </nav>
  </header>
</template>

<style scoped>
.topbar {
  background-color: var(--color-surface);
  height: 64px;
  padding: 0 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 10;
}
.logo-area { display: flex; align-items: center; gap: 0.75rem; }
.logo-mark {
  background-color: var(--color-primary);
  color: white;
  width: 32px; height: 32px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-weight: bold;
}
.logo-text { font-size: 1.25rem; font-weight: 700; color: var(--color-primary); }
.user-area { display: flex; align-items: center; gap: 1rem; }
.icon-button { background: none; border: none; font-size: 1.25rem; cursor: pointer; }
.avatar {
  width: 36px; height: 36px;
  border-radius: 50%;
  background-color: var(--color-primary-light);
  color: var(--color-primary);
  display: flex; align-items: center; justify-content: center;
  font-weight: 600;
}
</style>