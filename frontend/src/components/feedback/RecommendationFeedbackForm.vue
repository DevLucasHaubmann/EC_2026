<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { aiService } from '@/services/modules/aiRecommendation'
import { useToast } from '@/composables/useToast'
import type { Recommendation } from '@/types/api'

const props = defineProps<{
  recommendationId: number
  recommendationStatus: 'GENERATED' | 'VIEWED' | 'ARCHIVED'
}>()

const emit = defineEmits<{
  feedbackSubmitted: []
}>()

const toast = useToast()

const LIKED_TAGS: Array<{ tag: Recommendation.FeedbackTag; label: string }> = [
  { tag: 'PRACTICAL', label: 'Prático' },
  { tag: 'VARIED', label: 'Variado' },
  { tag: 'AFFORDABLE', label: 'Acessível' },
  { tag: 'BALANCED', label: 'Equilibrado' },
  { tag: 'TASTY', label: 'Saboroso' },
  { tag: 'EASY_TO_PREPARE', label: 'Fácil de preparar' },
]

const DISLIKED_TAGS: Array<{ tag: Recommendation.FeedbackTag; label: string }> = [
  { tag: 'TOO_RESTRICTIVE', label: 'Muito restritivo' },
  { tag: 'REPETITIVE', label: 'Repetitivo' },
  { tag: 'EXPENSIVE', label: 'Caro' },
  { tag: 'LACKING_PROTEIN', label: 'Pouca proteína' },
  { tag: 'OTHER', label: 'Outro' },
]

const rating = ref(0)
const likedTags = ref<Recommendation.FeedbackTag[]>([])
const dislikedTags = ref<Recommendation.FeedbackTag[]>([])
const comment = ref('')
const existingFeedback = ref<Recommendation.FeedbackResponse | null>(null)
const loadingFeedback = ref(true)
const submitting = ref(false)

const isArchived = computed(() => props.recommendationStatus === 'ARCHIVED')
const hasExistingFeedback = computed(() => existingFeedback.value !== null)
const canSubmit = computed(() => rating.value > 0 && !submitting.value && !isArchived.value)

function populateForm(feedback: Recommendation.FeedbackResponse): void {
  rating.value = feedback.rating
  likedTags.value = [...feedback.likedTags]
  dislikedTags.value = [...feedback.dislikedTags]
  comment.value = feedback.comment ?? ''
}

function toggleTag(list: Recommendation.FeedbackTag[], tag: Recommendation.FeedbackTag): void {
  const idx = list.indexOf(tag)
  if (idx === -1) list.push(tag)
  else list.splice(idx, 1)
}

function isTagActive(list: Recommendation.FeedbackTag[], tag: Recommendation.FeedbackTag): boolean {
  return list.includes(tag)
}

async function submitFeedback(): Promise<void> {
  if (!canSubmit.value) return
  const wasUpdate = hasExistingFeedback.value
  submitting.value = true
  try {
    const result = await aiService.addFeedback(props.recommendationId, {
      rating: rating.value,
      likedTags: likedTags.value,
      dislikedTags: dislikedTags.value,
      comment: comment.value || undefined,
    })
    existingFeedback.value = result
    toast.success(wasUpdate ? 'Feedback atualizado com sucesso!' : 'Feedback enviado com sucesso!')
    emit('feedbackSubmitted')
  } catch {
    toast.error('Não foi possível salvar o feedback. Tente novamente.')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const feedback = await aiService.getFeedback(props.recommendationId)
    existingFeedback.value = feedback
    if (feedback) populateForm(feedback)
  } catch {
    // silently ignore — form starts empty
  } finally {
    loadingFeedback.value = false
  }
})
</script>

<template>
  <div class="feedback-card">
    <h3 class="feedback-title">Avaliar esta dieta</h3>

    <div v-if="loadingFeedback" class="feedback-loading">
      <div class="fb-spinner"></div>
    </div>

    <template v-else>
      <!-- Archived notice -->
      <p v-if="isArchived && !hasExistingFeedback" class="feedback-archived-note">
        Esta dieta está arquivada e não possui feedback registrado.
      </p>

      <!-- Rating -->
      <div class="fb-field">
        <span class="fb-label">Avaliação</span>
        <div class="star-row">
          <button
            v-for="n in 5"
            :key="n"
            type="button"
            class="star-btn"
            :class="{ filled: n <= rating, disabled: isArchived }"
            :disabled="isArchived"
            @click="rating = n"
          >
            {{ n <= rating ? '★' : '☆' }}
          </button>
        </div>
      </div>

      <!-- Liked tags -->
      <div v-if="!isArchived || likedTags.length > 0" class="fb-field">
        <span class="fb-label">Gostei de:</span>
        <div class="tag-row">
          <button
            v-for="{ tag, label } in LIKED_TAGS"
            :key="tag"
            type="button"
            class="tag-chip"
            :class="{ active: isTagActive(likedTags, tag), disabled: isArchived }"
            :disabled="isArchived"
            @click="toggleTag(likedTags, tag)"
          >
            {{ label }}
          </button>
        </div>
      </div>

      <!-- Disliked tags -->
      <div v-if="!isArchived || dislikedTags.length > 0" class="fb-field">
        <span class="fb-label">Não gostei de:</span>
        <div class="tag-row">
          <button
            v-for="{ tag, label } in DISLIKED_TAGS"
            :key="tag"
            type="button"
            class="tag-chip tag-chip--negative"
            :class="{ active: isTagActive(dislikedTags, tag), disabled: isArchived }"
            :disabled="isArchived"
            @click="toggleTag(dislikedTags, tag)"
          >
            {{ label }}
          </button>
        </div>
      </div>

      <!-- Comment -->
      <div v-if="!isArchived" class="fb-field">
        <span class="fb-label">Comentário</span>
        <textarea
          v-model="comment"
          class="fb-textarea"
          placeholder="Deixe um comentário (opcional)..."
          maxlength="1000"
          rows="3"
          :disabled="isArchived"
        />
        <span class="char-count">{{ comment.length }} / 1000</span>
      </div>
      <div v-else-if="existingFeedback?.comment" class="fb-field">
        <span class="fb-label">Comentário</span>
        <p class="fb-comment-readonly">{{ existingFeedback.comment }}</p>
      </div>

      <!-- Archived badge -->
      <p v-if="isArchived && hasExistingFeedback" class="feedback-archived-note">
        Esta dieta está arquivada.
      </p>

      <!-- Submit -->
      <button
        v-if="!isArchived"
        type="button"
        class="fb-submit-btn"
        :disabled="!canSubmit"
        @click="submitFeedback"
      >
        <span v-if="submitting" class="fb-spinner fb-spinner--small"></span>
        <span v-else>{{ hasExistingFeedback ? 'Atualizar feedback' : 'Enviar feedback' }}</span>
      </button>
    </template>
  </div>
</template>

<style scoped>
.feedback-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.feedback-title {
  font-size: 0.8rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: #94a3b8;
  margin: 0;
}

.fb-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.fb-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Stars */
.star-row {
  display: flex;
  gap: 0.25rem;
}

.star-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.4rem;
  color: #64748b;
  padding: 0 2px;
  line-height: 1;
  transition: color 0.1s, transform 0.1s;
}

.star-btn.filled {
  color: #10b981;
}

.star-btn:hover:not(.disabled) {
  color: #10b981;
  transform: scale(1.15);
}

.star-btn.disabled {
  cursor: default;
  opacity: 0.6;
}

/* Tags */
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.tag-chip {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #94a3b8;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  letter-spacing: 0.2px;
}

.tag-chip:hover:not(.disabled) {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  border-color: rgba(16, 185, 129, 0.3);
}

.tag-chip.active {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
  border-color: rgba(16, 185, 129, 0.5);
}

.tag-chip--negative:hover:not(.disabled) {
  background: rgba(248, 113, 113, 0.08);
  color: #fca5a5;
  border-color: rgba(248, 113, 113, 0.3);
}

.tag-chip--negative.active {
  background: rgba(248, 113, 113, 0.1);
  color: #fca5a5;
  border-color: rgba(248, 113, 113, 0.4);
}

.tag-chip.disabled {
  cursor: default;
  opacity: 0.5;
}

/* Textarea */
.fb-textarea {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  color: #e2e8f0;
  font-size: 0.85rem;
  padding: 0.6rem 0.75rem;
  resize: vertical;
  font-family: inherit;
  line-height: 1.5;
  transition: border-color 0.15s;
}

.fb-textarea:focus {
  outline: none;
  border-color: rgba(16, 185, 129, 0.4);
}

.char-count {
  font-size: 0.7rem;
  color: #475569;
  text-align: right;
}

/* Comment readonly */
.fb-comment-readonly {
  font-size: 0.85rem;
  color: #94a3b8;
  line-height: 1.5;
  margin: 0;
}

/* Submit button */
.fb-submit-btn {
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.4);
  color: #10b981;
  font-size: 0.82rem;
  font-weight: 700;
  padding: 0.5rem 1.25rem;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  align-self: flex-start;
  min-width: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fb-submit-btn:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.2);
  border-color: rgba(16, 185, 129, 0.7);
}

.fb-submit-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Loading */
.feedback-loading {
  display: flex;
  justify-content: center;
  padding: 1rem;
}

.fb-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(16, 185, 129, 0.15);
  border-top-color: #10b981;
  border-radius: 50%;
  animation: fb-spin 0.8s linear infinite;
}

.fb-spinner--small {
  width: 16px;
  height: 16px;
  border-width: 2px;
}

@keyframes fb-spin {
  to { transform: rotate(360deg); }
}

/* Archived note */
.feedback-archived-note {
  font-size: 0.78rem;
  color: #64748b;
  margin: 0;
  font-style: italic;
}
</style>
