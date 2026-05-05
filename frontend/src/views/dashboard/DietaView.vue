<script setup lang="ts">
import { ref, onMounted } from 'vue';
// Importando o serviço de IA da sua branch
import { aiService } from '@/services/modules/aiRecommendation';

const dieta = ref(null);
const carregando = ref(true);

onMounted(async () => {
  try {
    // Busca a dieta mais recente vinda do Spring Boot
    const resposta = await aiService.getLatest();

    // Adaptando o retorno do backend para o formato que a sua tela espera
    dieta.value = {
      objetivo: resposta.goal === 'MUSCLE_GAIN' ? 'Ganho de Massa' : 'Redução de Gordura',
      totalKcal: resposta.totalCalories || 0,
      macros: {
        proteina: `${resposta.proteinGrams || 0}g`,
        carbo: `${resposta.carbsGrams || 0}g`,
        gordura: `${resposta.fatGrams || 0}g`
      },
      // Fazendo um 'map' para listar as refeições geradas pela IA
      refeicoes: resposta.meals.map((meal: any, index: number) => ({
        id: index + 1,
        nome: meal.name,
        horario: meal.time || 'A definir',
        itens: meal.items || meal.foods.map((f: any) => f.name) // Se o backend retornar um array de 'foods'
      }))
    };
  } catch (error) {
    console.error("Erro ao buscar a dieta:", error);
  } finally {
    carregando.value = false;
  }
});
</script>