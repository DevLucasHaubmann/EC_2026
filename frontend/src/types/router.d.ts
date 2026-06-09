// Module augmentation for Vue Router — extends RouteMeta with project-specific fields.
// Must be a module (has export {}) so the augmentation merges correctly.
import 'vue-router'

export {}

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresAdmin?: boolean
    guestOnly?: boolean
    userOnly?: boolean
    noSidebar?: boolean
  }
}
