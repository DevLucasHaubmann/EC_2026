<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { profileService } from '@/services/modules/profile';
import { assessmentService } from '@/services/modules/assessment';
import { meService } from '@/services/modules/me';
import { useMeStore } from '@/stores/me';
import {
  HEALTH_CONDITION_OPTIONS,
  ALLERGY_OPTIONS,
  NUTRITIONAL_GOAL_OPTIONS,
  ACTIVITY_LEVEL_OPTIONS,
  DIET_TYPE_OPTIONS,
} from '@/constants/assessmentOptions';
import { COMMON_MESSAGES, PROFILE_MESSAGES, VALIDATION_MESSAGES } from '@/constants/messages';
import { useToast } from '@/composables/useToast';
import { splitField } from '@/utils/formatters';
import { toggleArrayItem } from '@/utils/array';
import { isSafeAvatarUrl } from '@/utils/avatarUrl';
import type { UpdateProfileRequest } from '@/types/profile';
import type { UpdateAssessmentRequest } from '@/types/assessment';
import type { Dashboard, DietType } from '@/types/api';
import UserAvatar from '@/components/UserAvatar.vue';

const router = useRouter();
const toast = useToast();
const meStore = useMeStore();

const inicializando = ref(true);
const carregando = ref(false);
const erroInicial = ref<string | null>(null);

// ── Avatar ───────────────────────────────────────────────────────────────────
const avatarUrlInput = ref<string>('');
const salvandoAvatar = ref(false);
const nomeAtual = ref('');
const emailAtual = ref('');

const erroAvatarUrl = computed<string | null>(() => {
  const v = avatarUrlInput.value.trim();
  if (v === '') return null; // empty means "clear avatar" — valid
  if (!isSafeAvatarUrl(v)) return 'URL inválida. Use um endereço http:// ou https://.';
  return null;
});

const avatarPreviewUrl = computed<string | null>(() => {
  const v = avatarUrlInput.value.trim();
  if (v === '' || !isSafeAvatarUrl(v)) return null;
  return v;
});

async function salvarAvatar(): Promise<void> {
  if (erroAvatarUrl.value !== null) return;
  salvandoAvatar.value = true;
  try {
    const newUrl = avatarUrlInput.value.trim() === '' ? null : avatarUrlInput.value.trim();
    await meService.updateAvatar(newUrl);
    meStore.setAvatar(newUrl);
    toast.success('Avatar atualizado com sucesso.');
  } catch {
    toast.error('Não foi possível atualizar o avatar. Tente novamente.');
  } finally {
    salvandoAvatar.value = false;
  }
}

const form = ref({
  dataNascimento: '',
  genero: 'MALE',
  peso: null as number | null,
  altura: null as number | null,
  nivelAtividade: 'MODERATE',
  objetivo: '',
  qtdRefeicoes: 3,
  pesoAlvo: null as number | null,
  preferenciaAlimentar: 'ONIVORA',
  condicoesSaude: [] as string[],
  alergias: [] as string[],
});

// ── Inline validation ────────────────────────────────────────────────────────
// Ranges are derived directly from VALIDATION_MESSAGES text to stay consistent.
// WEIGHT_RANGE: 20–500 kg | HEIGHT_RANGE: 50–300 cm | TARGET_WEIGHT_POSITIVE: > 0

const erroPeso = computed<string | null>(() => {
  const v = form.value.peso;
  if (v === null || v === undefined) return null;
  if (v < 20 || v > 500) return VALIDATION_MESSAGES.WEIGHT_RANGE;
  return null;
});

const erroAltura = computed<string | null>(() => {
  const v = form.value.altura;
  if (v === null || v === undefined) return null;
  if (v < 50 || v > 300) return VALIDATION_MESSAGES.HEIGHT_RANGE;
  return null;
});

const erroPesoAlvo = computed<string | null>(() => {
  const v = form.value.pesoAlvo;
  if (v === null || v === undefined) return null;
  if (v <= 0) return VALIDATION_MESSAGES.TARGET_WEIGHT_POSITIVE;
  return null;
});

const formularioInvalido = computed(
  () =>
    !form.value.objetivo ||
    erroPeso.value !== null ||
    erroAltura.value !== null ||
    erroPesoAlvo.value !== null,
);

// ── Helpers ──────────────────────────────────────────────────────────────────

function generoLabel(genero: string): string {
  if (genero === 'MALE') return 'Masculino';
  if (genero === 'FEMALE') return 'Feminino';
  return '—';
}

// ── Lifecycle ────────────────────────────────────────────────────────────────

onMounted(async () => {
  try {
    const me = await meService.getMe();
    nomeAtual.value  = me.name ?? '';
    emailAtual.value = me.email ?? '';
    avatarUrlInput.value = me.avatarUrl ?? '';
    if (me.profile) {
      form.value.dataNascimento = me.profile.dateOfBirth?.slice(0, 10) ?? '';
      form.value.genero         = me.profile.gender ?? 'MALE';
      form.value.peso           = me.profile.weightKg ?? null;
      form.value.altura         = me.profile.heightCm ?? null;
      form.value.nivelAtividade = me.profile.activityLevel ?? 'MODERATE';
    }
    if (me.assessment) {
      form.value.objetivo             = me.assessment.goal ?? '';
      form.value.preferenciaAlimentar = me.assessment.dietType ?? 'ONIVORA';
      form.value.condicoesSaude       = splitField(me.assessment.healthConditions);
      form.value.alergias             = splitField(me.assessment.allergies);
      form.value.qtdRefeicoes         = me.assessment.mealsPerDay ?? 3;
      form.value.pesoAlvo             = me.assessment.targetWeightKg ?? null;
    }
  } catch {
    erroInicial.value = COMMON_MESSAGES.LOAD_ERROR;
  } finally {
    inicializando.value = false;
  }
});

// ── Payload builders — unchanged contract ────────────────────────────────────

function buildProfilePayload(): UpdateProfileRequest {
  return {
    weightKg:      form.value.peso ?? undefined,
    heightCm:      form.value.altura ?? undefined,
    activityLevel: form.value.nivelAtividade as Dashboard.ActivityLevel,
  };
}

function buildAssessmentPayload(): UpdateAssessmentRequest {
  return {
    goal:             form.value.objetivo as Dashboard.NutritionalGoal,
    dietType:         form.value.preferenciaAlimentar as DietType,
    healthConditions: form.value.condicoesSaude.join(', '),
    allergies:        form.value.alergias.join(', '),
    mealsPerDay:      form.value.qtdRefeicoes,
    targetWeightKg:   form.value.pesoAlvo ?? undefined,
  };
}

const salvarAlteracoes = async () => {
  carregando.value = true;
  try {
    await profileService.updateOwn(buildProfilePayload());
    await assessmentService.updateOwn(buildAssessmentPayload());
    toast.success(PROFILE_MESSAGES.UPDATE_SUCCESS);
    router.push({ name: 'perfil' });
  } catch {
    toast.error(PROFILE_MESSAGES.SAVE_ERROR);
  } finally {
    carregando.value = false;
  }
};

const cancelar = () => router.back();
</script>

<template>
  <div class="edit-wrapper">

    <!-- NAV -->
    <nav class="edit-nav" aria-label="Navegação da página">
      <button type="button" @click="cancelar" class="btn-back" aria-label="Voltar ao perfil">
        <span class="back-icon" aria-hidden="true"></span>
        Voltar ao Perfil
      </button>
      <span class="nav-badge" aria-hidden="true">Edição de Perfil</span>
    </nav>

    <!-- Estado: carregando -->
    <div v-if="inicializando" class="state-center" role="status" aria-live="polite">
      <div class="spinner" aria-hidden="true"></div>
      <p class="state-msg">{{ PROFILE_MESSAGES.LOADING }}</p>
    </div>

    <!-- Estado: erro inicial -->
    <div v-else-if="erroInicial" class="state-center" role="alert">
      <p class="state-msg state-error">{{ erroInicial }}</p>
    </div>

    <!-- Formulário principal -->
    <main v-else class="edit-container">

      <header class="edit-header">
        <h1 class="edit-title">Editar Informações</h1>
        <p class="edit-subtitle">
          Atualize seus dados para que a IA recalcule seu plano alimentar.
        </p>
      </header>

      <form @submit.prevent="salvarAlteracoes" class="edit-form" novalidate>

        <!-- ── SEÇÃO 1: BIOMETRIA ────────────────────────────────────────── -->
        <section class="form-section" aria-labelledby="section-biometria">
          <div class="section-header">
            <span class="section-number" aria-hidden="true">01</span>
            <h2 id="section-biometria" class="section-title">Dados Biométricos</h2>
          </div>

          <div class="input-grid">

            <!-- Peso -->
            <div class="field">
              <label for="campo-peso" class="field-label">Peso Atual (kg)</label>
              <input
                id="campo-peso"
                type="number"
                step="0.1"
                min="20"
                max="500"
                v-model="form.peso"
                class="field-input"
                :class="{ 'field-input--error': erroPeso }"
                :aria-invalid="erroPeso ? 'true' : 'false'"
                :aria-describedby="erroPeso ? 'erro-peso' : undefined"
                required
              />
              <p
                v-if="erroPeso"
                id="erro-peso"
                class="field-error"
                aria-live="polite"
              >
                {{ erroPeso }}
              </p>
            </div>

            <!-- Altura -->
            <div class="field">
              <label for="campo-altura" class="field-label">Altura (cm)</label>
              <input
                id="campo-altura"
                type="number"
                min="50"
                max="300"
                v-model="form.altura"
                class="field-input"
                :class="{ 'field-input--error': erroAltura }"
                :aria-invalid="erroAltura ? 'true' : 'false'"
                :aria-describedby="erroAltura ? 'erro-altura' : undefined"
                required
              />
              <p
                v-if="erroAltura"
                id="erro-altura"
                class="field-error"
                aria-live="polite"
              >
                {{ erroAltura }}
              </p>
            </div>

            <!-- Gênero — read-only -->
            <div class="field">
              <p class="field-label" id="label-genero">Gênero</p>
              <div class="readonly-field" aria-labelledby="label-genero">
                {{ generoLabel(form.genero) }}
              </div>
              <p class="readonly-note">Gênero não pode ser alterado.</p>
            </div>

            <!-- Data de Nascimento — read-only -->
            <div class="field">
              <p class="field-label" id="label-nascimento">Data de Nascimento</p>
              <div class="readonly-field" aria-labelledby="label-nascimento">
                {{ form.dataNascimento || '—' }}
              </div>
              <p class="readonly-note">Data de nascimento não pode ser alterada.</p>
            </div>

          </div>
        </section>

        <!-- ── SEÇÃO 2: OBJETIVO E ROTINA ───────────────────────────────── -->
        <section class="form-section" aria-labelledby="section-plano">
          <div class="section-header">
            <span class="section-number" aria-hidden="true">02</span>
            <h2 id="section-plano" class="section-title">Objetivo e Rotina</h2>
          </div>

          <!-- Objetivo principal -->
          <div class="field" role="group" aria-labelledby="label-objetivo">
            <p class="field-label" id="label-objetivo">Objetivo Principal</p>
            <div class="goal-grid">
              <button
                v-for="obj in NUTRITIONAL_GOAL_OPTIONS"
                :key="obj.value"
                type="button"
                class="goal-btn"
                :class="{ active: form.objetivo === obj.value }"
                :aria-pressed="form.objetivo === obj.value"
                @click="form.objetivo = obj.value"
              >
                {{ obj.label }}
              </button>
            </div>
          </div>

          <!-- Nível de atividade -->
          <div class="field field--mt">
            <label for="campo-atividade" class="field-label">Nível de Atividade Física</label>
            <select id="campo-atividade" v-model="form.nivelAtividade" class="field-input">
              <option
                v-for="opt in ACTIVITY_LEVEL_OPTIONS"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </option>
            </select>
          </div>

          <div class="input-grid field--mt">

            <!-- Refeições por dia -->
            <div class="field" role="group" aria-labelledby="label-refeicoes">
              <p class="field-label" id="label-refeicoes">Refeições por dia</p>
              <div class="radio-tabs" aria-labelledby="label-refeicoes">
                <button
                  v-for="n in [3, 4, 5]"
                  :key="n"
                  type="button"
                  :class="{ active: form.qtdRefeicoes === n }"
                  :aria-pressed="form.qtdRefeicoes === n"
                  @click="form.qtdRefeicoes = n"
                >
                  {{ n }}
                </button>
              </div>
            </div>

            <!-- Peso alvo -->
            <div class="field">
              <label for="campo-peso-alvo" class="field-label">
                Peso alvo (kg)
                <span class="label-optional">— opcional</span>
              </label>
              <input
                id="campo-peso-alvo"
                type="number"
                step="0.1"
                min="0.1"
                v-model="form.pesoAlvo"
                class="field-input"
                :class="{ 'field-input--error': erroPesoAlvo }"
                :aria-invalid="erroPesoAlvo ? 'true' : 'false'"
                :aria-describedby="erroPesoAlvo ? 'erro-peso-alvo' : undefined"
                placeholder="ex: 75"
              />
              <p
                v-if="erroPesoAlvo"
                id="erro-peso-alvo"
                class="field-error"
                aria-live="polite"
              >
                {{ erroPesoAlvo }}
              </p>
            </div>

          </div>
        </section>

        <!-- ── SEÇÃO 3: SAÚDE ────────────────────────────────────────────── -->
        <section class="form-section" aria-labelledby="section-saude">
          <div class="section-header">
            <span class="section-number" aria-hidden="true">03</span>
            <h2 id="section-saude" class="section-title">Restrições e Saúde</h2>
          </div>

          <!-- Preferência alimentar -->
          <div class="field" role="group" aria-labelledby="label-dieta">
            <p class="field-label" id="label-dieta">Preferência Alimentar</p>
            <div class="radio-tabs radio-tabs--wrap" aria-labelledby="label-dieta">
              <button
                v-for="opt in DIET_TYPE_OPTIONS"
                :key="opt.value"
                type="button"
                :class="{ active: form.preferenciaAlimentar === opt.value }"
                :aria-pressed="form.preferenciaAlimentar === opt.value"
                @click="form.preferenciaAlimentar = opt.value"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>

          <!-- Condições clínicas -->
          <div class="field field--mt" role="group" aria-labelledby="label-condicoes">
            <p class="field-label" id="label-condicoes">Condições Clínicas</p>
            <div class="chip-container" aria-labelledby="label-condicoes">
              <button
                v-for="item in HEALTH_CONDITION_OPTIONS"
                :key="item"
                type="button"
                class="chip"
                :class="{ selected: form.condicoesSaude.includes(item) }"
                :aria-pressed="form.condicoesSaude.includes(item)"
                @click="toggleArrayItem(form.condicoesSaude, item)"
              >
                {{ item }}
              </button>
            </div>
          </div>

          <!-- Alergias -->
          <div class="field field--mt" role="group" aria-labelledby="label-alergias">
            <p class="field-label" id="label-alergias">Alergias</p>
            <div class="chip-container" aria-labelledby="label-alergias">
              <button
                v-for="item in ALLERGY_OPTIONS"
                :key="item"
                type="button"
                class="chip"
                :class="{ selected: form.alergias.includes(item) }"
                :aria-pressed="form.alergias.includes(item)"
                @click="toggleArrayItem(form.alergias, item)"
              >
                {{ item }}
              </button>
            </div>
          </div>

        </section>

        <!-- ── SEÇÃO 4: AVATAR ──────────────────────────────────────────── -->
        <section class="form-section" aria-labelledby="section-avatar">
          <div class="section-header">
            <span class="section-number" aria-hidden="true">04</span>
            <h2 id="section-avatar" class="section-title">Foto de Perfil</h2>
          </div>

          <div class="avatar-edit-layout">
            <UserAvatar
              :name="nomeAtual || undefined"
              :email="emailAtual || undefined"
              :avatar-url="avatarPreviewUrl"
              :size="64"
            />
            <div class="avatar-edit-fields">
              <div class="field">
                <label for="campo-avatar-url" class="field-label">
                  URL da imagem
                  <span class="label-optional">— opcional</span>
                </label>
                <input
                  id="campo-avatar-url"
                  v-model="avatarUrlInput"
                  type="url"
                  placeholder="https://exemplo.com/foto.jpg"
                  class="field-input"
                  :class="{ 'field-input--error': erroAvatarUrl }"
                  :aria-invalid="erroAvatarUrl ? 'true' : 'false'"
                  :aria-describedby="erroAvatarUrl ? 'erro-avatar-url' : undefined"
                />
                <p
                  v-if="erroAvatarUrl"
                  id="erro-avatar-url"
                  class="field-error"
                  aria-live="polite"
                >
                  {{ erroAvatarUrl }}
                </p>
                <p class="readonly-note">Deixe vazio para remover o avatar e usar as iniciais.</p>
              </div>
              <button
                type="button"
                class="btn-save avatar-save-btn"
                :disabled="salvandoAvatar || erroAvatarUrl !== null"
                :aria-busy="salvandoAvatar"
                @click="salvarAvatar"
              >
                <span v-if="salvandoAvatar" class="btn-spinner" aria-hidden="true"></span>
                {{ salvandoAvatar ? 'Salvando...' : 'Salvar Avatar' }}
              </button>
            </div>
          </div>
        </section>

        <!-- ── AÇÕES ──────────────────────────────────────────────────────── -->
        <div class="form-actions">
          <button
            type="submit"
            class="btn-save"
            :disabled="carregando || formularioInvalido"
            :aria-busy="carregando"
          >
            <span v-if="carregando" class="btn-spinner" aria-hidden="true"></span>
            {{ carregando ? 'Salvando...' : 'Confirmar Alterações' }}
          </button>
          <button type="button" @click="cancelar" class="btn-cancel">
            Descartar mudanças
          </button>
        </div>

      </form>
    </main>

  </div>
</template>

<style scoped>
/* ── Design tokens ────────────────────────────────────────────────────── */
.edit-wrapper {
  --bg-dark:   #0f172a;
  --card-dark: #1e293b;
  --card-hover: #243347;
  --accent:    #10b981;
  --accent-hi: #34d399;
  --danger:    #f87171;
  --text-main: #f8fafc;
  --text-muted: #94a3b8;
  --border:    rgba(255, 255, 255, 0.05);
  --border-input: rgba(255, 255, 255, 0.1);
  --transition: 0.2s ease;

  min-height: 100vh;
  background-color: var(--bg-dark);
  color: var(--text-main);
  font-family: 'Inter', sans-serif;
}

/* ── Navigation bar ───────────────────────────────────────────────────── */
.edit-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 5%;
  background: rgba(15, 23, 42, 0.85);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 20;
  backdrop-filter: blur(8px);
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 8px;
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
  padding: 0.5rem 0;
  transition: color var(--transition);
}
.btn-back:hover { color: var(--accent); }
.btn-back:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 4px;
  border-radius: 6px;
}

.back-icon {
  width: 8px;
  height: 8px;
  border-left: 2px solid currentColor;
  border-bottom: 2px solid currentColor;
  transform: rotate(45deg);
  flex-shrink: 0;
}

.nav-badge {
  font-size: 0.72rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--text-muted);
}

/* ── Estado: carregando / erro ─────────────────────────────────────────── */
.state-center {
  min-height: 50vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 2rem;
}

.state-msg {
  color: var(--text-muted);
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
}
.state-error { color: var(--danger); }

/* Spinner para o estado de carregamento inicial */
.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(16, 185, 129, 0.15);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.75s linear infinite;
}

/* Spinner inline do botão de salvar */
.btn-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(15, 23, 42, 0.3);
  border-top-color: var(--bg-dark);
  border-radius: 50%;
  animation: spin 0.75s linear infinite;
  vertical-align: middle;
  margin-right: 6px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ── Página principal ─────────────────────────────────────────────────── */
.edit-container {
  max-width: 720px;
  margin: 0 auto;
  padding: 3.5rem 1.5rem 5rem;
}

/* ── Header da página ─────────────────────────────────────────────────── */
.edit-header {
  margin-bottom: 3rem;
}

.edit-title {
  font-size: 2rem;
  font-weight: 900;
  letter-spacing: -0.5px;
  margin: 0 0 0.5rem;
  line-height: 1.15;
}

.edit-subtitle {
  color: var(--text-muted);
  font-size: 0.95rem;
  line-height: 1.6;
  margin: 0;
}

/* ── Seções do formulário ─────────────────────────────────────────────── */
.form-section {
  background: var(--card-dark);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 2rem 2.5rem;
  margin-bottom: 1.5rem;
  transition: border-color var(--transition);
}
.form-section:hover { border-color: rgba(255, 255, 255, 0.08); }

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 1.75rem;
}

.section-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(16, 185, 129, 0.12);
  color: var(--accent);
  font-size: 0.78rem;
  font-weight: 800;
  flex-shrink: 0;
}

.section-title {
  font-size: 0.85rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--text-muted);
  margin: 0;
}

/* ── Grid biométrico ──────────────────────────────────────────────────── */
.input-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}

/* ── Campo genérico ───────────────────────────────────────────────────── */
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field--mt { margin-top: 1.5rem; }

.field-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0;
}

.label-optional {
  font-size: 0.75rem;
  font-weight: 400;
  text-transform: none;
  letter-spacing: 0;
}

/* ── Inputs e selects ─────────────────────────────────────────────────── */
.field-input {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid var(--border-input);
  border-radius: 12px;
  padding: 0.85rem 1rem;
  color: var(--text-main);
  font-size: 0.95rem;
  font-family: inherit;
  transition: border-color var(--transition), box-shadow var(--transition);
  width: 100%;
  box-sizing: border-box;
}
.field-input:focus {
  border-color: var(--accent);
  outline: none;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.15);
}
.field-input:focus-visible {
  border-color: var(--accent);
  outline: none;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.25);
}
.field-input--error {
  border-color: var(--danger);
  box-shadow: 0 0 0 3px rgba(248, 113, 113, 0.1);
}
.field-input--error:focus,
.field-input--error:focus-visible {
  border-color: var(--danger);
  box-shadow: 0 0 0 3px rgba(248, 113, 113, 0.2);
}

/* ── Campos somente leitura ───────────────────────────────────────────── */
.readonly-field {
  padding: 0.85rem 1rem;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.02);
  color: var(--text-muted);
  font-size: 0.95rem;
  cursor: not-allowed;
}

.readonly-note {
  font-size: 0.75rem;
  color: var(--text-muted);
  opacity: 0.6;
  margin: 0;
}

/* ── Mensagem de erro inline ──────────────────────────────────────────── */
.field-error {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--danger);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 5px;
}
.field-error::before {
  content: '!';
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--danger);
  color: #0f172a;
  font-size: 0.65rem;
  font-weight: 900;
  flex-shrink: 0;
}

/* ── Botões de objetivo (goal grid) ───────────────────────────────────── */
.goal-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.goal-btn {
  padding: 0.7rem 0.5rem;
  border-radius: 12px;
  border: 1px solid var(--border-input);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  font-family: inherit;
  font-size: 0.82rem;
  font-weight: 600;
  text-align: center;
  transition: background var(--transition), border-color var(--transition), color var(--transition);
}
.goal-btn:hover {
  border-color: rgba(16, 185, 129, 0.4);
  color: var(--text-main);
  background: rgba(16, 185, 129, 0.05);
}
.goal-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.goal-btn.active {
  background: rgba(16, 185, 129, 0.12);
  border-color: var(--accent);
  color: var(--accent);
}

/* ── Radio tabs (refeições + dieta) ───────────────────────────────────── */
.radio-tabs {
  display: flex;
  gap: 8px;
}
.radio-tabs--wrap { flex-wrap: wrap; }

.radio-tabs button {
  padding: 0.65rem 1rem;
  border-radius: 12px;
  border: 1px solid var(--border-input);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  font-family: inherit;
  font-size: 0.83rem;
  font-weight: 600;
  transition: background var(--transition), border-color var(--transition), color var(--transition);
  white-space: nowrap;
}
.radio-tabs button:hover {
  border-color: rgba(16, 185, 129, 0.4);
  color: var(--text-main);
  background: rgba(16, 185, 129, 0.05);
}
.radio-tabs button:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.radio-tabs button.active {
  background: var(--accent);
  color: var(--bg-dark);
  border-color: var(--accent);
  font-weight: 700;
}

/* ── Chips ────────────────────────────────────────────────────────────── */
.chip-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-input);
  color: var(--text-muted);
  padding: 6px 14px;
  border-radius: 20px;
  font-family: inherit;
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: background var(--transition), border-color var(--transition), color var(--transition);
}
.chip:hover {
  border-color: rgba(16, 185, 129, 0.4);
  color: var(--text-main);
  background: rgba(16, 185, 129, 0.05);
}
.chip:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.chip.selected {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(16, 185, 129, 0.1);
}

/* ── Ações finais ─────────────────────────────────────────────────────── */
.form-actions {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.875rem;
  margin-top: 2rem;
}

.btn-save {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: var(--accent);
  color: var(--bg-dark);
  border: none;
  border-radius: 16px;
  padding: 1.1rem;
  font-family: inherit;
  font-size: 1rem;
  font-weight: 800;
  cursor: pointer;
  transition: transform var(--transition), box-shadow var(--transition), opacity var(--transition);
}
.btn-save:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(16, 185, 129, 0.25);
}
.btn-save:focus-visible {
  outline: 2px solid var(--accent-hi);
  outline-offset: 3px;
}
.btn-save:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.btn-cancel {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-family: inherit;
  font-size: 0.875rem;
  font-weight: 600;
  padding: 0.5rem;
  cursor: pointer;
  transition: color var(--transition);
}
.btn-cancel:hover { color: var(--text-main); }
.btn-cancel:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 4px;
  border-radius: 6px;
}

/* ── Avatar edit layout ───────────────────────────────────────────────── */
.avatar-edit-layout {
  display: flex;
  align-items: flex-start;
  gap: 1.5rem;
}

.avatar-edit-fields {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.avatar-save-btn {
  align-self: flex-start;
  padding: 0.75rem 1.5rem;
  font-size: 0.875rem;
}

/* ── Responsividade ───────────────────────────────────────────────────── */
@media (max-width: 640px) {
  .edit-container { padding: 2.5rem 1rem 4rem; }
  .form-section { padding: 1.5rem 1.25rem; }
  .input-grid { grid-template-columns: 1fr; }
  .goal-grid { grid-template-columns: 1fr 1fr; }
  .edit-title { font-size: 1.6rem; }
  .avatar-edit-layout { flex-direction: column; align-items: center; }
  .avatar-save-btn { align-self: stretch; }
}
</style>
