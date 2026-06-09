<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { isSafeAvatarUrl, getInitials } from '@/utils/avatarUrl'

const props = withDefaults(
  defineProps<{
    name?: string
    email?: string
    avatarUrl?: string | null
    size?: number
  }>(),
  { size: 40 },
)

const hasError = ref(false)

const safeUrl = computed<string | null>(() => {
  if (!props.avatarUrl || !isSafeAvatarUrl(props.avatarUrl)) return null
  return props.avatarUrl
})

const showImage = computed(() => safeUrl.value !== null && !hasError.value)

const initials = computed(() => getInitials(props.name, props.email))

const sizeStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  fontSize: `${Math.round(props.size * 0.38)}px`,
}))

function onImageError() {
  hasError.value = true
}

// Reset the error state when the source changes, so a previously broken image
// does not keep an instance stuck on initials when a new valid URL arrives
// (e.g. sidebar avatar reacting to a profile update via the me store).
watch(safeUrl, () => {
  hasError.value = false
})
</script>

<template>
  <img
    v-if="showImage"
    :src="safeUrl!"
    :alt="name ?? email ?? 'Avatar'"
    class="user-avatar user-avatar--img"
    :style="sizeStyle"
    @error="onImageError"
  />
  <span
    v-else
    class="user-avatar user-avatar--initials"
    :style="sizeStyle"
    role="img"
    :aria-label="name ?? email ?? 'Avatar'"
  >
    {{ initials }}
  </span>
</template>

<style scoped>
.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
}

.user-avatar--img {
  object-fit: cover;
}

.user-avatar--initials {
  background: rgba(16, 185, 129, 0.15);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #10b981;
  font-weight: 700;
  font-family: 'Inter', sans-serif;
  letter-spacing: 0.5px;
  user-select: none;
}
</style>
