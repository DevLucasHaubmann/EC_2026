// @vitest-environment jsdom
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import type { AdminUser, Page } from '@/types/admin'
import type { MeResponse } from '@/types/api'

// ── Mocks ─────────────────────────────────────────────────────────────────────
// The admin dashboard loads the user page on mount and re-fetches on search.
// We mount it and assert against the adminUserService boundary.

vi.mock('@/services/modules/adminUser', () => ({
  adminUserService: {
    findAll: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    revokeSessions: vi.fn(),
  },
}))
vi.mock('@/services/modules/me', () => ({
  meService: { getMe: vi.fn() },
}))
vi.mock('@/composables/useToast', () => ({
  useToast: () => ({ success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() }),
}))
vi.mock('@/composables/useModal', () => ({
  useModal: () => ({ open: vi.fn().mockResolvedValue(false) }),
}))
// DashboardView transitively loads the real router (via AdminUserModal → services →
// api), so keep the actual module and override only the composable we consume.
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return { ...actual, useRouter: () => ({ push: vi.fn() }) }
})
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ logout: vi.fn() }),
}))

import DashboardView from '@/views/admin/DashboardView.vue'
import { adminUserService } from '@/services/modules/adminUser'
import { meService } from '@/services/modules/me'

// ── Factory ───────────────────────────────────────────────────────────────────

function makePage(content: AdminUser[], totalElements = content.length): Page<AdminUser> {
  return { content, totalElements, totalPages: 1, number: 0, size: 20 }
}

// Externally-resolvable promise to control response ordering in race-condition tests.
function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((r) => { resolve = r })
  return { promise, resolve }
}

const ANA: AdminUser = { id: 3, name: 'Ana', email: 'ana@tukan.test', type: 'USER', status: 'ACTIVE' }
const BIA: AdminUser = { id: 4, name: 'Bia', email: 'bia@tukan.test', type: 'USER', status: 'ACTIVE' }

const SAMPLE: AdminUser[] = [
  { id: 1, name: 'Alice', email: 'alice@tukan.test', type: 'USER', status: 'ACTIVE' },
  { id: 2, name: 'Bob', email: 'bob@tukan.test', type: 'ADMIN', status: 'BLOCKED' },
]

const DEBOUNCE_MS = 350

// MeResponse for the admin whose id=1 (matches SAMPLE[0])
function makeMe(id: number): MeResponse {
  return {
    id,
    name: 'Admin',
    email: 'admin@tukan.test',
    type: 'ADMIN',
    status: 'ACTIVE',
    profile: null,
    assessment: null,
  }
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('DashboardView — admin search', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    // Default: meService returns an admin whose id does not collide with SAMPLE.
    vi.mocked(meService.getMe).mockResolvedValue(makeMe(99))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('loads the first unfiltered page on mount', async () => {
    vi.mocked(adminUserService.findAll).mockResolvedValue(makePage(SAMPLE))

    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(adminUserService.findAll).toHaveBeenCalledWith(0, 20, '')
    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('bob@tukan.test')
  })

  it('debounces typing and re-fetches page 0 with the search term', async () => {
    vi.useFakeTimers()
    vi.mocked(adminUserService.findAll).mockResolvedValue(makePage(SAMPLE))

    const wrapper = mount(DashboardView)
    await flushPromises()

    await wrapper.find('input.search-input').setValue('ali')

    // No request until the debounce window elapses.
    expect(adminUserService.findAll).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(DEBOUNCE_MS)
    await flushPromises()

    expect(adminUserService.findAll).toHaveBeenCalledTimes(2)
    expect(adminUserService.findAll).toHaveBeenLastCalledWith(0, 20, 'ali')
  })

  it('shows a search-aware empty message when the term yields no results', async () => {
    vi.useFakeTimers()
    vi.mocked(adminUserService.findAll)
      .mockResolvedValueOnce(makePage(SAMPLE))
      .mockResolvedValueOnce(makePage([], 0))

    const wrapper = mount(DashboardView)
    await flushPromises()

    await wrapper.find('input.search-input').setValue('zzz')
    vi.advanceTimersByTime(DEBOUNCE_MS)
    await flushPromises()

    expect(wrapper.text()).toContain('Nenhum usuário encontrado para "zzz".')
  })

  it('ignores an out-of-order response so the most recent search wins', async () => {
    vi.useFakeTimers()
    const defA = deferred<Page<AdminUser>>()
    const defB = deferred<Page<AdminUser>>()

    vi.mocked(adminUserService.findAll)
      .mockResolvedValueOnce(makePage(SAMPLE)) // onMounted
      .mockReturnValueOnce(defA.promise)       // search A (older)
      .mockReturnValueOnce(defB.promise)       // search B (newer)

    const wrapper = mount(DashboardView)
    await flushPromises()

    // Dispatch search A, then search B — both in-flight.
    await wrapper.find('input.search-input').setValue('an')
    vi.advanceTimersByTime(DEBOUNCE_MS)
    await wrapper.find('input.search-input').setValue('bi')
    vi.advanceTimersByTime(DEBOUNCE_MS)

    // B (newer) resolves first, then A (older) resolves late.
    defB.resolve(makePage([BIA]))
    await flushPromises()
    defA.resolve(makePage([ANA]))
    await flushPromises()

    // The stale A response must not overwrite B's result.
    expect(wrapper.text()).toContain('Bia')
    expect(wrapper.text()).not.toContain('Ana')
  })

  it('keeps sending the active term when paginating', async () => {
    vi.useFakeTimers()
    // totalElements > pageSize so the pagination controls render.
    vi.mocked(adminUserService.findAll).mockResolvedValue(makePage(SAMPLE, 40))

    const wrapper = mount(DashboardView)
    await flushPromises()

    await wrapper.find('input.search-input').setValue('ali')
    vi.advanceTimersByTime(DEBOUNCE_MS)
    await flushPromises()

    const nextButton = wrapper.findAll('button.btn-page').find(b => b.text().includes('Próxima'))
    await nextButton!.trigger('click')
    await flushPromises()

    expect(adminUserService.findAll).toHaveBeenLastCalledWith(1, 20, 'ali')
  })
})

// ── Self-action guard (item 9) ─────────────────────────────────────────────────
// When the authenticated admin is in the user list, their own row's action buttons
// must be disabled. Other users' buttons remain enabled.

describe('DashboardView — self-action guard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('disables all three action buttons for the admin own row', async () => {
    // given the authenticated admin has id=1 (same as SAMPLE[0] = Alice)
    vi.mocked(meService.getMe).mockResolvedValue(makeMe(1))
    vi.mocked(adminUserService.findAll).mockResolvedValue(makePage(SAMPLE))

    const wrapper = mount(DashboardView)
    await flushPromises()

    // The table rows are in the same order as SAMPLE.
    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(2)

    // Row 0 is Alice (id=1, same as logged-in admin) — all 3 buttons must be disabled.
    const selfButtons = rows[0].findAll('button.btn-table')
    expect(selfButtons).toHaveLength(3)
    selfButtons.forEach((btn) => {
      expect(btn.attributes('disabled')).toBeDefined()
    })
  })

  it('keeps action buttons enabled for other users in the same page', async () => {
    // given the authenticated admin has id=1 (only Alice's row is self)
    vi.mocked(meService.getMe).mockResolvedValue(makeMe(1))
    vi.mocked(adminUserService.findAll).mockResolvedValue(makePage(SAMPLE))

    const wrapper = mount(DashboardView)
    await flushPromises()

    const rows = wrapper.findAll('tbody tr')

    // Row 1 is Bob (id=2, different from admin id=1).
    // Bob's status is BLOCKED so "Desbloquear" is enabled; revoke and delete too.
    const otherButtons = rows[1].findAll('button.btn-table')
    expect(otherButtons).toHaveLength(3)
    otherButtons.forEach((btn) => {
      expect(btn.attributes('disabled')).toBeUndefined()
    })
  })

  it('disables no buttons when the admin id does not appear in the user list', async () => {
    // given the admin id=99 is not in SAMPLE (ids 1 and 2)
    vi.mocked(meService.getMe).mockResolvedValue(makeMe(99))
    vi.mocked(adminUserService.findAll).mockResolvedValue(makePage(SAMPLE))

    const wrapper = mount(DashboardView)
    await flushPromises()

    const rows = wrapper.findAll('tbody tr')
    rows.forEach((row) => {
      row.findAll('button.btn-table').forEach((btn) => {
        expect(btn.attributes('disabled')).toBeUndefined()
      })
    })
  })
})
