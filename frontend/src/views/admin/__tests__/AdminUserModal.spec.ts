// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import type { AdminUser, AssessmentData, ProfileData } from '@/types/admin'
import { DIET_TYPE_OPTIONS } from '@/constants/assessmentOptions'
import { CRITICAL_DIET_TYPES } from '@/views/__tests__/dietTestSupport'

// ── Mocks ─────────────────────────────────────────────────────────────────────
// The admin triagem form maps dietType into the create/update payload inside
// <script setup>; we mount the modal and assert against the service boundary.

vi.mock('@/services/modules/adminAssessment', () => ({
  adminAssessmentService: {
    findByUserId: vi.fn(),
    create: vi.fn().mockResolvedValue({}),
    update: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue(undefined),
  },
}))
vi.mock('@/services/modules/adminProfile', () => ({
  adminProfileService: {
    findByUserId: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}))
vi.mock('@/services/modules/adminUser', () => ({
  adminUserService: {
    update: vi.fn(),
    delete: vi.fn(),
    revokeSessions: vi.fn(),
  },
}))
vi.mock('@/composables/useToast', () => ({
  useToast: () => ({ success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() }),
}))

import AdminUserModal from '@/views/admin/AdminUserModal.vue'
import { adminAssessmentService } from '@/services/modules/adminAssessment'
import { adminProfileService } from '@/services/modules/adminProfile'
import { adminUserService } from '@/services/modules/adminUser'

const user: AdminUser = {
  id: 42,
  name: 'Maria',
  email: 'maria@tukan.test',
  type: 'USER',
  status: 'ACTIVE',
  avatarUrl: null,
}

const existingProfile: ProfileData = {
  id: 7,
  userId: 42,
  userName: 'Maria',
  dateOfBirth: '1992-05-10',
  gender: 'FEMALE',
  weightKg: 60,
  heightCm: 165,
  activityLevel: 'MODERATE',
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: null,
}

const existingAssessment: AssessmentData = {
  id: 99,
  userId: 42,
  userName: 'Maria',
  goal: 'WEIGHT_LOSS',
  dietType: 'ONIVORA',
  dietaryRestrictions: 'sem glúten',
  allergies: 'Amendoim',
  healthConditions: 'Hipertensão',
  mealsPerDay: 4,
  targetWeightKg: 55,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: null,
}

const notFound = { response: { status: 404 } }

// The dietType <select> is the one whose options include the advanced diet values
// (the goal <select> never does), so we locate it structurally, not by label text.
function dietSelect(wrapper: VueWrapper) {
  const target = wrapper
    .findAll('select')
    .find((s) => s.findAll('option').some((o) => (o.element as HTMLOptionElement).value === 'CETOGENICA'))
  if (!target) throw new Error('Diet <select> not rendered')
  return target
}

// The triagem form's submit button is the `.btn-salvar` inside the form footer — stable
// across the create ("Criar Triagem") and edit ("Salvar Triagem") modes, and never
// collides with the "+ Criar Triagem" toggle (a `.btn-outline-sm` outside the footer).
function triagemSubmit(wrapper: VueWrapper) {
  return wrapper.find('.aba-footer .btn-salvar')
}

// The "Refeições / dia" input is the number field constrained to 3..5 (min="3"); the only other
// number input (peso alvo) has no min, so this locates mealsPerDay structurally, not by label.
function mealsInput(wrapper: VueWrapper) {
  const target = wrapper
    .findAll('input[type="number"]')
    .find((i) => i.attributes('min') === '3')
  if (!target) throw new Error('mealsPerDay input not rendered')
  return target
}

async function openTriagemTab(wrapper: VueWrapper) {
  const triagemTab = wrapper.findAll('.tab-btn').find((b) => b.text() === 'Triagem')
  if (!triagemTab) throw new Error('Triagem tab not found')
  await triagemTab.trigger('click')
  await flushPromises() // lazy-load of triagem + profile
}

// Helper: mount the modal with isSelf defaulting to false (preserves existing test behaviour).
function mountModal(props: { user: AdminUser; isSelf?: boolean }) {
  return mount(AdminUserModal, { props: { isSelf: false, ...props } })
}

describe('AdminUserModal — diet type in the admin triagem flow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('offers all 8 diet types (plus the "not defined" null option) when creating a triagem', async () => {
    // given a user with a profile but no triagem
    vi.mocked(adminProfileService.findByUserId).mockResolvedValue(existingProfile)
    vi.mocked(adminAssessmentService.findByUserId).mockRejectedValue(notFound)
    const wrapper = mountModal({ user })
    await openTriagemTab(wrapper)

    // when starting a new triagem
    const createBtn = wrapper.findAll('button').find((b) => b.text().includes('Criar Triagem'))
    await createBtn!.trigger('click')

    // then the diet selector exposes the null option followed by all 8 canonical types
    const optionValues = dietSelect(wrapper)
      .findAll('option')
      .map((o) => (o.element as HTMLOptionElement).value)
    // first option is the "not defined" (null) placeholder; the rest are the 8 canonical types
    expect(optionValues).toHaveLength(9)
    expect(optionValues.slice(1)).toEqual(DIET_TYPE_OPTIONS.map((opt) => opt.value))
  })

  it.each(CRITICAL_DIET_TYPES)(
    'sends dietType=%s when an admin creates a triagem',
    async (dietType) => {
      // given a user with a profile and no triagem
      vi.mocked(adminProfileService.findByUserId).mockResolvedValue(existingProfile)
      vi.mocked(adminAssessmentService.findByUserId).mockRejectedValue(notFound)
      const wrapper = mountModal({ user })
      await openTriagemTab(wrapper)
      const openFormBtn = wrapper.findAll('button').find((b) => b.text().includes('Criar Triagem'))
      await openFormBtn!.trigger('click')

      // when the admin selects the diet type, sets meals/day (now required) and saves
      await dietSelect(wrapper).setValue(dietType)
      await mealsInput(wrapper).setValue(4)
      await triagemSubmit(wrapper).trigger('click')
      await flushPromises()

      // then the create payload carries exactly that dietType and mealsPerDay, scoped to user 42
      expect(adminAssessmentService.create).toHaveBeenCalledTimes(1)
      const [userId, payload] = vi.mocked(adminAssessmentService.create).mock.calls[0]!
      expect(userId).toBe(42)
      expect(payload.dietType).toBe(dietType)
      expect(payload.mealsPerDay).toBe(4)
    },
  )

  it('blocks creation and shows a clear error when dietType is missing', async () => {
    // given a user with a profile and no triagem
    vi.mocked(adminProfileService.findByUserId).mockResolvedValue(existingProfile)
    vi.mocked(adminAssessmentService.findByUserId).mockRejectedValue(notFound)
    const wrapper = mountModal({ user })
    await openTriagemTab(wrapper)
    const openFormBtn = wrapper.findAll('button').find((b) => b.text().includes('Criar Triagem'))
    await openFormBtn!.trigger('click')

    // when the admin sets meals/day but leaves dietType unselected and tries to save
    await mealsInput(wrapper).setValue(4)
    await triagemSubmit(wrapper).trigger('click')
    await flushPromises()

    // then submit is blocked: no payload is sent and a clear error is shown
    expect(adminAssessmentService.create).not.toHaveBeenCalled()
    expect(wrapper.find('.msg.erro').text()).toContain('Tipo de dieta é obrigatório')
  })

  it('blocks creation and shows a clear error when mealsPerDay is missing', async () => {
    // given a user with a profile and no triagem
    vi.mocked(adminProfileService.findByUserId).mockResolvedValue(existingProfile)
    vi.mocked(adminAssessmentService.findByUserId).mockRejectedValue(notFound)
    const wrapper = mountModal({ user })
    await openTriagemTab(wrapper)
    const openFormBtn = wrapper.findAll('button').find((b) => b.text().includes('Criar Triagem'))
    await openFormBtn!.trigger('click')

    // when the admin selects a dietType but leaves meals/day empty and tries to save
    await dietSelect(wrapper).setValue('ONIVORA')
    await triagemSubmit(wrapper).trigger('click')
    await flushPromises()

    // then submit is blocked: no payload is sent and a clear error is shown
    expect(adminAssessmentService.create).not.toHaveBeenCalled()
    expect(wrapper.find('.msg.erro').text()).toContain('Número de refeições por dia é obrigatório')
  })

  it('changes dietType on edit while keeping dietaryRestrictions independent', async () => {
    // given an existing triagem with dietType ONIVORA and a separate restrictions string
    vi.mocked(adminProfileService.findByUserId).mockResolvedValue(existingProfile)
    vi.mocked(adminAssessmentService.findByUserId).mockResolvedValue(existingAssessment)
    const wrapper = mountModal({ user })
    await openTriagemTab(wrapper)

    // when the admin edits and switches the diet to CARNIVORA. One representative type is
    // enough here: create/edit share the same dietType v-model and submitTriagem() mapping,
    // and the create path is already proven for all 6 critical types above. This case adds
    // the non-conflation invariant that is specific to editing a pre-loaded assessment.
    const editBtn = wrapper.findAll('button').find((b) => b.text() === 'Editar')
    await editBtn!.trigger('click')
    await dietSelect(wrapper).setValue('CARNIVORA')
    await triagemSubmit(wrapper).trigger('click')
    await flushPromises()

    // then the update payload reflects the new dietType, and dietaryRestrictions is
    // preserved untouched as a distinct field (no conflation of the two)
    expect(adminAssessmentService.update).toHaveBeenCalledTimes(1)
    const [id, payload] = vi.mocked(adminAssessmentService.update).mock.calls[0]!
    expect(id).toBe(99)
    expect(payload.dietType).toBe('CARNIVORA')
    expect(payload.dietaryRestrictions).toBe('sem glúten')
  })

  it('shows the current dietType label in the read-only triagem view', async () => {
    // given an existing triagem with dietType ONIVORA
    vi.mocked(adminProfileService.findByUserId).mockResolvedValue(existingProfile)
    vi.mocked(adminAssessmentService.findByUserId).mockResolvedValue({
      ...existingAssessment,
      dietType: 'VEGANA',
    })
    const wrapper = mountModal({ user })
    await openTriagemTab(wrapper)

    // then the diet preference is shown with its human label, not the raw enum
    expect(wrapper.text()).toContain('Vegana')
    expect(wrapper.text()).not.toContain('VEGANA')
  })
})

// ── Admin profile deletion (and its conditional cascade over the triagem) ───────
// This locks the cross-tab orchestration extracted into useAdminPerfil/useAdminTriagem:
// loading is keyed by userId, but the destructive calls target each entity's own id,
// and a linked-assessment failure must keep the profile intact.

async function openPerfilTab(wrapper: VueWrapper) {
  const perfilTab = wrapper.findAll('.tab-btn').find((b) => b.text() === 'Perfil')
  if (!perfilTab) throw new Error('Perfil tab not found')
  await perfilTab.trigger('click')
  await flushPromises() // lazy-load of profile
}

// Opens the inline delete confirmation from the read-only profile view. This also
// resolves the triagem state (solicitarExclusaoPerfil loads it on demand), so the
// confirmation renders its simple- or cascade-variant deterministically.
async function abrirConfirmacaoExcluirPerfil(wrapper: VueWrapper) {
  await wrapper.find('.btn-danger-outline').trigger('click')
  await flushPromises()
}

describe('AdminUserModal — admin profile deletion by userId', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('deletes only the profile (by its own id) when the user has no triagem', async () => {
    // given a user with a profile and no triagem
    vi.mocked(adminProfileService.findByUserId).mockResolvedValue(existingProfile)
    vi.mocked(adminAssessmentService.findByUserId).mockRejectedValue(notFound)
    vi.mocked(adminProfileService.delete).mockResolvedValue(undefined)
    const wrapper = mountModal({ user })
    await openPerfilTab(wrapper)

    // when the admin confirms deletion (simple variant — no linked triagem)
    await abrirConfirmacaoExcluirPerfil(wrapper)
    expect(wrapper.find('.confirm-inline').text()).toContain('Confirma exclusão do perfil')
    await wrapper.find('.confirm-inline .btn-danger').trigger('click')
    await flushPromises()

    // then only the profile is deleted, addressed by the entity id (7), not the userId (42)
    expect(adminProfileService.delete).toHaveBeenCalledTimes(1)
    expect(adminProfileService.delete).toHaveBeenCalledWith(7)
    expect(adminAssessmentService.delete).not.toHaveBeenCalled()
  })

  it('cascades: deletes the linked assessment first, then the profile, each by its own id', async () => {
    // given a user with both a profile and a triagem
    vi.mocked(adminProfileService.findByUserId).mockResolvedValue(existingProfile)
    vi.mocked(adminAssessmentService.findByUserId).mockResolvedValue(existingAssessment)
    vi.mocked(adminAssessmentService.delete).mockResolvedValue(undefined)
    vi.mocked(adminProfileService.delete).mockResolvedValue(undefined)
    const wrapper = mountModal({ user })
    await openPerfilTab(wrapper)

    // when the admin confirms deletion (cascade variant — triagem is linked)
    await abrirConfirmacaoExcluirPerfil(wrapper)
    expect(wrapper.find('.confirm-inline').text()).toContain('triagem vinculada')
    await wrapper.find('.confirm-inline .btn-danger').trigger('click')
    await flushPromises()

    // then both are deleted by their own ids: assessment (99) then profile (7)
    expect(adminAssessmentService.delete).toHaveBeenCalledWith(99)
    expect(adminProfileService.delete).toHaveBeenCalledWith(7)
  })

  it('keeps the profile and warns when the linked assessment deletion fails', async () => {
    // given a user with both a profile and a triagem, but the assessment delete fails
    vi.mocked(adminProfileService.findByUserId).mockResolvedValue(existingProfile)
    vi.mocked(adminAssessmentService.findByUserId).mockResolvedValue(existingAssessment)
    vi.mocked(adminAssessmentService.delete).mockRejectedValue({ response: { data: {} } })
    const wrapper = mountModal({ user })
    await openPerfilTab(wrapper)

    // when the admin confirms the cascade deletion
    await abrirConfirmacaoExcluirPerfil(wrapper)
    await wrapper.find('.confirm-inline .btn-danger').trigger('click')
    await flushPromises()

    // then the cascade stops before the profile: assessment delete was attempted, profile was not,
    // and the banner makes clear the profile was kept
    expect(adminAssessmentService.delete).toHaveBeenCalledWith(99)
    expect(adminProfileService.delete).not.toHaveBeenCalled()
    expect(wrapper.find('.msg.erro').text()).toContain('O perfil não foi excluído.')
  })
})

// ── isSelf guard (item 9) ──────────────────────────────────────────────────────
// When isSelf=true the modal must disable destructive actions and type/status selects.
// When isSelf=false behaviour must be identical to before this change.

async function openAcoesTab(wrapper: VueWrapper) {
  const acoesTab = wrapper.findAll('.tab-btn').find((b) => b.text() === 'Ações')
  if (!acoesTab) throw new Error('Ações tab not found')
  await acoesTab.trigger('click')
  await flushPromises()
}

async function openDadosTab(wrapper: VueWrapper) {
  // Dados is the default tab, but we click it explicitly for clarity.
  const dadosTab = wrapper.findAll('.tab-btn').find((b) => b.text() === 'Dados')
  if (!dadosTab) throw new Error('Dados tab not found')
  await dadosTab.trigger('click')
  await flushPromises()
}

describe('AdminUserModal — isSelf guard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('disables Revogar Sessões and Deletar Usuário buttons when isSelf=true', async () => {
    // given the modal is opened for the admin's own account
    const wrapper = mountModal({ user, isSelf: true })
    await openAcoesTab(wrapper)

    const acoesButtons = wrapper.findAll('.btn-acao')
    // Revogar Sessões
    const revogar = acoesButtons.find((b) => b.text().includes('Revogar'))
    expect(revogar).toBeDefined()
    expect(revogar!.attributes('disabled')).toBeDefined()

    // Deletar Usuário
    const deletar = acoesButtons.find((b) => b.text().includes('Deletar'))
    expect(deletar).toBeDefined()
    expect(deletar!.attributes('disabled')).toBeDefined()
  })

  it('shows the self-warning message in the Ações tab when isSelf=true', async () => {
    // given isSelf=true
    const wrapper = mountModal({ user, isSelf: true })
    await openAcoesTab(wrapper)

    // then the warning message is visible
    expect(wrapper.find('.msg-self-warn').exists()).toBe(true)
    expect(wrapper.find('.msg-self-warn').text()).toContain('própria conta')
  })

  it('disables type and status selects in the Dados tab when isSelf=true', async () => {
    // given the modal is opened for the admin's own account
    const wrapper = mountModal({ user, isSelf: true })
    await openDadosTab(wrapper)

    const selects = wrapper.findAll('select')
    const typeSelect = selects.find((s) =>
      s.findAll('option').some((o) => (o.element as HTMLOptionElement).value === 'ADMIN'),
    )
    const statusSelect = selects.find((s) =>
      s.findAll('option').some((o) => (o.element as HTMLOptionElement).value === 'BANNED'),
    )

    expect(typeSelect).toBeDefined()
    expect(typeSelect!.attributes('disabled')).toBeDefined()

    expect(statusSelect).toBeDefined()
    expect(statusSelect!.attributes('disabled')).toBeDefined()
  })

  it('enables Revogar Sessões and Deletar Usuário buttons when isSelf=false', async () => {
    // given the modal is opened for a different user
    const wrapper = mountModal({ user, isSelf: false })
    await openAcoesTab(wrapper)

    const acoesButtons = wrapper.findAll('.btn-acao')

    const revogar = acoesButtons.find((b) => b.text().includes('Revogar'))
    expect(revogar).toBeDefined()
    expect(revogar!.attributes('disabled')).toBeUndefined()

    const deletar = acoesButtons.find((b) => b.text().includes('Deletar'))
    expect(deletar).toBeDefined()
    expect(deletar!.attributes('disabled')).toBeUndefined()
  })

  it('does not show the self-warning message when isSelf=false', async () => {
    // given isSelf=false
    const wrapper = mountModal({ user, isSelf: false })
    await openAcoesTab(wrapper)

    expect(wrapper.find('.msg-self-warn').exists()).toBe(false)
  })

  it('enables type and status selects in the Dados tab when isSelf=false', async () => {
    // given isSelf=false
    const wrapper = mountModal({ user, isSelf: false })
    await openDadosTab(wrapper)

    const selects = wrapper.findAll('select')
    const typeSelect = selects.find((s) =>
      s.findAll('option').some((o) => (o.element as HTMLOptionElement).value === 'ADMIN'),
    )
    const statusSelect = selects.find((s) =>
      s.findAll('option').some((o) => (o.element as HTMLOptionElement).value === 'BANNED'),
    )

    expect(typeSelect).toBeDefined()
    expect(typeSelect!.attributes('disabled')).toBeUndefined()

    expect(statusSelect).toBeDefined()
    expect(statusSelect!.attributes('disabled')).toBeUndefined()
  })

  it('omits type/status from the save payload when isSelf=true (defense in depth)', async () => {
    // given the modal is opened for the admin's own account
    vi.mocked(adminUserService.update).mockResolvedValue({} as never)
    const wrapper = mountModal({ user, isSelf: true })
    await openDadosTab(wrapper)

    // when saving the Dados tab
    const salvar = wrapper.findAll('.btn-salvar').find((b) => b.text().includes('Salvar Dados'))
    expect(salvar).toBeDefined()
    await salvar!.trigger('click')
    await flushPromises()

    // then only name/email are sent — type/status are stripped (backend rejects self changes)
    expect(adminUserService.update).toHaveBeenCalledTimes(1)
    const [, payload] = vi.mocked(adminUserService.update).mock.calls[0]!
    expect(payload).not.toHaveProperty('type')
    expect(payload).not.toHaveProperty('status')
    expect(payload).toMatchObject({ name: user.name, email: user.email })
  })

  it('includes type/status in the save payload when isSelf=false', async () => {
    // given the modal is opened for a different user
    vi.mocked(adminUserService.update).mockResolvedValue({} as never)
    const wrapper = mountModal({ user, isSelf: false })
    await openDadosTab(wrapper)

    // when saving the Dados tab
    const salvar = wrapper.findAll('.btn-salvar').find((b) => b.text().includes('Salvar Dados'))
    await salvar!.trigger('click')
    await flushPromises()

    // then the full payload (including type/status) is sent
    expect(adminUserService.update).toHaveBeenCalledTimes(1)
    const [, payload] = vi.mocked(adminUserService.update).mock.calls[0]!
    expect(payload).toHaveProperty('type')
    expect(payload).toHaveProperty('status')
  })
})
