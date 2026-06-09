-- Fix: adiciona MORNING_SNACK ao suitable_meals dos alimentos elegíveis para lanche da manhã
--
-- Contexto: O campo suitable_meals define em quais refeições um alimento pode aparecer.
-- Alimentos marcados como BREAKFAST,AFTERNOON_SNACK são semanticamente adequados também para
-- o lanche da manhã (MORNING_SNACK), que é usado quando o usuário escolhe 5 refeições por dia.
-- Sem essa correção, a geração de dieta com 5 refeições lança BusinessException por pool vazio.
--
-- Afetados: 1176 alimentos (frutas, laticínios, cereais, barras, pães, etc.)
-- Não afetados: alimentos com apenas AFTERNOON_SNACK (temperos, industrializados — inadequados)
--
-- Executar uma única vez no banco tukan_db.

UPDATE food
SET suitable_meals = 'BREAKFAST,MORNING_SNACK,AFTERNOON_SNACK'
WHERE suitable_meals = 'BREAKFAST,AFTERNOON_SNACK'
  AND active = 1;
