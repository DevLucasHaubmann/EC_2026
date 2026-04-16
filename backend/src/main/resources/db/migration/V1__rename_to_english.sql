-- ============================================================
-- Migração: Renomear colunas e atualizar valores de enum/categoria
-- Estado atual: tabelas já com nomes em inglês (criadas pelo ddl-auto),
-- mas colunas e valores de enum ainda em português.
-- ============================================================

-- ── 1. NUTRITIONAL_PROFILE ──────────────────────────────────

-- Alterar enum de sexo para inglês (precisa recriar a coluna enum)
ALTER TABLE nutritional_profile
    MODIFY COLUMN sexo ENUM('MALE','FEMALE','MASCULINO','FEMININO') NOT NULL;
UPDATE nutritional_profile SET sexo = 'MALE' WHERE sexo = 'MASCULINO';
UPDATE nutritional_profile SET sexo = 'FEMALE' WHERE sexo = 'FEMININO';
ALTER TABLE nutritional_profile
    MODIFY COLUMN sexo ENUM('MALE','FEMALE') NOT NULL;

-- Alterar enum de nível de atividade
ALTER TABLE nutritional_profile
    MODIFY COLUMN nivel_atividade ENUM('SEDENTARY','LIGHT','MODERATE','INTENSE','VERY_INTENSE','SEDENTARIO','LEVE','MODERADO','INTENSO','MUITO_INTENSO') NOT NULL;
UPDATE nutritional_profile SET nivel_atividade = 'SEDENTARY' WHERE nivel_atividade = 'SEDENTARIO';
UPDATE nutritional_profile SET nivel_atividade = 'LIGHT' WHERE nivel_atividade = 'LEVE';
UPDATE nutritional_profile SET nivel_atividade = 'MODERATE' WHERE nivel_atividade = 'MODERADO';
UPDATE nutritional_profile SET nivel_atividade = 'INTENSE' WHERE nivel_atividade = 'INTENSO';
UPDATE nutritional_profile SET nivel_atividade = 'VERY_INTENSE' WHERE nivel_atividade = 'MUITO_INTENSO';
ALTER TABLE nutritional_profile
    MODIFY COLUMN nivel_atividade ENUM('SEDENTARY','LIGHT','MODERATE','INTENSE','VERY_INTENSE') NOT NULL;

-- Renomear colunas
ALTER TABLE nutritional_profile
    CHANGE COLUMN usuario_id user_id INT NOT NULL,
    CHANGE COLUMN data_nascimento date_of_birth DATE NOT NULL,
    CHANGE COLUMN sexo gender ENUM('MALE','FEMALE') NOT NULL,
    CHANGE COLUMN peso_kg weight_kg DOUBLE NOT NULL,
    CHANGE COLUMN altura_cm height_cm DOUBLE NOT NULL,
    CHANGE COLUMN nivel_atividade activity_level ENUM('SEDENTARY','LIGHT','MODERATE','INTENSE','VERY_INTENSE') NOT NULL,
    CHANGE COLUMN criado_em created_at DATETIME(6) NOT NULL,
    CHANGE COLUMN atualizado_em updated_at DATETIME(6);

-- ── 2. ASSESSMENT ───────────────────────────────────────────

-- Alterar enum de objetivo
ALTER TABLE assessment
    MODIFY COLUMN objetivo ENUM('WEIGHT_LOSS','MUSCLE_GAIN','MAINTENANCE','DIETARY_REEDUCATION','SPORTS_PERFORMANCE','PERDA_DE_PESO','GANHO_DE_MASSA','MANUTENCAO','REEDUCACAO_ALIMENTAR','PERFORMANCE_ESPORTIVA') NOT NULL;
UPDATE assessment SET objetivo = 'WEIGHT_LOSS' WHERE objetivo = 'PERDA_DE_PESO';
UPDATE assessment SET objetivo = 'MUSCLE_GAIN' WHERE objetivo = 'GANHO_DE_MASSA';
UPDATE assessment SET objetivo = 'MAINTENANCE' WHERE objetivo = 'MANUTENCAO';
UPDATE assessment SET objetivo = 'DIETARY_REEDUCATION' WHERE objetivo = 'REEDUCACAO_ALIMENTAR';
UPDATE assessment SET objetivo = 'SPORTS_PERFORMANCE' WHERE objetivo = 'PERFORMANCE_ESPORTIVA';
ALTER TABLE assessment
    MODIFY COLUMN objetivo ENUM('WEIGHT_LOSS','MUSCLE_GAIN','MAINTENANCE','DIETARY_REEDUCATION','SPORTS_PERFORMANCE') NOT NULL;

-- Renomear colunas
ALTER TABLE assessment
    CHANGE COLUMN usuario_id user_id INT NOT NULL,
    CHANGE COLUMN objetivo goal ENUM('WEIGHT_LOSS','MUSCLE_GAIN','MAINTENANCE','DIETARY_REEDUCATION','SPORTS_PERFORMANCE') NOT NULL,
    CHANGE COLUMN restricoes_alimentares dietary_restrictions VARCHAR(500),
    CHANGE COLUMN alergias allergies VARCHAR(500),
    CHANGE COLUMN condicoes_saude health_conditions VARCHAR(500),
    CHANGE COLUMN criado_em created_at DATETIME(6) NOT NULL,
    CHANGE COLUMN atualizado_em updated_at DATETIME(6);

-- ── 3. FOOD ─────────────────────────────────────────────────

-- Atualizar categorias
UPDATE food SET categoria = 'PROTEIN' WHERE categoria = 'PROTEINA';
UPDATE food SET categoria = 'CARBOHYDRATE' WHERE categoria = 'CARBOIDRATO';
UPDATE food SET categoria = 'VEGETABLE' WHERE categoria = 'LEGUME_VERDURA';
UPDATE food SET categoria = 'FRUIT' WHERE categoria = 'FRUTA';
UPDATE food SET categoria = 'LEGUME' WHERE categoria = 'LEGUMINOSA';
UPDATE food SET categoria = 'DAIRY' WHERE categoria = 'LATICINIO';
UPDATE food SET categoria = 'HEALTHY_FAT' WHERE categoria = 'GORDURA_BOA';
UPDATE food SET categoria = 'BEVERAGE' WHERE categoria = 'BEBIDA';
UPDATE food SET categoria = 'ADDITIVE' WHERE categoria = 'ADITIVO';
UPDATE food SET categoria = 'CONDIMENT' WHERE categoria = 'CONDIMENTO';
UPDATE food SET categoria = 'SEASONING' WHERE categoria = 'TEMPERO';
UPDATE food SET categoria = 'SAUCE' WHERE categoria = 'MOLHO';
UPDATE food SET categoria = 'OTHER' WHERE categoria = 'OUTROS';
UPDATE food SET categoria = 'FAT' WHERE categoria = 'GORDURA';
UPDATE food SET categoria = 'GRAIN' WHERE categoria = 'CEREAL';
UPDATE food SET categoria = 'NUT_SEED' WHERE categoria = 'OLEAGINOSA';

-- Atualizar meal types
UPDATE food SET tipo_refeicao_principal = 'BREAKFAST' WHERE tipo_refeicao_principal = 'CAFE_MANHA';
UPDATE food SET tipo_refeicao_principal = 'LUNCH' WHERE tipo_refeicao_principal = 'ALMOCO';
UPDATE food SET tipo_refeicao_principal = 'AFTERNOON_SNACK' WHERE tipo_refeicao_principal = 'LANCHE_TARDE';
UPDATE food SET tipo_refeicao_principal = 'DINNER' WHERE tipo_refeicao_principal = 'JANTA';

-- Atualizar suitable_meals (comma-separated)
UPDATE food SET refeicoes_indicadas = REPLACE(refeicoes_indicadas, 'CAFE_MANHA', 'BREAKFAST') WHERE refeicoes_indicadas LIKE '%CAFE_MANHA%';
UPDATE food SET refeicoes_indicadas = REPLACE(refeicoes_indicadas, 'ALMOCO', 'LUNCH') WHERE refeicoes_indicadas LIKE '%ALMOCO%';
UPDATE food SET refeicoes_indicadas = REPLACE(refeicoes_indicadas, 'LANCHE_TARDE', 'AFTERNOON_SNACK') WHERE refeicoes_indicadas LIKE '%LANCHE_TARDE%';
UPDATE food SET refeicoes_indicadas = REPLACE(refeicoes_indicadas, 'JANTA', 'DINNER') WHERE refeicoes_indicadas LIKE '%JANTA%';

-- Renomear colunas
ALTER TABLE food
    CHANGE COLUMN nome name VARCHAR(255) NOT NULL,
    CHANGE COLUMN nome_normalizado normalized_name VARCHAR(255),
    CHANGE COLUMN calorias_100g calories_per_100g DECIMAL(38,2),
    CHANGE COLUMN proteina_100g protein_per_100g DECIMAL(38,2),
    CHANGE COLUMN carboidrato_100g carbs_per_100g DECIMAL(38,2),
    CHANGE COLUMN gordura_100g fat_per_100g DECIMAL(38,2),
    CHANGE COLUMN fibra_100g fiber_per_100g DECIMAL(38,2),
    CHANGE COLUMN categoria category VARCHAR(255) NOT NULL,
    CHANGE COLUMN subcategoria subcategory VARCHAR(255),
    CHANGE COLUMN tipo_refeicao_principal primary_meal_type VARCHAR(255),
    CHANGE COLUMN refeicoes_indicadas suitable_meals VARCHAR(255),
    CHANGE COLUMN porcao_referencia_g reference_portion_grams DECIMAL(38,2),
    CHANGE COLUMN contem_lactose contains_lactose TINYINT(1) NOT NULL DEFAULT 0,
    CHANGE COLUMN contem_gluten contains_gluten TINYINT(1) NOT NULL DEFAULT 0,
    CHANGE COLUMN contem_ovo contains_egg TINYINT(1) NOT NULL DEFAULT 0,
    CHANGE COLUMN vegetariano vegetarian TINYINT(1) NOT NULL DEFAULT 0,
    CHANGE COLUMN vegano vegan TINYINT(1) NOT NULL DEFAULT 0,
    CHANGE COLUMN restricoes_compativeis compatible_restrictions VARCHAR(255),
    CHANGE COLUMN ativo active TINYINT(1) NOT NULL DEFAULT 1;

-- food não tem criado_em/atualizado_em visíveis no DESCRIBE, pular se não existirem

-- ── 4. RECOMMENDATION ───────────────────────────────────────

ALTER TABLE recommendation
    CHANGE COLUMN usuario_id user_id INT NOT NULL,
    CHANGE COLUMN resumo summary TEXT,
    CHANGE COLUMN plano_json plan_json MEDIUMTEXT,
    CHANGE COLUMN explicacao_refeicoes_json meal_explanations_json TEXT,
    CHANGE COLUMN dicas_json tips_json TEXT,
    CHANGE COLUMN alertas_json alerts_json TEXT,
    CHANGE COLUMN contexto_json context_json MEDIUMTEXT,
    CHANGE COLUMN criado_em created_at DATETIME(6) NOT NULL;

-- ── 5. RECOMMENDATION_FEEDBACK ──────────────────────────────

ALTER TABLE recommendation_feedback
    CHANGE COLUMN recomendacao_id recommendation_id INT NOT NULL,
    CHANGE COLUMN avaliacao rating ENUM('LIKED','DISLIKED') NOT NULL,
    CHANGE COLUMN motivo reason VARCHAR(500),
    CHANGE COLUMN observacao observation VARCHAR(1000),
    CHANGE COLUMN criado_em created_at DATETIME(6) NOT NULL;

-- ── 6. USER_SESSION ─────────────────────────────────────────

ALTER TABLE user_session
    CHANGE COLUMN usuario_id user_id INT NOT NULL,
    CHANGE COLUMN criado_em created_at DATETIME(6) NOT NULL,
    CHANGE COLUMN expira_em expires_at DATETIME(6) NOT NULL,
    CHANGE COLUMN revogado_em revoked_at DATETIME(6),
    CHANGE COLUMN ultimo_uso_em last_used_at DATETIME(6) NOT NULL,
    CHANGE COLUMN dispositivo device VARCHAR(255),
    CHANGE COLUMN endereco_ip ip_address VARCHAR(45);

-- ── 7. LIMPEZA: remover tabelas antigas não mais usadas ─────
-- (Tabelas legadas que o Hibernate não gerencia mais)
-- Descomente após confirmar que não há dados importantes nelas.
-- DROP TABLE IF EXISTS perfil;
-- DROP TABLE IF EXISTS opcao_refeicao;
-- DROP TABLE IF EXISTS peso_registro;
-- DROP TABLE IF EXISTS plano_alimentar;
-- DROP TABLE IF EXISTS refeicao;