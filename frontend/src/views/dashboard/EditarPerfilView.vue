<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { profileService } from '@/services/modules/profile';
import { assessmentService } from '@/services/modules/assessment';
import { meService } from '@/services/modules/me';

const router = useRouter();

const inicializando = ref(true);
const carregando = ref(false);
const erroInicial = ref<string | null>(null);
const erroSalvar = ref<string | null>(null);

const listaCondicoes = [
  'Diabetes Tipo 1', 'Diabetes Tipo 2', 'Hipertensão', 'Colesterol Alto',
  'Gastrite', 'Anemia', 'Hipotireoidismo', 'Hipertireoidismo', 'Síndrome do Intestino Irritável',
];
const listaAlergias = [
  'Glúten', 'Lactose', 'Frutos do Mar', 'Amendoim', 'Ovo',
  'Soja', 'Proteína do Leite (APLV)', 'Trigo', 'Nozes',
];

const OBJETIVOS = [
  { value: 'WEIGHT_LOSS',          label: 'Emagrecimento' },
  { value: 'MUSCLE_GAIN',          label: 'Ganho de Massa' },
  { value: 'MAINTENANCE',          label: 'Manutenção' },
  { value: 'DIETARY_REEDUCATION',  label: 'Reeducação Alimentar' },
  { value: 'SPORTS_PERFORMANCE',   label: 'Performance Esportiva' },
];

const form = ref({
  dataNascimento: '',
  genero: 'MALE',
  peso: null as number | null,
  altura: null as number | null,
  nivelAtividade: 'MODERATE',
  objetivo: '',
  preferenciaAlimentar: 'ONIVORA',
  condicoesSaude: [] as string[],
  alergias: [] as string[],
});

function splitField(raw: string | null | undefined): string[] {
  if (!raw) return [];
  return raw.split(',').map(s => s.trim()).filter(Boolean);
}

onMounted(async () => {
  try {
    const me = await meService.getMe();
    if (me.profile) {
      form.value.dataNascimento = me.profile.dateOfBirth?.slice(0, 10) ?? '';
      form.value.genero         = me.profile.gender ?? 'MALE';
      form.value.peso           = me.profile.weightKg ?? null;
      form.value.altura         = me.profile.heightCm ?? null;
      form.value.nivelAtividade = me.profile.activityLevel ?? 'MODERATE';
    }
    if (me.assessment) {
      form.value.objetivo            = me.assessment.goal ?? '';
      form.value.preferenciaAlimentar = me.assessment.dietaryRestrictions ?? 'ONIVORA';
      form.value.condicoesSaude      = splitField(me.assessment.healthConditions);
      form.value.alergias            = splitField(me.assessment.allergies);
    }
  } catch {
    erroInicial.value = 'Não foi possível carregar seus dados. Tente novamente.';
  } finally {
    inicializando.value = false;
  }
});

const toggleItem = (lista: 'condicoesSaude' | 'alergias', item: string) => {
  const idx = form.value[lista].indexOf(item);
  if (idx > -1) form.value[lista].splice(idx, 1);
  else form.value[lista].push(item);
};

const salvarAlteracoes = async () => {
  erroSalvar.value = null;
  try {
    carregando.value = true;

    await profileService.updateOwn({
      dateOfBirth:   form.value.dataNascimento,
      gender:        form.value.genero,
      weightKg:      form.value.peso,
      heightCm:      form.value.altura,
      activityLevel: form.value.nivelAtividade,
    });

    await assessmentService.updateOwn({
      goal:                form.value.objetivo,
      dietaryRestrictions: form.value.preferenciaAlimentar,
      healthConditions:    form.value.condicoesSaude.join(', '),
      allergies:           form.value.alergias.join(', '),
    });

    router.push({ name: 'perfil' });
  } catch {
    erroSalvar.value = 'Erro ao salvar. Verifique os dados e tente novamente.';
  } finally {
    carregando.value = false;
  }
};

const cancelar = () => router.back();
</script>

<template>
  <div class="edit-wrapper">
    <nav class="edit-nav">
      <button @click="cancelar" class="btn-back">
        <span class="back-icon"></span>
        Voltar ao Perfil
      </button>
      <span class="nav-title">Ajustes de Perfil</span>
    </nav>

    <!-- Loading inicial -->
    <div v-if="inicializando" class="state-center">
      <p class="state-msg">Carregando seus dados...</p>
    </div>

    <!-- Erro inicial -->
    <div v-else-if="erroInicial" class="state-center">
      <p class="state-msg state-error">{{ erroInicial }}</p>
    </div>

    <!-- Formulário -->
    <main v-else class="edit-container">
      <header class="edit-intro">
        <h1>Editar Informações</h1>
        <p>Atualize seus dados para que a IA recalcule seu plano alimentar.</p>
      </header>

      <form @submit.prevent="salvarAlteracoes" class="edit-form">

        <!-- SEÇÃO 1: BIOMETRIA -->
        <section class="form-section">
          <div class="section-header">
            <span class="section-number">01</span>
            <h3>Dados Biométricos</h3>
          </div>
          <div class="input-grid">
            <div class="field">
              <label>Peso Atual (kg)</label>
              <input type="number" step="0.1" v-model="form.peso" required />
            </div>
            <div class="field">
              <label>Altura (cm)</label>
              <input type="number" v-model="form.altura" required />
            </div>
            <div class="field">
              <label>Gênero</label>
              <select v-model="form.genero">
                <option value="MALE">Masculino</option>
                <option value="FEMALE">Feminino</option>
              </select>
            </div>
            <div class="field">
              <label>Data de Nascimento</label>
              <input type="date" v-model="form.dataNascimento" required />
            </div>
          </div>
        </section>

        <!-- SEÇÃO 2: PLANO -->
        <section class="form-section">
          <div class="section-header">
            <span class="section-number">02</span>
            <h3>Objetivo e Rotina</h3>
          </div>

          <div class="field">
            <label>Objetivo Principal</label>
            <div class="goal-grid">
              <button
                v-for="obj in OBJETIVOS"
                :key="obj.value"
                type="button"
                class="goal-btn"
                :class="{ active: form.objetivo === obj.value }"
                @click="form.objetivo = obj.value"
              >
                {{ obj.label }}
              </button>
            </div>
          </div>

          <div class="field mt-4">
            <label>Nível de Atividade Física</label>
            <select v-model="form.nivelAtividade">
              <option value="SEDENTARY">Sedentário (pouco movimento)</option>
              <option value="MODERATE">Moderado (3–5x por semana)</option>
              <option value="INTENSE">Intenso (treino diário)</option>
            </select>
          </div>
        </section>

        <!-- SEÇÃO 3: SAÚDE -->
        <section class="form-section">
          <div class="section-header">
            <span class="section-number">03</span>
            <h3>Restrições e Saúde</h3>
          </div>

          <div class="field">
            <label>Preferência Alimentar</label>
            <div class="radio-tabs">
              <button type="button" :class="{ active: form.preferenciaAlimentar === 'ONIVORA' }"    @click="form.preferenciaAlimentar = 'ONIVORA'">Onívora</button>
              <button type="button" :class="{ active: form.preferenciaAlimentar === 'VEGETARIANA' }" @click="form.preferenciaAlimentar = 'VEGETARIANA'">Vegetariana</button>
              <button type="button" :class="{ active: form.preferenciaAlimentar === 'VEGANA' }"      @click="form.preferenciaAlimentar = 'VEGANA'">Vegana</button>
              <button type="button" :class="{ active: form.preferenciaAlimentar === 'PESCATARIANA' }" @click="form.preferenciaAlimentar = 'PESCATARIANA'">Pescatariana</button>
            </div>
          </div>

          <div class="field mt-4">
            <label>Condições Clínicas</label>
            <div class="chip-container">
              <button
                v-for="item in listaCondicoes" :key="item" type="button"
                class="chip" :class="{ selected: form.condicoesSaude.includes(item) }"
                @click="toggleItem('condicoesSaude', item)"
              >{{ item }}</button>
            </div>
          </div>

          <div class="field mt-4">
            <label>Alergias</label>
            <div class="chip-container">
              <button
                v-for="item in listaAlergias" :key="item" type="button"
                class="chip" :class="{ selected: form.alergias.includes(item) }"
                @click="toggleItem('alergias', item)"
              >{{ item }}</button>
            </div>
          </div>
        </section>

        <!-- AÇÕES -->
        <p v-if="erroSalvar" class="erro-salvar">{{ erroSalvar }}</p>
        <div class="form-actions">
          <button type="submit" class="btn-save" :disabled="carregando || !form.objetivo">
            {{ carregando ? 'Salvando...' : 'Confirmar Alterações' }}
          </button>
          <button type="button" @click="cancelar" class="btn-cancel">Descartar mudanças</button>
        </div>

      </form>
    </main>
  </div>
</template>

<style scoped>
.edit-wrapper {
  --bg-dark: #0f172a;
  --card-dark: #1e293b;
  --accent: #10b981;
  --text-muted: #94a3b8;

  min-height: 100vh;
  background-color: var(--bg-dark);
  color: white;
  font-family: 'Inter', sans-serif;
}

/* NAV */
.edit-nav {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1.5rem 5%; background: rgba(15, 23, 42, 0.8);
  border-bottom: 1px solid rgba(255,255,255,0.05); position: sticky; top: 0; z-index: 10;
}
.btn-back { background: transparent; border: none; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 8px; font-weight: 600; }
.btn-back:hover { color: var(--accent); }
.back-icon { width: 8px; height: 8px; border-left: 2px solid currentColor; border-bottom: 2px solid currentColor; transform: rotate(45deg); }
.nav-title { font-weight: 800; text-transform: uppercase; letter-spacing: 1px; font-size: 0.8rem; }

/* ESTADOS */
.state-center { min-height: 50vh; display: flex; align-items: center; justify-content: center; }
.state-msg { color: var(--text-muted); font-size: 1rem; }
.state-error { color: #f87171; }

/* CONTAINER */
.edit-container { max-width: 700px; margin: 0 auto; padding: 4rem 1.5rem; }
.edit-intro h1 { font-size: 2.2rem; font-weight: 800; margin-bottom: 0.5rem; }
.edit-intro p { color: var(--text-muted); margin-bottom: 3rem; }

/* FORM SECTIONS */
.form-section { background: var(--card-dark); padding: 2.5rem; border-radius: 24px; margin-bottom: 2rem; border: 1px solid rgba(255,255,255,0.05); }
.section-header { display: flex; align-items: center; gap: 12px; margin-bottom: 2rem; }
.section-number { color: var(--accent); font-weight: 800; font-size: 0.9rem; background: rgba(16, 185, 129, 0.1); width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 8px; }
.section-header h3 { font-size: 1.1rem; font-weight: 700; }

.input-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
.field { display: flex; flex-direction: column; gap: 8px; margin-bottom: 1.5rem; }
.field label { font-size: 0.85rem; font-weight: 600; color: var(--text-muted); }

input, select {
  background: rgba(15, 23, 42, 0.6); border: 1px solid rgba(255,255,255,0.1);
  padding: 0.9rem; border-radius: 12px; color: white; font-size: 0.95rem;
}
input:focus, select:focus { border-color: var(--accent); outline: none; }

/* GOALS */
.goal-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.goal-btn {
  padding: 0.75rem 0.5rem; border-radius: 12px; border: 1px solid rgba(255,255,255,0.1);
  background: transparent; color: var(--text-muted); cursor: pointer; font-weight: 600;
  font-size: 0.82rem; transition: 0.2s; text-align: center;
}
.goal-btn:hover { border-color: rgba(16,185,129,0.4); color: white; }
.goal-btn.active { background: rgba(16,185,129,0.12); border-color: var(--accent); color: var(--accent); }

/* RADIO TABS */
.radio-tabs { display: flex; flex-wrap: wrap; gap: 8px; }
.radio-tabs button {
  padding: 0.7rem 1.1rem; border-radius: 12px; border: 1px solid rgba(255,255,255,0.1);
  background: transparent; color: var(--text-muted); cursor: pointer; font-weight: 600; font-size: 0.85rem; transition: 0.2s;
}
.radio-tabs button.active { background: var(--accent); color: var(--bg-dark); border-color: var(--accent); }

/* CHIPS */
.chip-container { display: flex; flex-wrap: wrap; gap: 8px; }
.chip {
  background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-muted); padding: 7px 14px; border-radius: 20px; font-size: 0.8rem; cursor: pointer; transition: 0.2s;
}
.chip.selected { border-color: var(--accent); color: var(--accent); background: rgba(16, 185, 129, 0.1); }

/* AÇÕES */
.erro-salvar { color: #f87171; font-size: 0.85rem; margin-bottom: 0.5rem; }
.form-actions { display: flex; flex-direction: column; gap: 1rem; margin-top: 2rem; }
.btn-save {
  background: var(--accent); color: var(--bg-dark); border: none; padding: 1.2rem;
  border-radius: 16px; font-weight: 800; font-size: 1.1rem; cursor: pointer; transition: all 0.3s;
}
.btn-save:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-save:not(:disabled):hover { transform: translateY(-2px); box-shadow: 0 10px 20px rgba(16, 185, 129, 0.2); }
.btn-cancel { background: transparent; border: none; color: var(--text-muted); padding: 0.5rem; cursor: pointer; font-weight: 600; }

.mt-4 { margin-top: 1.5rem; }

@media (max-width: 600px) {
  .input-grid { grid-template-columns: 1fr; }
  .goal-grid { grid-template-columns: 1fr 1fr; }
}
</style>
