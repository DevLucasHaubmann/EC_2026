import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { meService } from '@/services/modules/me'
import type { MeResponse } from '@/types/api'

// Mirrors backend AssessmentCompletenessPolicy: an assessment counts as complete only
// when the three fields required for meal-plan generation are all present. A stale
// assessment with a null goal/dietType/mealsPerDay does NOT complete onboarding.
function isAssessmentComplete(assessment: MeResponse['assessment']): boolean {
  return (
    assessment !== null &&
    assessment.goal != null &&
    assessment.dietType != null &&
    assessment.mealsPerDay != null
  )
}

export const useMeStore = defineStore('me', () => {
  const me = ref<MeResponse | null>(null)
  const loading = ref(false)

  const currentUserId = computed<number | null>(() => me.value?.id ?? null)

  // Onboarding is complete when the user has a profile AND a complete assessment
  // (same definition the backend uses to decide whether triagem is still required).
  const onboardingComplete = computed<boolean>(
    () => !!me.value && me.value.profile !== null && isAssessmentComplete(me.value.assessment),
  )

  // Idempotent: fetches only once unless force=true. Prevents duplicate /me calls
  // between components that share this store (e.g. sidebar + admin dashboard).
  async function load(force = false): Promise<void> {
    if (loading.value) return
    if (me.value && !force) return
    loading.value = true
    try {
      me.value = await meService.getMe()
    } finally {
      loading.value = false
    }
  }

  function clear(): void {
    me.value = null
  }

  // Updates avatarUrl in local state after a successful PATCH /me/avatar,
  // avoiding a full re-fetch of the /me endpoint.
  function setAvatar(avatarUrl: string | null): void {
    if (me.value) {
      me.value = { ...me.value, avatarUrl }
    }
  }

  return { me, loading, currentUserId, onboardingComplete, load, clear, setAvatar }
})
