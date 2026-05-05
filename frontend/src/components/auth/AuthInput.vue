<template>
  <div class="input-group">
    <label v-if="label" class="label">{{ label }}</label>
    <input 
      :type="type"
      :value="modelValue"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      class="custom-input"
      v-bind="$attrs"
    />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label?: string
  type?: string
  modelValue?: string
}>()
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<style scoped>
.input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #4A5568;
}

.custom-input {
  /* Visual escuro/moderno solicitado */
  background-color: #2D3748; 
  color: #EDF2F7;
  border: 1px solid #1A202C;
  padding: 0.875rem 1rem;
  border-radius: 8px;
  font-size: 1rem;
  outline: none;
  transition: all 0.2s;
}

.custom-input:focus {
  border-color: #2E7D32;
  box-shadow: 0 0 0 3px rgba(46, 125, 50, 0.15);
}

.custom-input::placeholder {
  color: #718096;
}
</style>