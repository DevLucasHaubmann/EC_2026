import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Rehydrate session from localStorage before the first route is resolved
const authStore = useAuthStore()
authStore.restoreSession().finally(() => {
  app.mount('#app')
})
