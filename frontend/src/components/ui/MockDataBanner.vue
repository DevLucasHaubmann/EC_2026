<template>
  <div class="mock-banner" role="alert" aria-live="polite">
    <span class="mock-banner__icon">⚠</span>
    <span class="mock-banner__text">
      <strong>Tela em desenvolvimento.</strong>
      Os dados exibidos são demonstrativos e não refletem informações reais.
    </span>
  </div>
</template>

<style scoped>
.mock-banner {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.35);
  border-radius: 12px;
  padding: 0.9rem 1.25rem;
  margin-bottom: 2rem;
  color: #fbbf24;
  font-size: 0.88rem;
  line-height: 1.5;
}

.mock-banner__icon {
  flex-shrink: 0;
  font-size: 1rem;
  margin-top: 1px;
}

.mock-banner__text strong {
  font-weight: 700;
  margin-right: 0.25rem;
}
</style>
