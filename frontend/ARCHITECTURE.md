# Frontend Architecture - Tukan

Estrutura profissional e escalável do frontend Vue do projeto Tukan.

## Estrutura de Diretórios

```
frontend/src/
├─ assets/                    # Imagens, fontes e arquivos estáticos
│  ├─ images/                 # Imagens PNG, JPG, SVG
│  ├─ fonts/                  # Fontes customizadas
│  ├─ base.css                # Reset CSS & variáveis globais
│  └─ main.css                # Estilos globais aplicados
│
├─ components/                # Componentes Vue reutilizáveis
│  ├─ ui/                      # Componentes de interface (botões, cards, inputs)
│  │  ├─ SummaryCard.vue       # Card de métrica com título e valor
│  │  └─ QuickActionWidget.vue # Botão de ação rápida
│  │
│  ├─ layout/                  # Componentes de estrutura
│  │  └─ TopBar.vue            # Barra de navegação superior
│  │
│  ├─ GreetingHero.vue         # Componente de boas-vindas
│  ├─ RecommendationWidget.vue # Card de recomendação inteligente
│  └─ ProgressWidget.vue       # Card de progresso do usuário
│
├─ composables/                # Vue Composables reutilizáveis
│  └─ index.ts                 # Exportações de composables
│
├─ layouts/                    # Layout templates compostos
│  └─ index.ts                 # Documentação de layouts
│
├─ router/                     # Configuração de rotas
│  └─ index.ts                 # Definição de rotas
│
├─ services/                   # Serviços e lógica de negócio
│  ├─ http/                    # Configuração do cliente HTTP
│  │  └─ api.ts                # Instância axios configurada
│  │
│  └─ modules/                 # Serviços por feature
│     ├─ aiRecommendation.ts   # Recomendações IA
│     ├─ mealPlan.ts           # Planos de refeição
│     ├─ user.ts               # Dados do usuário
│     └─ index.ts              # Exportações
│
├─ stores/                     # Estado global (Pinia)
│  ├─ authStore.ts             # Estado de autenticação
│  ├─ userStore.ts             # Estado do usuário
│  ├─ uiStore.ts               # Estado de UI (modais, notificações)
│  └─ index.ts                 # Exportações
│
├─ styles/                     # Estilos CSS globais e utilities
│  ├─ variables.css            # Variáveis do design system
│  ├─ utilities.css            # Classes utility (espacamento, etc)
│  ├─ layouts.css              # Layout utilities
│  └─ index.css                # Arquivo principal
│
├─ types/                      # Tipos TypeScript globais
│  ├─ user.ts                  # Tipos de usuário
│  ├─ api.ts                   # Tipos de respostas da API
│  ├─ domain.ts                # Modelos de domínio
│  └─ index.ts                 # Exportações
│
├─ utils/                      # Funções utilitárias
│  ├─ formatters.ts            # Formatação de data, número, moeda
│  ├─ validators.ts            # Validações de formulário
│  ├─ helpers.ts               # Funções genéricas
│  └─ index.ts                 # Exportações
│
├─ views/                      # Páginas da aplicação
│  ├─ auth/                     # Autenticação
│  │  ├─ LoginView.vue
│  │  ├─ RegisterView.vue
│  │  └─ ResetPasswordView.vue
│  │
│  ├─ dashboard/               # Dashboard principal
│  │  ├─ HomeView.vue          # Página inicial
│  │  └─ OverviewView.vue      # Visão geral
│  │
│  ├─ profile/                 # Perfil do usuário
│  │  ├─ ProfileView.vue
│  │  └─ SettingsView.vue
│  │
│  └─ recommendations/         # Recomendações e planos
│     ├─ RecommendationView.vue
│     └─ MealPlanView.vue
│
├─ App.vue                     # Componente raiz (apenas RouterView)
├─ main.ts                     # Ponto de entrada da aplicação
└─ env.d.ts                    # Declarações de tipos para módulos .vue
```

## Convenções

### Nomenclatura
- **Pastas**: kebab-case (ex: `quick-action-widget/`)
- **Componentes Vue**: PascalCase (ex: `TopBar.vue`)
- **Serviços/Utilitários**: camelCase (ex: `formatters.ts`)
- **Tipos**: PascalCase (ex: `UserType.ts`)

### Imports
```typescript
// Usar path absolutos com alias @
import { api } from '@/services/http/api'
import { formatDate } from '@/utils/formatters'
import type { User } from '@/types/user'
```

### Organização por Feature
- Código relacionado à mesma feature fica no mesmo local
- Views contém toda lógica de página específica
- Components são blocos reutilizáveis
- Services centralizam requisições HTTP

## Fluxo de Dados

```
View (página)
  ├─ [Composable] → lógica reativa
  ├─ [Store] → estado global (Pinia)
  ├─ [Service] → requisições HTTP
  └─ [Component] → UI reutilizável
      └─ [UI Component] → elemento base
```

## Próximas Etapas

- [ ] Implementar serviços HTTP em `services/modules/`
- [ ] Criar stores Pinia em `stores/`
- [ ] Desenvolver componentes UI em `components/ui/`
- [ ] Adicionar tipos TypeScript em `types/`
- [ ] Implementar validadores e formatters em `utils/`
- [ ] Criar composables reutilizáveis em `composables/`
- [ ] Definir design system em `styles/`

## Notas Arquiteturais

✅ **Sem overengineering** - apenas camadas necessárias
✅ **Clareza estrutural** - fácil navegar e entender
✅ **Escalabilidade** - suporta crescimento do projeto
✅ **Separação clara** - código agrupado por responsabilidade
✅ **TypeScript** - type-safe em toda a aplicação
✅ **Profissional** - pronto para ambiente de produção

---

**Criado em:** Abril 2026
**Versão:** 1.0
