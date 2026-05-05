<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { adminUserService } from '../../services/modules/adminUser'
import AdminUserModal from './AdminUserModal.vue'

interface AdminUser {
  id: number
  name: string
  email: string
  type: 'ADMIN' | 'USER'
  status: 'ACTIVE' | 'BLOCKED' | 'BANNED'
}

interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

const router = useRouter()
const authStore = useAuthStore()

const usuarios = ref<AdminUser[]>([])
const totalElements = ref(0)
const currentPage = ref(0)
const pageSize = 20

const loading = ref(false)
const erro = ref<string | null>(null)

const loadingAcao = ref<number | null>(null)
const sucesso = ref<string | null>(null)

const confirmDelete = ref<AdminUser | null>(null)
const usuarioSelecionado = ref<AdminUser | null>(null)

const totalAtivos = computed(() => usuarios.value.filter(u => u.status === 'ACTIVE').length)
const totalBloqueados = computed(() => usuarios.value.filter(u => u.status !== 'ACTIVE').length)

async function carregarUsuarios(page = 0) {
  loading.value = true
  erro.value = null
  try {
    const data: Page<AdminUser> = await adminUserService.findAll(page, pageSize)
    usuarios.value = data.content
    totalElements.value = data.totalElements
    currentPage.value = data.number
  } catch (err: any) {
    erro.value = err.response?.data?.message ?? 'Erro ao carregar usuários.'
  } finally {
    loading.value = false
  }
}

async function alternarStatus(user: AdminUser) {
  const novoStatus = user.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE'
  loadingAcao.value = user.id
  try {
    await adminUserService.update(user.id, { status: novoStatus })
    user.status = novoStatus
  } catch (err: any) {
    erro.value = err.response?.data?.message ?? 'Erro ao atualizar status.'
  } finally {
    loadingAcao.value = null
  }
}

async function revogarSessoes(user: AdminUser) {
  loadingAcao.value = user.id
  erro.value = null
  sucesso.value = null
  try {
    await adminUserService.revokeSessions(user.id)
    sucesso.value = `Sessões de ${user.name} revogadas com sucesso.`
  } catch (err: any) {
    erro.value = err.response?.data?.message ?? 'Erro ao revogar sessões.'
  } finally {
    loadingAcao.value = null
  }
}

async function confirmarDelete() {
  const user = confirmDelete.value
  if (!user) return
  loadingAcao.value = user.id
  confirmDelete.value = null
  try {
    await adminUserService.delete(user.id)
    usuarios.value = usuarios.value.filter(u => u.id !== user.id)
    totalElements.value -= 1
  } catch (err: any) {
    erro.value = err.response?.data?.message ?? 'Erro ao deletar usuário.'
  } finally {
    loadingAcao.value = null
  }
}

function statusLabel(status: string) {
  if (status === 'ACTIVE') return 'Ativo'
  if (status === 'BLOCKED') return 'Bloqueado'
  if (status === 'BANNED') return 'Banido'
  return status
}

function statusClass(status: string) {
  if (status === 'ACTIVE') return 'ativo'
  if (status === 'BLOCKED') return 'bloqueado'
  return 'banido'
}

function onUsuarioAtualizado(updated: AdminUser) {
  const idx = usuarios.value.findIndex(u => u.id === updated.id)
  if (idx !== -1) usuarios.value[idx] = updated
  usuarioSelecionado.value = updated
}

function onUsuarioDeletado(userId: number) {
  usuarios.value = usuarios.value.filter(u => u.id !== userId)
  totalElements.value -= 1
  usuarioSelecionado.value = null
}

const voltar = () => router.push('/dashboard')

onMounted(() => carregarUsuarios(0))
</script>

<template>
  <div class="admin-wrapper">
    <nav class="admin-nav">
      <div class="nav-content">
        <button class="btn-back" @click="voltar">
          <span class="chevron-left"></span> Dashboard Principal
        </button>
        <span class="admin-tag">Painel Administrativo</span>
      </div>
    </nav>

    <main class="admin-container">

      <!-- Estatísticas calculadas dos dados reais -->
      <section class="stats-grid">
        <article class="stat-card">
          <label>Total de Usuários</label>
          <p>{{ loading ? '—' : totalElements }}</p>
        </article>
        <article class="stat-card">
          <label>Ativos exibidos</label>
          <p>{{ loading ? '—' : totalAtivos }}</p>
        </article>
        <article class="stat-card">
          <label>Bloqueados / Banidos exibidos</label>
          <p>{{ loading ? '—' : totalBloqueados }}</p>
        </article>
      </section>

      <!-- Sucesso -->
      <div v-if="sucesso" class="sucesso-banner">
        {{ sucesso }}
        <button class="erro-fechar" @click="sucesso = null">✕</button>
      </div>

      <!-- Erro global -->
      <div v-if="erro" class="erro-banner">
        {{ erro }}
        <button class="erro-fechar" @click="erro = null">✕</button>
      </div>

      <!-- Tabela de usuários -->
      <section class="users-section">
        <div class="card-header">
          <h3>Controle de Acessos</h3>
          <p>Gerencie permissões e sessões dos usuários</p>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="estado-centro">
          <div class="spinner"></div>
          <p>Carregando usuários…</p>
        </div>

        <!-- Vazio -->
        <div v-else-if="!erro && usuarios.length === 0" class="estado-centro">
          <p class="estado-texto">Nenhum usuário encontrado.</p>
        </div>

        <!-- Tabela -->
        <div v-else class="table-container">
          <table class="user-table">
            <thead>
              <tr>
                <th>Nome / Email</th>
                <th>Tipo</th>
                <th>Status</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in usuarios" :key="user.id" :class="{ 'row-loading': loadingAcao === user.id }">
                <td>
                  <div class="user-cell user-cell-link" @click="usuarioSelecionado = user">
                    <strong>{{ user.name }}</strong>
                    <span>{{ user.email }}</span>
                  </div>
                </td>
                <td>
                  <span class="type-pill" :class="user.type.toLowerCase()">{{ user.type }}</span>
                </td>
                <td>
                  <span class="status-pill" :class="statusClass(user.status)">
                    {{ statusLabel(user.status) }}
                  </span>
                </td>
                <td>
                  <div class="action-btns">
                    <button
                      :disabled="loadingAcao === user.id || user.status === 'BANNED'"
                      @click="alternarStatus(user)"
                      class="btn-table danger"
                    >
                      {{ user.status === 'ACTIVE' ? 'Bloquear' : 'Desbloquear' }}
                    </button>
                    <button
                      :disabled="loadingAcao === user.id"
                      @click="revogarSessoes(user)"
                      class="btn-table"
                    >
                      Revogar Sessões
                    </button>
                    <button
                      :disabled="loadingAcao === user.id"
                      @click="confirmDelete = user"
                      class="btn-table delete"
                    >
                      Deletar
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- Paginação -->
          <div v-if="totalElements > pageSize" class="pagination">
            <button
              :disabled="currentPage === 0"
              @click="carregarUsuarios(currentPage - 1)"
              class="btn-page"
            >← Anterior</button>
            <span class="page-info">Página {{ currentPage + 1 }}</span>
            <button
              :disabled="(currentPage + 1) * pageSize >= totalElements"
              @click="carregarUsuarios(currentPage + 1)"
              class="btn-page"
            >Próxima →</button>
          </div>
        </div>
      </section>

    </main>

    <!-- Modal de gestão do usuário -->
    <AdminUserModal
      v-if="usuarioSelecionado"
      :user="usuarioSelecionado"
      @close="usuarioSelecionado = null"
      @updated="onUsuarioAtualizado"
      @deleted="onUsuarioDeletado"
    />

    <!-- Modal de confirmação de delete -->
    <div v-if="confirmDelete" class="modal-overlay" @click.self="confirmDelete = null">
      <div class="modal-card">
        <h3>Confirmar exclusão</h3>
        <p>
          Tem certeza que deseja deletar permanentemente o usuário
          <strong>{{ confirmDelete.name }}</strong> ({{ confirmDelete.email }})?
          Esta ação não pode ser desfeita.
        </p>
        <div class="modal-actions">
          <button class="btn-modal-cancel" @click="confirmDelete = null">Cancelar</button>
          <button class="btn-modal-confirm" @click="confirmarDelete">Deletar</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-wrapper {
  --bg: #0f172a; --card: #1e293b; --emerald: #10b981; --text-dim: #94a3b8;
  --amber: #f59e0b; --amber-dim: rgba(245, 158, 11, 0.15); --amber-border: rgba(245, 158, 11, 0.35);
  min-height: 100vh; background-color: var(--bg); color: white; font-family: 'Inter', sans-serif;
  border-left: 3px solid var(--amber);
}

/* NAVBAR */
.admin-nav {
  background: rgba(15, 23, 42, 0.95); backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--amber-border);
  box-shadow: 0 1px 20px rgba(245, 158, 11, 0.08);
  position: sticky; top: 0; z-index: 100;
}
.nav-content { max-width: 1200px; margin: 0 auto; padding: 1.2rem 1.5rem; display: flex; justify-content: space-between; align-items: center; }
.btn-back { background: transparent; border: none; color: var(--text-dim); cursor: pointer; font-weight: 600; display: flex; align-items: center; gap: 10px; }
.btn-back:hover { color: var(--amber); }
.chevron-left { width: 7px; height: 7px; border-left: 2px solid currentColor; border-bottom: 2px solid currentColor; transform: rotate(45deg); }
.admin-tag {
  font-weight: 800; text-transform: uppercase; letter-spacing: 2px; font-size: 0.7rem;
  color: var(--amber);
  background: var(--amber-dim);
  border: 1px solid var(--amber-border);
  padding: 4px 12px; border-radius: 20px;
}

/* CONTENT */
.admin-container { max-width: 1200px; margin: 0 auto; padding: 3rem 1.5rem; }

/* STATS */
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.5rem; margin-bottom: 2.5rem; }
.stat-card { background: var(--card); padding: 2rem; border-radius: 20px; border: 1px solid rgba(255,255,255,0.03); border-top: 2px solid var(--amber-border); }
.stat-card label { display: block; font-size: 0.75rem; font-weight: 800; text-transform: uppercase; color: var(--text-dim); margin-bottom: 0.5rem; }
.stat-card p { font-size: 2.2rem; font-weight: 900; letter-spacing: -1px; color: var(--amber); }

/* ERRO */
.sucesso-banner { background: rgba(16, 185, 129, 0.1); border: 1px solid rgba(16, 185, 129, 0.3); color: #6ee7b7; padding: 1rem 1.5rem; border-radius: 12px; margin-bottom: 1rem; display: flex; justify-content: space-between; align-items: center; }
.erro-banner { background: rgba(239, 68, 68, 0.12); border: 1px solid rgba(239, 68, 68, 0.3); color: #fca5a5; padding: 1rem 1.5rem; border-radius: 12px; margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: center; }
.erro-fechar { background: transparent; border: none; color: inherit; cursor: pointer; font-size: 1rem; }

/* USERS */
.users-section { background: var(--card); border-radius: 24px; padding: 2.5rem; border: 1px solid rgba(255,255,255,0.05); }
.card-header { margin-bottom: 2.5rem; }
.card-header h3 { font-size: 1.4rem; font-weight: 800; }
.card-header p { color: var(--text-dim); font-size: 0.9rem; }

/* ESTADOS */
.estado-centro { display: flex; flex-direction: column; align-items: center; padding: 4rem 0; gap: 1rem; color: var(--text-dim); }
.estado-texto { font-size: 0.95rem; }
.spinner { width: 28px; height: 28px; border: 3px solid rgba(255,255,255,0.1); border-top-color: var(--emerald); border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* TABLE */
.table-container { overflow-x: auto; }
.user-table { width: 100%; border-collapse: collapse; text-align: left; }
.user-table th { padding: 1rem; color: var(--text-dim); font-size: 0.75rem; text-transform: uppercase; font-weight: 800; border-bottom: 1px solid rgba(255,255,255,0.05); }
.user-table td { padding: 1.2rem 1rem; border-bottom: 1px solid rgba(255,255,255,0.03); vertical-align: middle; }
.row-loading { opacity: 0.5; pointer-events: none; }

.user-cell { display: flex; flex-direction: column; }
.user-cell span { font-size: 0.8rem; color: var(--text-dim); }
.user-cell-link { cursor: pointer; }
.user-cell-link:hover strong { color: #f59e0b; }
.user-cell-link:hover span { color: #d1d5db; }

.status-pill { padding: 4px 12px; border-radius: 20px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; }
.status-pill.ativo { background: rgba(16, 185, 129, 0.1); color: var(--emerald); }
.status-pill.bloqueado { background: rgba(239, 68, 68, 0.1); color: #f87171; }
.status-pill.banido { background: rgba(239, 68, 68, 0.2); color: #ef4444; }

.type-pill { padding: 3px 10px; border-radius: 20px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; }
.type-pill.admin { background: rgba(245, 158, 11, 0.15); color: #f59e0b; }
.type-pill.user { background: rgba(148, 163, 184, 0.1); color: var(--text-dim); }

.action-btns { display: flex; gap: 8px; flex-wrap: wrap; }
.btn-table { background: rgba(255,255,255,0.05); border: none; color: white; padding: 6px 12px; border-radius: 8px; font-size: 0.75rem; font-weight: 700; cursor: pointer; transition: background 0.15s; }
.btn-table:hover:not(:disabled) { background: rgba(255,255,255,0.1); }
.btn-table:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-table.danger { color: #f87171; }
.btn-table.danger:hover:not(:disabled) { background: rgba(239, 68, 68, 0.1); }
.btn-table.delete { color: #ef4444; }
.btn-table.delete:hover:not(:disabled) { background: rgba(239, 68, 68, 0.18); }

/* PAGINAÇÃO */
.pagination { display: flex; align-items: center; justify-content: center; gap: 1.5rem; margin-top: 2rem; padding-top: 1.5rem; border-top: 1px solid rgba(255,255,255,0.05); }
.btn-page { background: rgba(255,255,255,0.05); border: none; color: white; padding: 8px 18px; border-radius: 10px; font-weight: 700; cursor: pointer; }
.btn-page:hover:not(:disabled) { background: rgba(255,255,255,0.1); }
.btn-page:disabled { opacity: 0.3; cursor: not-allowed; }
.page-info { font-size: 0.85rem; color: var(--text-dim); }

/* MODAL */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal-card { background: #1e293b; border: 1px solid rgba(255,255,255,0.1); border-radius: 20px; padding: 2.5rem; max-width: 420px; width: 90%; }
.modal-card h3 { font-size: 1.2rem; font-weight: 800; margin-bottom: 1rem; }
.modal-card p { color: var(--text-dim); font-size: 0.9rem; line-height: 1.6; margin-bottom: 2rem; }
.modal-card strong { color: white; }
.modal-actions { display: flex; gap: 1rem; justify-content: flex-end; }
.btn-modal-cancel { background: rgba(255,255,255,0.05); border: none; color: var(--text-dim); padding: 0.7rem 1.5rem; border-radius: 10px; font-weight: 700; cursor: pointer; }
.btn-modal-confirm { background: rgba(239, 68, 68, 0.15); border: 1px solid rgba(239, 68, 68, 0.4); color: #f87171; padding: 0.7rem 1.5rem; border-radius: 10px; font-weight: 800; cursor: pointer; }
.btn-modal-confirm:hover { background: rgba(239, 68, 68, 0.25); }

@media (max-width: 1000px) { .stats-grid { grid-template-columns: 1fr 1fr; } }
@media (max-width: 600px) { .stats-grid { grid-template-columns: 1fr; } .action-btns { flex-direction: column; } }
</style>
