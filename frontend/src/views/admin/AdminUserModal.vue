<script setup lang="ts">
import { ref, watch } from 'vue'
import { adminUserService } from '../../services/modules/adminUser'
import { adminProfileService } from '../../services/modules/adminProfile'
import { adminAssessmentService } from '../../services/modules/adminAssessment'

interface AdminUser {
  id: number
  name: string
  email: string
  type: 'ADMIN' | 'USER'
  status: 'ACTIVE' | 'BLOCKED' | 'BANNED'
}

interface ProfileData {
  id: number
  userId: number
  dateOfBirth: string
  gender: 'MALE' | 'FEMALE'
  weightKg: number
  heightCm: number
  activityLevel: string
}

interface AssessmentData {
  id: number
  userId: number
  goal: string
  dietaryRestrictions: string | null
  allergies: string | null
  healthConditions: string | null
  mealsPerDay: number | null
  targetWeightKg: number | null
}

type TabState<T> = T | 'loading' | 'not-found' | 'error'

const props = defineProps<{ user: AdminUser }>()
const emit = defineEmits<{
  close: []
  updated: [user: AdminUser]
  deleted: [userId: number]
}>()

type Tab = 'dados' | 'perfil' | 'triagem' | 'acoes'
const abaAtiva = ref<Tab>('dados')

const erro = ref<string | null>(null)
const sucesso = ref<string | null>(null)

function clearMensagens() {
  erro.value = null
  sucesso.value = null
}

// ═══════════════════════════════════════════
// ABA DADOS
// ═══════════════════════════════════════════
const formDados = ref({
  name: props.user.name,
  email: props.user.email,
  type: props.user.type,
  status: props.user.status,
})
const loadingDados = ref(false)

async function salvarDados() {
  if (!formDados.value.name.trim()) { erro.value = 'Nome é obrigatório.'; return }
  if (!formDados.value.email.includes('@')) { erro.value = 'E-mail inválido.'; return }
  clearMensagens()
  loadingDados.value = true
  try {
    await adminUserService.update(props.user.id, formDados.value)
    sucesso.value = 'Dados atualizados com sucesso.'
    emit('updated', { ...props.user, ...formDados.value })
  } catch (err: any) {
    erro.value = err.response?.data?.message ?? 'Erro ao salvar dados.'
  } finally {
    loadingDados.value = false
  }
}

// ═══════════════════════════════════════════
// ABA PERFIL
// ═══════════════════════════════════════════
const perfil = ref<TabState<ProfileData>>('loading')
const formPerfil = ref({ dateOfBirth: '', gender: 'MALE' as string, weightKg: '' as string | number, heightCm: '' as string | number, activityLevel: 'SEDENTARY' })
const loadingPerfil = ref(false)
const confirmExcluirPerfil = ref(false)
const modoEdicaoPerfil = ref(false)
const modoNovoPerfil = ref(false)

async function carregarPerfil() {
  perfil.value = 'loading'
  try {
    const data: ProfileData = await adminProfileService.findByUserId(props.user.id)
    perfil.value = data
    formPerfil.value = {
      dateOfBirth: data.dateOfBirth?.slice(0, 10) ?? '',
      gender: data.gender,
      weightKg: data.weightKg,
      heightCm: data.heightCm,
      activityLevel: data.activityLevel,
    }
  } catch (err: any) {
    perfil.value = err.response?.status === 404 ? 'not-found' : 'error'
    if (err.response?.status !== 404) erro.value = 'Erro ao carregar perfil.'
  }
}

function iniciarNovoPerfil() {
  formPerfil.value = { dateOfBirth: '', gender: 'MALE', weightKg: '', heightCm: '', activityLevel: 'SEDENTARY' }
  modoNovoPerfil.value = true
}

// Valida formulário de perfil antes de enviar
function validarPerfil(): string | null {
  const f = formPerfil.value
  if (!f.dateOfBirth) return 'Data de nascimento é obrigatória.'
  if (new Date(f.dateOfBirth as string) >= new Date()) return 'Data de nascimento deve estar no passado.'
  if (!f.gender) return 'Gênero é obrigatório.'
  const peso = Number(f.weightKg)
  if (!f.weightKg || isNaN(peso) || peso < 20 || peso > 500) return 'Peso deve ser entre 20 e 500 kg.'
  const altura = Number(f.heightCm)
  if (!f.heightCm || isNaN(altura) || altura < 50 || altura > 300) return 'Altura deve ser entre 50 e 300 cm.'
  if (!f.activityLevel) return 'Nível de atividade é obrigatório.'
  return null
}

async function salvarPerfil() {
  const erroValidacao = validarPerfil()
  if (erroValidacao) { erro.value = erroValidacao; return }
  clearMensagens()
  loadingPerfil.value = true
  try {
    const updated: ProfileData = await adminProfileService.update((perfil.value as ProfileData).id, formPerfil.value)
    perfil.value = updated
    modoEdicaoPerfil.value = false
    sucesso.value = 'Perfil atualizado com sucesso.'
  } catch (err: any) {
    erro.value = err.response?.data?.message ?? 'Erro ao salvar perfil.'
  } finally {
    loadingPerfil.value = false
  }
}

async function criarPerfil() {
  const erroValidacao = validarPerfil()
  if (erroValidacao) { erro.value = erroValidacao; return }
  clearMensagens()
  loadingPerfil.value = true
  try {
    const created: ProfileData = await adminProfileService.create(props.user.id, formPerfil.value)
    perfil.value = created
    modoNovoPerfil.value = false
    sucesso.value = 'Perfil criado com sucesso.'
  } catch (err: any) {
    erro.value = err.response?.data?.message ?? 'Erro ao criar perfil.'
  } finally {
    loadingPerfil.value = false
  }
}

// Antes de mostrar confirmação de excluir perfil, garante que o estado da triagem é conhecido
async function solicitarExclusaoPerfil() {
  if (triagem.value === 'loading') {
    await carregarTriagem()
  }
  confirmExcluirPerfil.value = true
}

// Exclusão de perfil com cascata condicional sobre triagem
async function confirmarExcluirPerfil() {
  clearMensagens()
  confirmExcluirPerfil.value = false
  loadingPerfil.value = true

  const triagemExistente = typeof triagem.value === 'object' && triagem.value !== null

  // Passo 1: excluir triagem vinculada, se existir
  if (triagemExistente) {
    try {
      await adminAssessmentService.delete((triagem.value as AssessmentData).id)
      triagem.value = 'not-found'
    } catch (err: any) {
      erro.value = (err.response?.data?.message ?? 'Erro ao excluir triagem vinculada.') + ' O perfil não foi excluído.'
      loadingPerfil.value = false
      return
    }
  }

  // Passo 2: excluir o perfil
  try {
    await adminProfileService.delete((perfil.value as ProfileData).id)
    perfil.value = 'not-found'
    modoEdicaoPerfil.value = false
    sucesso.value = triagemExistente
      ? 'Perfil e triagem excluídos com sucesso.'
      : 'Perfil excluído com sucesso.'
  } catch (err: any) {
    // Triagem já foi excluída mas perfil falhou — recarrega para estado consistente
    erro.value = (err.response?.data?.message ?? 'Erro ao excluir perfil.') + ' A triagem já foi removida. Recarregando dados.'
    await Promise.all([carregarPerfil(), carregarTriagem()])
  } finally {
    loadingPerfil.value = false
  }
}

// ═══════════════════════════════════════════
// ABA TRIAGEM
// ═══════════════════════════════════════════
const triagem = ref<TabState<AssessmentData>>('loading')
const formTriagem = ref({ goal: 'WEIGHT_LOSS', dietaryRestrictions: '', allergies: '', healthConditions: '', mealsPerDay: null as number | null, targetWeightKg: null as number | null })
const loadingTriagem = ref(false)
const confirmExcluirTriagem = ref(false)
const modoEdicaoTriagem = ref(false)
const modoNovaTriagem = ref(false)

async function carregarTriagem() {
  triagem.value = 'loading'
  try {
    const data: AssessmentData = await adminAssessmentService.findByUserId(props.user.id)
    triagem.value = data
    formTriagem.value = {
      goal: data.goal,
      dietaryRestrictions: data.dietaryRestrictions ?? '',
      allergies: data.allergies ?? '',
      healthConditions: data.healthConditions ?? '',
      mealsPerDay: data.mealsPerDay,
      targetWeightKg: data.targetWeightKg,
    }
  } catch (err: any) {
    triagem.value = err.response?.status === 404 ? 'not-found' : 'error'
    if (err.response?.status !== 404) erro.value = 'Erro ao carregar triagem.'
  }
}

function iniciarNovaTriagem() {
  formTriagem.value = { goal: 'WEIGHT_LOSS', dietaryRestrictions: '', allergies: '', healthConditions: '', mealsPerDay: null, targetWeightKg: null }
  modoNovaTriagem.value = true
}

// Valida formulário de triagem antes de enviar
function validarTriagem(): string | null {
  const f = formTriagem.value
  if (!f.goal) return 'Objetivo nutricional é obrigatório.'
  if (f.mealsPerDay !== null && f.mealsPerDay !== undefined) {
    if (f.mealsPerDay < 1 || f.mealsPerDay > 10) return 'Refeições por dia deve ser entre 1 e 10.'
  }
  if (f.targetWeightKg !== null && f.targetWeightKg !== undefined && f.targetWeightKg <= 0) {
    return 'Peso alvo deve ser positivo.'
  }
  return null
}

async function salvarTriagem() {
  const erroValidacao = validarTriagem()
  if (erroValidacao) { erro.value = erroValidacao; return }
  clearMensagens()
  loadingTriagem.value = true
  try {
    const updated: AssessmentData = await adminAssessmentService.update((triagem.value as AssessmentData).id, formTriagem.value)
    triagem.value = updated
    modoEdicaoTriagem.value = false
    sucesso.value = 'Triagem atualizada com sucesso.'
  } catch (err: any) {
    erro.value = err.response?.data?.message ?? 'Erro ao salvar triagem.'
  } finally {
    loadingTriagem.value = false
  }
}

async function criarTriagem() {
  const erroValidacao = validarTriagem()
  if (erroValidacao) { erro.value = erroValidacao; return }
  clearMensagens()
  loadingTriagem.value = true
  try {
    const created: AssessmentData = await adminAssessmentService.create(props.user.id, formTriagem.value)
    triagem.value = created
    modoNovaTriagem.value = false
    sucesso.value = 'Triagem criada com sucesso.'
  } catch (err: any) {
    erro.value = err.response?.data?.message ?? 'Erro ao criar triagem.'
  } finally {
    loadingTriagem.value = false
  }
}

async function confirmarExcluirTriagem() {
  clearMensagens()
  confirmExcluirTriagem.value = false
  loadingTriagem.value = true
  try {
    await adminAssessmentService.delete((triagem.value as AssessmentData).id)
    triagem.value = 'not-found'
    modoEdicaoTriagem.value = false
    sucesso.value = 'Triagem excluída.'
  } catch (err: any) {
    erro.value = err.response?.data?.message ?? 'Erro ao excluir triagem.'
  } finally {
    loadingTriagem.value = false
  }
}

// ═══════════════════════════════════════════
// ABA AÇÕES
// ═══════════════════════════════════════════
const confirmDeletarUsuario = ref(false)
const loadingAcao = ref<string | null>(null)

async function revogarSessoes() {
  clearMensagens()
  loadingAcao.value = 'sessions'
  try {
    await adminUserService.revokeSessions(props.user.id)
    sucesso.value = 'Sessões revogadas com sucesso.'
  } catch (err: any) {
    erro.value = err.response?.data?.message ?? 'Erro ao revogar sessões.'
  } finally {
    loadingAcao.value = null
  }
}

async function confirmarDeletarUsuario() {
  clearMensagens()
  confirmDeletarUsuario.value = false
  loadingAcao.value = 'delete'
  try {
    await adminUserService.delete(props.user.id)
    emit('deleted', props.user.id)
  } catch (err: any) {
    erro.value = err.response?.data?.message ?? 'Erro ao deletar usuário.'
    loadingAcao.value = null
  }
}

// Lazy-load ao trocar aba
// Ao entrar na aba triagem, também carrega perfil para saber se pode criar triagem
watch(abaAtiva, (tab) => {
  clearMensagens()
  if (tab === 'perfil' && perfil.value === 'loading') carregarPerfil()
  if (tab === 'triagem') {
    if (triagem.value === 'loading') carregarTriagem()
    if (perfil.value === 'loading') carregarPerfil()
  }
  modoEdicaoPerfil.value = false
  modoNovoPerfil.value = false
  modoEdicaoTriagem.value = false
  modoNovaTriagem.value = false
})

function labelActivityLevel(v: string) {
  const m: Record<string, string> = { SEDENTARY: 'Sedentário', MODERATE: 'Moderado', INTENSE: 'Intenso', VERY_INTENSE: 'Muito Intenso' }
  return m[v] ?? v
}
function labelGoal(v: string) {
  const m: Record<string, string> = { WEIGHT_LOSS: 'Perda de Peso', MUSCLE_GAIN: 'Ganho de Massa', MAINTENANCE: 'Manutenção', DIETARY_REEDUCATION: 'Reeducação Alimentar', SPORTS_PERFORMANCE: 'Performance Esportiva' }
  return m[v] ?? v
}
function labelGender(v: string) { return v === 'MALE' ? 'Masculino' : 'Feminino' }

// Retorna true se sabemos com certeza que o perfil não existe (não está ainda carregando)
const perfilAusente = () => perfil.value === 'not-found'
// Retorna true se triagem é um objeto real (existe)
const triagemExiste = () => typeof triagem.value === 'object' && triagem.value !== null
</script>

<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-card">

      <!-- Cabeçalho -->
      <header class="modal-header">
        <div class="modal-user-info">
          <div class="modal-avatar">{{ user.name.charAt(0).toUpperCase() }}</div>
          <div>
            <h2>{{ user.name }}</h2>
            <p>{{ user.email }}</p>
          </div>
        </div>
        <button class="btn-fechar" @click="emit('close')">✕</button>
      </header>

      <!-- Abas -->
      <nav class="modal-tabs">
        <button v-for="tab in (['dados','perfil','triagem','acoes'] as Tab[])" :key="tab"
          class="tab-btn" :class="{ active: abaAtiva === tab }" @click="abaAtiva = tab">
          {{ tab === 'dados' ? 'Dados' : tab === 'perfil' ? 'Perfil' : tab === 'triagem' ? 'Triagem' : 'Ações' }}
        </button>
      </nav>

      <!-- Mensagens globais da modal -->
      <div v-if="sucesso" class="msg sucesso">{{ sucesso }} <button @click="sucesso = null">✕</button></div>
      <div v-if="erro" class="msg erro">{{ erro }} <button @click="erro = null">✕</button></div>

      <div class="modal-body">

        <!-- ── ABA DADOS ── -->
        <section v-if="abaAtiva === 'dados'" class="aba">
          <div class="field-row">
            <div class="field">
              <label>Nome</label>
              <input v-model="formDados.name" type="text" placeholder="Nome completo" />
            </div>
            <div class="field">
              <label>E-mail</label>
              <input v-model="formDados.email" type="email" placeholder="email@exemplo.com" />
            </div>
          </div>
          <div class="field-row">
            <div class="field">
              <label>Tipo</label>
              <select v-model="formDados.type">
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>
            <div class="field">
              <label>Status</label>
              <select v-model="formDados.status">
                <option value="ACTIVE">ACTIVE</option>
                <option value="BLOCKED">BLOCKED</option>
                <option value="BANNED">BANNED</option>
              </select>
            </div>
          </div>
          <div class="aba-footer">
            <button class="btn-salvar" :disabled="loadingDados" @click="salvarDados">
              {{ loadingDados ? 'Salvando…' : 'Salvar Dados' }}
            </button>
          </div>
        </section>

        <!-- ── ABA PERFIL ── -->
        <section v-else-if="abaAtiva === 'perfil'" class="aba">

          <div v-if="perfil === 'loading'" class="estado-centro">
            <div class="spinner"></div><p>Carregando perfil…</p>
          </div>

          <div v-else-if="perfil === 'error'" class="estado-centro">
            <p class="estado-dim">Erro ao carregar perfil.</p>
            <button class="btn-outline-sm" @click="carregarPerfil">Tentar novamente</button>
          </div>

          <!-- Sem perfil -->
          <div v-else-if="perfil === 'not-found'">
            <div v-if="!modoNovoPerfil" class="estado-centro">
              <p class="estado-dim">Usuário não possui perfil nutricional.</p>
              <button class="btn-outline-sm" @click="iniciarNovoPerfil">+ Criar Perfil</button>
            </div>
            <div v-else>
              <h4 class="aba-subtitulo">Novo Perfil</h4>
              <div class="field-row">
                <div class="field">
                  <label>Data de Nascimento</label>
                  <input v-model="formPerfil.dateOfBirth" type="date" />
                </div>
                <div class="field">
                  <label>Gênero</label>
                  <select v-model="formPerfil.gender">
                    <option value="MALE">Masculino</option>
                    <option value="FEMALE">Feminino</option>
                  </select>
                </div>
              </div>
              <div class="field-row">
                <div class="field">
                  <label>Peso (kg)</label>
                  <input v-model="formPerfil.weightKg" type="number" step="0.1" placeholder="ex: 70.5" />
                </div>
                <div class="field">
                  <label>Altura (cm)</label>
                  <input v-model="formPerfil.heightCm" type="number" step="0.1" placeholder="ex: 175" />
                </div>
              </div>
              <div class="field">
                <label>Nível de Atividade</label>
                <select v-model="formPerfil.activityLevel">
                  <option value="SEDENTARY">Sedentário</option>
                  <option value="MODERATE">Moderado</option>
                  <option value="INTENSE">Intenso</option>
                  <option value="VERY_INTENSE">Muito Intenso</option>
                </select>
              </div>
              <div class="aba-footer">
                <button class="btn-cancelar" @click="modoNovoPerfil = false">Cancelar</button>
                <button class="btn-salvar" :disabled="loadingPerfil" @click="criarPerfil">
                  {{ loadingPerfil ? 'Criando…' : 'Criar Perfil' }}
                </button>
              </div>
            </div>
          </div>

          <!-- Perfil existente — visualização -->
          <div v-else-if="!modoEdicaoPerfil">
            <div class="info-grid">
              <div class="info-item"><span>Data de Nascimento</span><strong>{{ (perfil as ProfileData).dateOfBirth?.slice(0,10) ?? '—' }}</strong></div>
              <div class="info-item"><span>Gênero</span><strong>{{ labelGender((perfil as ProfileData).gender) }}</strong></div>
              <div class="info-item"><span>Peso</span><strong>{{ (perfil as ProfileData).weightKg }} kg</strong></div>
              <div class="info-item"><span>Altura</span><strong>{{ (perfil as ProfileData).heightCm }} cm</strong></div>
              <div class="info-item"><span>Nível de Atividade</span><strong>{{ labelActivityLevel((perfil as ProfileData).activityLevel) }}</strong></div>
            </div>
            <div class="aba-footer">
              <button class="btn-danger-outline" @click="solicitarExclusaoPerfil">Excluir Perfil</button>
              <button class="btn-salvar" @click="modoEdicaoPerfil = true">Editar</button>
            </div>
          </div>

          <!-- Perfil existente — edição -->
          <div v-else>
            <h4 class="aba-subtitulo">Editar Perfil</h4>
            <div class="field-row">
              <div class="field">
                <label>Data de Nascimento</label>
                <input v-model="formPerfil.dateOfBirth" type="date" />
              </div>
              <div class="field">
                <label>Gênero</label>
                <select v-model="formPerfil.gender">
                  <option value="MALE">Masculino</option>
                  <option value="FEMALE">Feminino</option>
                </select>
              </div>
            </div>
            <div class="field-row">
              <div class="field">
                <label>Peso (kg)</label>
                <input v-model="formPerfil.weightKg" type="number" step="0.1" />
              </div>
              <div class="field">
                <label>Altura (cm)</label>
                <input v-model="formPerfil.heightCm" type="number" step="0.1" />
              </div>
            </div>
            <div class="field">
              <label>Nível de Atividade</label>
              <select v-model="formPerfil.activityLevel">
                <option value="SEDENTARY">Sedentário</option>
                <option value="MODERATE">Moderado</option>
                <option value="INTENSE">Intenso</option>
                <option value="VERY_INTENSE">Muito Intenso</option>
              </select>
            </div>
            <div class="aba-footer">
              <button class="btn-cancelar" @click="modoEdicaoPerfil = false">Cancelar</button>
              <button class="btn-salvar" :disabled="loadingPerfil" @click="salvarPerfil">
                {{ loadingPerfil ? 'Salvando…' : 'Salvar Perfil' }}
              </button>
            </div>
          </div>

          <!-- Confirmação de exclusão de perfil (simples ou com cascata) -->
          <div v-if="confirmExcluirPerfil" class="confirm-inline">
            <!-- Verificando triagem: estado de espera -->
            <div v-if="triagem === 'loading'" class="confirm-verificando">
              <div class="spinner-sm"></div>
              <span>Verificando triagem vinculada…</span>
            </div>
            <!-- Com triagem: aviso de exclusão conjunta -->
            <template v-else-if="triagemExiste()">
              <p class="confirm-aviso-conjunto">
                Atenção: este usuário também possui uma triagem vinculada ao perfil.<br>
                <strong>Confirmar excluirá o perfil e a triagem em conjunto.</strong>
              </p>
              <div class="confirm-btns">
                <button class="btn-cancelar" @click="confirmExcluirPerfil = false">Cancelar</button>
                <button class="btn-danger" @click="confirmarExcluirPerfil">Excluir Perfil e Triagem</button>
              </div>
            </template>
            <!-- Sem triagem: confirmação normal -->
            <template v-else>
              <p>Confirma exclusão do perfil nutricional?</p>
              <div class="confirm-btns">
                <button class="btn-cancelar" @click="confirmExcluirPerfil = false">Cancelar</button>
                <button class="btn-danger" @click="confirmarExcluirPerfil">Excluir Perfil</button>
              </div>
            </template>
          </div>
        </section>

        <!-- ── ABA TRIAGEM ── -->
        <section v-else-if="abaAtiva === 'triagem'" class="aba">

          <div v-if="triagem === 'loading'" class="estado-centro">
            <div class="spinner"></div><p>Carregando triagem…</p>
          </div>

          <div v-else-if="triagem === 'error'" class="estado-centro">
            <p class="estado-dim">Erro ao carregar triagem.</p>
            <button class="btn-outline-sm" @click="carregarTriagem">Tentar novamente</button>
          </div>

          <!-- Sem triagem -->
          <div v-else-if="triagem === 'not-found'">
            <!-- Sem perfil: bloqueado -->
            <div v-if="perfilAusente()" class="estado-centro">
              <p class="estado-dim aviso-perfil">Crie o perfil nutricional antes de cadastrar a triagem.</p>
              <button class="btn-outline-sm" @click="abaAtiva = 'perfil'">Ir para Perfil</button>
            </div>
            <!-- Com perfil ou ainda carregando: permitir criar -->
            <div v-else-if="!modoNovaTriagem" class="estado-centro">
              <p class="estado-dim">Usuário não possui triagem.</p>
              <button class="btn-outline-sm" :disabled="perfil === 'loading'" @click="iniciarNovaTriagem">+ Criar Triagem</button>
            </div>
            <div v-else>
              <h4 class="aba-subtitulo">Nova Triagem</h4>
              <div class="field">
                <label>Objetivo</label>
                <select v-model="formTriagem.goal">
                  <option value="WEIGHT_LOSS">Perda de Peso</option>
                  <option value="MUSCLE_GAIN">Ganho de Massa</option>
                  <option value="MAINTENANCE">Manutenção</option>
                  <option value="DIETARY_REEDUCATION">Reeducação Alimentar</option>
                  <option value="SPORTS_PERFORMANCE">Performance Esportiva</option>
                </select>
              </div>
              <div class="field-row">
                <div class="field">
                  <label>Refeições / dia</label>
                  <input v-model.number="formTriagem.mealsPerDay" type="number" min="1" max="10" placeholder="ex: 5" />
                </div>
                <div class="field">
                  <label>Peso alvo (kg) <span class="label-opt">opcional</span></label>
                  <input v-model.number="formTriagem.targetWeightKg" type="number" step="0.1" placeholder="ex: 70" />
                </div>
              </div>
              <div class="field">
                <label>Restrições alimentares <span class="label-opt">opcional</span></label>
                <input v-model="formTriagem.dietaryRestrictions" type="text" placeholder="ex: sem glúten" />
              </div>
              <div class="field">
                <label>Alergias <span class="label-opt">opcional</span></label>
                <input v-model="formTriagem.allergies" type="text" placeholder="ex: amendoim" />
              </div>
              <div class="field">
                <label>Condições de saúde <span class="label-opt">opcional</span></label>
                <input v-model="formTriagem.healthConditions" type="text" placeholder="ex: diabetes" />
              </div>
              <div class="aba-footer">
                <button class="btn-cancelar" @click="modoNovaTriagem = false">Cancelar</button>
                <button class="btn-salvar" :disabled="loadingTriagem" @click="criarTriagem">
                  {{ loadingTriagem ? 'Criando…' : 'Criar Triagem' }}
                </button>
              </div>
            </div>
          </div>

          <!-- Triagem existente — visualização -->
          <div v-else-if="!modoEdicaoTriagem">
            <div class="info-grid">
              <div class="info-item"><span>Objetivo</span><strong>{{ labelGoal((triagem as AssessmentData).goal) }}</strong></div>
              <div class="info-item"><span>Refeições / dia</span><strong>{{ (triagem as AssessmentData).mealsPerDay ?? '—' }}</strong></div>
              <div class="info-item"><span>Peso alvo</span><strong>{{ (triagem as AssessmentData).targetWeightKg ? (triagem as AssessmentData).targetWeightKg + ' kg' : '—' }}</strong></div>
              <div class="info-item"><span>Restrições</span><strong>{{ (triagem as AssessmentData).dietaryRestrictions || '—' }}</strong></div>
              <div class="info-item"><span>Alergias</span><strong>{{ (triagem as AssessmentData).allergies || '—' }}</strong></div>
              <div class="info-item"><span>Condições de saúde</span><strong>{{ (triagem as AssessmentData).healthConditions || '—' }}</strong></div>
            </div>
            <div class="aba-footer">
              <button class="btn-danger-outline" @click="confirmExcluirTriagem = true">Excluir Triagem</button>
              <button class="btn-salvar" @click="modoEdicaoTriagem = true">Editar</button>
            </div>
          </div>

          <!-- Triagem existente — edição -->
          <div v-else>
            <h4 class="aba-subtitulo">Editar Triagem</h4>
            <div class="field">
              <label>Objetivo</label>
              <select v-model="formTriagem.goal">
                <option value="WEIGHT_LOSS">Perda de Peso</option>
                <option value="MUSCLE_GAIN">Ganho de Massa</option>
                <option value="MAINTENANCE">Manutenção</option>
                <option value="DIETARY_REEDUCATION">Reeducação Alimentar</option>
                <option value="SPORTS_PERFORMANCE">Performance Esportiva</option>
              </select>
            </div>
            <div class="field-row">
              <div class="field">
                <label>Refeições / dia</label>
                <input v-model.number="formTriagem.mealsPerDay" type="number" min="1" max="10" />
              </div>
              <div class="field">
                <label>Peso alvo (kg)</label>
                <input v-model.number="formTriagem.targetWeightKg" type="number" step="0.1" />
              </div>
            </div>
            <div class="field">
              <label>Restrições alimentares</label>
              <input v-model="formTriagem.dietaryRestrictions" type="text" />
            </div>
            <div class="field">
              <label>Alergias</label>
              <input v-model="formTriagem.allergies" type="text" />
            </div>
            <div class="field">
              <label>Condições de saúde</label>
              <input v-model="formTriagem.healthConditions" type="text" />
            </div>
            <div class="aba-footer">
              <button class="btn-cancelar" @click="modoEdicaoTriagem = false">Cancelar</button>
              <button class="btn-salvar" :disabled="loadingTriagem" @click="salvarTriagem">
                {{ loadingTriagem ? 'Salvando…' : 'Salvar Triagem' }}
              </button>
            </div>
          </div>

          <div v-if="confirmExcluirTriagem" class="confirm-inline">
            <p>Confirma exclusão da triagem?</p>
            <div class="confirm-btns">
              <button class="btn-cancelar" @click="confirmExcluirTriagem = false">Cancelar</button>
              <button class="btn-danger" @click="confirmarExcluirTriagem">Excluir</button>
            </div>
          </div>
        </section>

        <!-- ── ABA AÇÕES ── -->
        <section v-else-if="abaAtiva === 'acoes'" class="aba">
          <div class="acoes-lista">

            <div class="acao-card">
              <div>
                <h4>Revogar Sessões</h4>
                <p>Desconecta o usuário de todos os dispositivos imediatamente.</p>
              </div>
              <button class="btn-acao" :disabled="loadingAcao === 'sessions'" @click="revogarSessoes">
                {{ loadingAcao === 'sessions' ? 'Revogando…' : 'Revogar Sessões' }}
              </button>
            </div>

            <div class="acao-card acao-danger">
              <div>
                <h4>Deletar Usuário</h4>
                <p>Remove permanentemente o usuário e todos os seus dados. Esta ação não pode ser desfeita.</p>
              </div>
              <button class="btn-acao danger" :disabled="loadingAcao === 'delete'" @click="confirmDeletarUsuario = true">
                {{ loadingAcao === 'delete' ? 'Deletando…' : 'Deletar Usuário' }}
              </button>
            </div>

          </div>

          <div v-if="confirmDeletarUsuario" class="confirm-inline danger">
            <p>Tem certeza? O usuário <strong>{{ user.name }}</strong> e todos os seus dados serão deletados permanentemente.</p>
            <div class="confirm-btns">
              <button class="btn-cancelar" @click="confirmDeletarUsuario = false">Cancelar</button>
              <button class="btn-danger" @click="confirmarDeletarUsuario">Deletar Definitivamente</button>
            </div>
          </div>
        </section>

      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.75);
  display: flex; align-items: center; justify-content: center;
  z-index: 300; padding: 1rem;
}

.modal-card {
  --amber: #f59e0b; --amber-dim: rgba(245,158,11,0.12); --amber-border: rgba(245,158,11,0.3);
  --bg: #0f172a; --card: #1e293b; --text-dim: #94a3b8;
  background: #1a2540; border: 1px solid var(--amber-border);
  border-top: 3px solid var(--amber);
  border-radius: 20px; width: 100%; max-width: 680px;
  max-height: 90vh; display: flex; flex-direction: column;
  box-shadow: 0 0 40px rgba(245,158,11,0.08);
  color: white; font-family: 'Inter', sans-serif;
}

/* Header */
.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1.5rem 2rem; border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}
.modal-user-info { display: flex; align-items: center; gap: 1rem; }
.modal-avatar {
  width: 44px; height: 44px; border-radius: 50%;
  background: var(--amber-dim); border: 1px solid var(--amber-border);
  color: var(--amber); font-weight: 800; font-size: 1.1rem;
  display: flex; align-items: center; justify-content: center;
}
.modal-header h2 { font-size: 1.1rem; font-weight: 800; margin: 0; }
.modal-header p { font-size: 0.8rem; color: var(--text-dim); margin: 0; }
.btn-fechar { background: transparent; border: none; color: var(--text-dim); cursor: pointer; font-size: 1.1rem; padding: 4px 8px; }
.btn-fechar:hover { color: white; }

/* Tabs */
.modal-tabs {
  display: flex; gap: 0; border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0; padding: 0 2rem;
}
.tab-btn {
  background: transparent; border: none; color: var(--text-dim);
  padding: 0.9rem 1.2rem; font-size: 0.85rem; font-weight: 700; cursor: pointer;
  border-bottom: 2px solid transparent; transition: all 0.15s;
}
.tab-btn:hover { color: white; }
.tab-btn.active { color: var(--amber); border-bottom-color: var(--amber); }

/* Mensagens */
.msg {
  margin: 0.75rem 2rem 0;
  padding: 0.7rem 1rem; border-radius: 10px; font-size: 0.85rem;
  display: flex; justify-content: space-between; align-items: center;
  flex-shrink: 0;
}
.msg.sucesso { background: rgba(16,185,129,0.1); border: 1px solid rgba(16,185,129,0.25); color: #6ee7b7; }
.msg.erro { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.25); color: #fca5a5; }
.msg button { background: transparent; border: none; color: inherit; cursor: pointer; font-size: 0.9rem; }

/* Body */
.modal-body { overflow-y: auto; flex: 1; padding: 1.5rem 2rem 2rem; }

.aba { display: flex; flex-direction: column; gap: 1.2rem; }
.aba-subtitulo { font-size: 0.95rem; font-weight: 800; color: var(--amber); margin: 0 0 0.5rem; }

/* Fields */
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 0.78rem; font-weight: 700; color: var(--text-dim); text-transform: uppercase; letter-spacing: 0.5px; }
.label-opt { font-weight: 400; text-transform: none; letter-spacing: 0; }
.field input, .field select {
  background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1);
  color: white; padding: 0.65rem 0.9rem; border-radius: 10px; font-size: 0.9rem;
  outline: none; transition: border-color 0.15s;
}
.field input:focus, .field select:focus { border-color: var(--amber); }
.field select option { background: #1e293b; }

/* Info grid */
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.info-item {
  background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06);
  border-radius: 10px; padding: 0.75rem 1rem;
}
.info-item span { display: block; font-size: 0.72rem; color: var(--text-dim); font-weight: 700; text-transform: uppercase; margin-bottom: 4px; }
.info-item strong { font-size: 0.9rem; }

/* Footer das abas */
.aba-footer { display: flex; justify-content: flex-end; gap: 0.75rem; padding-top: 0.5rem; }

/* Botões */
.btn-salvar {
  background: var(--amber); color: #0f172a; border: none;
  padding: 0.65rem 1.5rem; border-radius: 10px; font-weight: 800; font-size: 0.85rem; cursor: pointer;
}
.btn-salvar:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-cancelar { background: rgba(255,255,255,0.06); border: none; color: var(--text-dim); padding: 0.65rem 1.2rem; border-radius: 10px; font-weight: 700; font-size: 0.85rem; cursor: pointer; }
.btn-outline-sm { background: transparent; border: 1px solid rgba(255,255,255,0.15); color: var(--text-dim); padding: 0.55rem 1.2rem; border-radius: 10px; font-weight: 700; font-size: 0.82rem; cursor: pointer; }
.btn-outline-sm:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-danger-outline { background: transparent; border: 1px solid rgba(239,68,68,0.3); color: #f87171; padding: 0.65rem 1.2rem; border-radius: 10px; font-weight: 700; font-size: 0.85rem; cursor: pointer; }
.btn-danger { background: rgba(239,68,68,0.15); border: 1px solid rgba(239,68,68,0.4); color: #f87171; padding: 0.65rem 1.2rem; border-radius: 10px; font-weight: 800; font-size: 0.85rem; cursor: pointer; }

/* Estados vazios */
.estado-centro { display: flex; flex-direction: column; align-items: center; gap: 1rem; padding: 3rem 0; color: var(--text-dim); }
.estado-dim { font-size: 0.9rem; text-align: center; }
.aviso-perfil { color: #fcd34d; }
.spinner { width: 24px; height: 24px; border: 3px solid rgba(255,255,255,0.1); border-top-color: var(--amber); border-radius: 50%; animation: spin 0.7s linear infinite; }
.spinner-sm { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.1); border-top-color: #fca5a5; border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Confirmação inline */
.confirm-inline {
  background: rgba(239,68,68,0.07); border: 1px solid rgba(239,68,68,0.25);
  border-radius: 12px; padding: 1rem 1.2rem; margin-top: 1rem;
}
.confirm-inline p { font-size: 0.88rem; color: #fca5a5; margin: 0 0 0.75rem; }
.confirm-inline strong { color: white; }
.confirm-aviso-conjunto { color: #fcd34d !important; line-height: 1.5; }
.confirm-verificando { display: flex; align-items: center; gap: 0.6rem; color: #fca5a5; font-size: 0.85rem; padding: 0.5rem 0; }
.confirm-btns { display: flex; gap: 0.75rem; justify-content: flex-end; }

/* Aba Ações */
.acoes-lista { display: flex; flex-direction: column; gap: 1rem; }
.acao-card {
  background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.07);
  border-radius: 14px; padding: 1.2rem 1.5rem;
  display: flex; justify-content: space-between; align-items: center; gap: 1.5rem;
}
.acao-card h4 { font-size: 0.95rem; font-weight: 800; margin: 0 0 4px; }
.acao-card p { font-size: 0.8rem; color: var(--text-dim); margin: 0; }
.acao-card.acao-danger { border-color: rgba(239,68,68,0.2); }
.btn-acao { background: rgba(255,255,255,0.06); border: none; color: white; padding: 0.6rem 1.2rem; border-radius: 10px; font-weight: 700; font-size: 0.82rem; cursor: pointer; white-space: nowrap; }
.btn-acao:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-acao.danger { color: #f87171; background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.25); }

@media (max-width: 600px) {
  .field-row { grid-template-columns: 1fr; }
  .info-grid { grid-template-columns: 1fr; }
  .modal-card { max-height: 95vh; }
  .acao-card { flex-direction: column; align-items: flex-start; }
}
</style>
