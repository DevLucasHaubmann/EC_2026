<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMeStore } from '@/stores/me'
import UserAvatar from '@/components/UserAvatar.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const meStore = useMeStore()

const links = [
  { name: 'dashboard',    label: 'Dashboard'         },
  { name: 'dieta',        label: 'Minha Dieta'       },
  { name: 'dieta-diaria', label: 'Efetivar Refeição' },
  { name: 'triagem',      label: 'Triagem'            },
  { name: 'evolucao',     label: 'Evolução'           },
  { name: 'historico',    label: 'Histórico'          },
  { name: 'perfil',       label: 'Perfil'             },
]

// When onboarding is complete, the triagem link is hidden: no reason to re-do it.
// While loading (me still null), triagem stays visible so the user is never stuck.
const visibleLinks = computed(() =>
  meStore.onboardingComplete
    ? links.filter((l) => l.name !== 'triagem')
    : links,
)

onMounted(() => meStore.load())

const logout = async () => {
  meStore.clear()
  await authStore.logout()
  router.push({ name: 'auth' })
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar-brand">
      Tukan <span class="brand-dot"></span>
    </div>

    <div v-if="meStore.me" class="sidebar-user">
      <UserAvatar
        :name="meStore.me.name"
        :email="meStore.me.email"
        :avatar-url="meStore.me.avatarUrl"
        :size="36"
      />
      <span class="sidebar-user-name">{{ meStore.me.name }}</span>
    </div>

    <nav class="sidebar-nav">
      <RouterLink
        v-for="link in visibleLinks"
        :key="link.name"
        :to="{ name: link.name }"
        class="nav-link"
        :class="{ active: route.name === link.name }"
      >
        {{ link.label }}
      </RouterLink>
      <RouterLink
        v-if="authStore.isAdmin"
        :to="{ name: 'admin-dashboard' }"
        class="nav-link nav-link-admin"
        :class="{ active: route.name === 'admin-dashboard' }"
      >
        Admin
      </RouterLink>
    </nav>

    <button class="btn-logout" @click="logout">Sair</button>
  </aside>
</template>

<style scoped>
.sidebar {
  --bg-sidebar: #111827;
  --bg-hover: #1e293b;
  --accent: #10b981;
  --text-muted: #64748b;
  --text-main: #f8fafc;

  width: 220px;
  min-height: 100vh;
  background: var(--bg-sidebar);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  padding: 2rem 1.2rem;
  position: sticky;
  top: 0;
  align-self: flex-start;
  height: 100vh;
  flex-shrink: 0;
}

.sidebar-brand {
  font-weight: 900;
  font-size: 1.1rem;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: var(--text-main);
  margin-bottom: 2.5rem;
  display: flex;
  align-items: center;
  gap: 6px;
}

.brand-dot {
  width: 7px;
  height: 7px;
  background: var(--accent);
  border-radius: 50%;
  display: inline-block;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0.6rem 0.5rem;
  margin-bottom: 1rem;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  min-width: 0;
}

.sidebar-user-name {
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.nav-link {
  display: block;
  padding: 0.7rem 1rem;
  border-radius: 10px;
  color: var(--text-muted);
  font-size: 0.9rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.18s ease;
}

.nav-link:hover {
  background: var(--bg-hover);
  color: var(--text-main);
}

.nav-link.active {
  background: rgba(16, 185, 129, 0.12);
  color: var(--accent);
}

.nav-link-admin {
  color: #f59e0b;
  margin-top: 12px;
  border: 1px solid rgba(245, 158, 11, 0.2);
  background: rgba(245, 158, 11, 0.05);
}

.nav-link-admin:hover {
  color: #fcd34d;
  background: rgba(245, 158, 11, 0.12);
  border-color: rgba(245, 158, 11, 0.35);
}

.nav-link-admin.active {
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.45);
  color: #fbbf24;
  box-shadow: 0 0 12px rgba(245, 158, 11, 0.12);
}

.btn-logout {
  background: rgba(248, 113, 113, 0.08);
  color: #f87171;
  border: 1px solid rgba(248, 113, 113, 0.15);
  padding: 0.65rem 1rem;
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.18s ease;
  width: 100%;
  text-align: left;
  margin-top: auto;
}

.btn-logout:hover {
  background: rgba(248, 113, 113, 0.18);
  color: #fca5a5;
}

@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    height: auto;
    min-height: unset;
    position: static;
    flex-direction: row;
    align-items: center;
    padding: 0.8rem 1.2rem;
    gap: 1rem;
    overflow-x: auto;
  }

  .sidebar-brand {
    margin-bottom: 0;
    flex-shrink: 0;
  }

  .sidebar-user {
    display: none;
  }

  .sidebar-nav {
    flex-direction: row;
    flex: 1;
    gap: 4px;
    overflow-x: auto;
  }

  .btn-logout {
    margin-top: 0;
    flex-shrink: 0;
    width: auto;
  }
}
</style>
