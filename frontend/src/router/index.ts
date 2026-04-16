import { createRouter, createWebHistory } from 'vue-router'
import TestApiView from '../views/TestApiView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/test-api',
      name: 'test-api',
      component: TestApiView,
    },
  ],
})

export default router