<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
// Importando os seus serviços que já existem na branch!
import { profileService } from '@/services/modules/profile';
import { assessmentService } from '@/services/modules/assessment';
import { aiService } from '@/services/modules/aiRecommendation';

const router = useRouter();
const carregando = ref(false);

const form = ref({
  peso: null,
  altura: null,
  idade: null,
  genero: '',
  objetivo: '',
  nivelAtividade: 'MODERATE' // Padrão
});

const finalizarTriagem = async () => {
  try {
    carregando.value = true;

    // 1. Salva os dados físicos (Perfil)
    await profileService.createOwn({
      dateOfBirth: "2000-01-01", // Você pode adicionar isso no form depois
      gender: form.value.genero.toUpperCase(),
      weightKg: form.value.peso,
      heightCm: form.value.altura * 100, // Converte metros para centímetros
      activityLevel: form.value.nivelAtividade
    });

    // 2. Salva o objetivo da pessoa (Triagem)
    await assessmentService.createOwn({
      goal: form.value.objetivo === 'perda' ? 'WEIGHT_LOSS' : 'MUSCLE_GAIN',
      dietaryRestrictions: "",
      allergies: "",
      healthConditions: ""
    });

    // 3. Aciona a IA do backend para gerar a dieta
    await aiService.generateNew();

    // 4. Se deu tudo certo, vai pra tela de dieta!
    router.push('/dieta');

  } catch (error) {
    console.error('Erro ao salvar triagem:', error);
    alert('Ops! Ocorreu um erro ao gerar sua dieta. Tente novamente.');
  } finally {
    carregando.value = false;
  }
};
</script>