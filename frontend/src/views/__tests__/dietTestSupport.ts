import type { DietType } from '@/types/api'

// Advanced diet types that the Sprint 2.8 audit requires to be explicitly proven to flow
// through the user/admin payloads. Shared across the TriagemView, AdminUserModal and
// EditarPerfilView specs so a new advanced type is added in exactly one place.
// ONIVORA is intentionally excluded here: it is the default selection and is exercised
// through the control paths (default render, non-restrictive caution, preload).
export const CRITICAL_DIET_TYPES: DietType[] = [
  'LOW_CARB',
  'CETOGENICA',
  'CARNIVORA',
  'FLEXITARIANA',
  'VEGANA',
  'PESCATARIANA',
]
