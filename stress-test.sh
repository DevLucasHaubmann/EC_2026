#!/bin/bash
# ============================================================================
#  Stress Test — Tukan API (Perfil + Triagem)
#  Uso: bash stress-test.sh [NUM_USERS] [REQUESTS_PER_USER]
#  Ex:  bash stress-test.sh 30 10
# ============================================================================

set -euo pipefail

# ── Configuração ──────────────────────────────────────────────────────────────
BASE_URL="http://localhost:8081"
NUM_USERS="${1:-20}"          # Total de usuários a registrar
RPU="${2:-10}"                # Requests por usuário nas fases GET/PUT
TIMESTAMP=$(date +%s)
RESULTS_DIR="/tmp/tukan-stress-${TIMESTAMP}"
mkdir -p "$RESULTS_DIR"

# Arrays para armazenar tokens
declare -a TOKENS=()

# ── Funções auxiliares ────────────────────────────────────────────────────────

extract_json_field() {
    # Extrai valor de um campo JSON simples (string) sem depender de grep -P
    local json="$1" field="$2"
    echo "$json" | sed -n "s/.*\"${field}\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p" | head -1
}

print_header() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  $1"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

print_subheader() {
    echo ""
    echo "── $1 ──"
}

# Analisa resultados de uma fase
analyze_results() {
    local phase_dir="$1"
    local phase_name="$2"

    local total=0 success=0 fail=0
    local times=()

    for f in "$phase_dir"/*.txt; do
        [ -f "$f" ] || continue
        total=$((total + 1))
        local status
        status=$(head -1 "$f")
        local time_s
        time_s=$(tail -1 "$f")

        if [[ "$status" -ge 200 && "$status" -lt 300 ]]; then
            success=$((success + 1))
        else
            fail=$((fail + 1))
        fi
        times+=("$time_s")
    done

    if [ "$total" -eq 0 ]; then
        echo "  (sem resultados)"
        return
    fi

    # Ordenar tempos e calcular estatísticas
    local sorted
    sorted=$(printf '%s\n' "${times[@]}" | sort -n)
    local min max avg p50 p95 p99
    min=$(echo "$sorted" | head -1)
    max=$(echo "$sorted" | tail -1)

    # Média
    local sum
    sum=$(printf '%s\n' "${times[@]}" | awk '{s+=$1} END {printf "%.3f", s}')
    avg=$(echo "$sum $total" | awk '{printf "%.3f", $1/$2}')

    # Percentis
    local idx_50 idx_95 idx_99
    idx_50=$(echo "$total" | awk '{printf "%d", $1*0.5}')
    idx_95=$(echo "$total" | awk '{printf "%d", $1*0.95}')
    idx_99=$(echo "$total" | awk '{printf "%d", $1*0.99}')
    [ "$idx_50" -lt 1 ] && idx_50=1
    [ "$idx_95" -lt 1 ] && idx_95=1
    [ "$idx_99" -lt 1 ] && idx_99=1

    p50=$(echo "$sorted" | sed -n "${idx_50}p")
    p95=$(echo "$sorted" | sed -n "${idx_95}p")
    p99=$(echo "$sorted" | sed -n "${idx_99}p")

    local rate
    rate=$(echo "$success $total" | awk '{printf "%.1f", ($1/$2)*100}')

    echo "  Total:     $total requests"
    echo "  Sucesso:   $success ($rate%)"
    echo "  Falha:     $fail"
    echo "  ─────────────────────────────"
    echo "  Min:       ${min}s"
    echo "  Avg:       ${avg}s"
    echo "  P50:       ${p50}s"
    echo "  P95:       ${p95}s"
    echo "  P99:       ${p99}s"
    echo "  Max:       ${max}s"
}

# ── Início ────────────────────────────────────────────────────────────────────

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║        TUKAN API — TESTE DE ESTRESSE (Perfil + Triagem)    ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  Usuários:            ${NUM_USERS}                         "
echo "║  Requests/usuário:    ${RPU} (GET/PUT)                     "
echo "║  Base URL:            ${BASE_URL}                          "
echo "║  Resultados em:       ${RESULTS_DIR}                       "
echo "╚══════════════════════════════════════════════════════════════╝"

# ══════════════════════════════════════════════════════════════════════════════
#  FASE 0: Registrar usuários e obter tokens
# ══════════════════════════════════════════════════════════════════════════════

print_header "FASE 0: Registrando ${NUM_USERS} usuários e obtendo tokens"

REGISTER_OK=0
REGISTER_FAIL=0

for i in $(seq 1 "$NUM_USERS"); do
    EMAIL="stress${TIMESTAMP}u${i}@test.com"
    NOME="StressUser${i}"
    SENHA="stress123"

    # Registrar
    RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"nome\":\"${NOME}\",\"email\":\"${EMAIL}\",\"senha\":\"${SENHA}\"}" 2>/dev/null)

    HTTP_CODE=$(echo "$RESP" | tail -1)
    BODY=$(echo "$RESP" | sed '$d')

    if [[ "$HTTP_CODE" -ge 200 && "$HTTP_CODE" -lt 300 ]]; then
        TOKEN=$(extract_json_field "$BODY" "accessToken")
        if [ -n "$TOKEN" ]; then
            TOKENS+=("$TOKEN")
            REGISTER_OK=$((REGISTER_OK + 1))
        else
            REGISTER_FAIL=$((REGISTER_FAIL + 1))
        fi
    else
        REGISTER_FAIL=$((REGISTER_FAIL + 1))
    fi

    # Progresso a cada 10
    if [ $((i % 10)) -eq 0 ]; then
        echo "  Registrados: $i / $NUM_USERS"
    fi
done

echo ""
echo "  Registros OK:    $REGISTER_OK"
echo "  Registros FAIL:  $REGISTER_FAIL"
echo "  Tokens obtidos:  ${#TOKENS[@]}"

if [ "${#TOKENS[@]}" -eq 0 ]; then
    echo ""
    echo "ERRO: Nenhum token obtido. Verifique se a API está rodando em ${BASE_URL}"
    exit 1
fi

# ══════════════════════════════════════════════════════════════════════════════
#  FASE 1: Stress — POST /perfil/me (criação concorrente)
# ══════════════════════════════════════════════════════════════════════════════

print_header "FASE 1: POST /perfil/me — Criação concorrente de perfis"

PHASE_DIR="${RESULTS_DIR}/post-perfil"
mkdir -p "$PHASE_DIR"

SEXOS=("MASCULINO" "FEMININO")
NIVEIS=("SEDENTARIO" "LEVE" "MODERADO" "INTENSO" "MUITO_INTENSO")

for i in "${!TOKENS[@]}"; do
    TOKEN="${TOKENS[$i]}"
    SEXO="${SEXOS[$((RANDOM % 2))]}"
    NIVEL="${NIVEIS[$((RANDOM % 5))]}"
    PESO=$(( (RANDOM % 80) + 50 ))    # 50-129 kg
    ALTURA=$(( (RANDOM % 50) + 150 ))  # 150-199 cm
    ANO=$(( (RANDOM % 30) + 1970 ))
    MES=$(printf "%02d" $(( (RANDOM % 12) + 1 )))
    DIA=$(printf "%02d" $(( (RANDOM % 28) + 1 )))

    (
        RESULT=$(curl -s -o /dev/null -w "%{http_code}\n%{time_total}" \
            -X POST "${BASE_URL}/perfil/me" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${TOKEN}" \
            -d "{
                \"dataNascimento\": \"${ANO}-${MES}-${DIA}\",
                \"sexo\": \"${SEXO}\",
                \"pesoKg\": ${PESO}.0,
                \"alturaCm\": ${ALTURA}.0,
                \"nivelAtividade\": \"${NIVEL}\"
            }" 2>/dev/null)
        echo "$RESULT" > "${PHASE_DIR}/req_${i}.txt"
    ) &
done

wait
echo "  Disparados: ${#TOKENS[@]} requests em paralelo"
print_subheader "Resultados POST /perfil/me"
analyze_results "$PHASE_DIR" "POST /perfil/me"

# ══════════════════════════════════════════════════════════════════════════════
#  FASE 2: Stress — POST /triagem/me (criação concorrente)
# ══════════════════════════════════════════════════════════════════════════════

print_header "FASE 2: POST /triagem/me — Criação concorrente de triagens"

PHASE_DIR="${RESULTS_DIR}/post-triagem"
mkdir -p "$PHASE_DIR"

OBJETIVOS=("PERDA_DE_PESO" "GANHO_DE_MASSA" "MANUTENCAO" "REEDUCACAO_ALIMENTAR" "PERFORMANCE_ESPORTIVA")

for i in "${!TOKENS[@]}"; do
    TOKEN="${TOKENS[$i]}"
    OBJ="${OBJETIVOS[$((RANDOM % 5))]}"

    (
        RESULT=$(curl -s -o /dev/null -w "%{http_code}\n%{time_total}" \
            -X POST "${BASE_URL}/triagem/me" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${TOKEN}" \
            -d "{
                \"objetivo\": \"${OBJ}\",
                \"restricoesAlimentares\": \"Sem gluten, sem lactose\",
                \"alergias\": \"Amendoim\",
                \"condicoesSaude\": \"Nenhuma\"
            }" 2>/dev/null)
        echo "$RESULT" > "${PHASE_DIR}/req_${i}.txt"
    ) &
done

wait
echo "  Disparados: ${#TOKENS[@]} requests em paralelo"
print_subheader "Resultados POST /triagem/me"
analyze_results "$PHASE_DIR" "POST /triagem/me"

# ══════════════════════════════════════════════════════════════════════════════
#  FASE 3: Stress — GET /perfil/me (leitura concorrente)
# ══════════════════════════════════════════════════════════════════════════════

print_header "FASE 3: GET /perfil/me — ${RPU} requests/usuário em paralelo"

PHASE_DIR="${RESULTS_DIR}/get-perfil"
mkdir -p "$PHASE_DIR"

REQ_ID=0
for i in "${!TOKENS[@]}"; do
    TOKEN="${TOKENS[$i]}"
    for r in $(seq 1 "$RPU"); do
        (
            RESULT=$(curl -s -o /dev/null -w "%{http_code}\n%{time_total}" \
                -X GET "${BASE_URL}/perfil/me" \
                -H "Authorization: Bearer ${TOKEN}" 2>/dev/null)
            echo "$RESULT" > "${PHASE_DIR}/req_${REQ_ID}_${r}.txt"
        ) &
        REQ_ID=$((REQ_ID + 1))
    done
done

wait
TOTAL_GET_PERFIL=$((${#TOKENS[@]} * RPU))
echo "  Disparados: ${TOTAL_GET_PERFIL} requests em paralelo"
print_subheader "Resultados GET /perfil/me"
analyze_results "$PHASE_DIR" "GET /perfil/me"

# ══════════════════════════════════════════════════════════════════════════════
#  FASE 4: Stress — GET /triagem/me (leitura concorrente)
# ══════════════════════════════════════════════════════════════════════════════

print_header "FASE 4: GET /triagem/me — ${RPU} requests/usuário em paralelo"

PHASE_DIR="${RESULTS_DIR}/get-triagem"
mkdir -p "$PHASE_DIR"

REQ_ID=0
for i in "${!TOKENS[@]}"; do
    TOKEN="${TOKENS[$i]}"
    for r in $(seq 1 "$RPU"); do
        (
            RESULT=$(curl -s -o /dev/null -w "%{http_code}\n%{time_total}" \
                -X GET "${BASE_URL}/triagem/me" \
                -H "Authorization: Bearer ${TOKEN}" 2>/dev/null)
            echo "$RESULT" > "${PHASE_DIR}/req_${REQ_ID}_${r}.txt"
        ) &
        REQ_ID=$((REQ_ID + 1))
    done
done

wait
TOTAL_GET_TRIAGEM=$((${#TOKENS[@]} * RPU))
echo "  Disparados: ${TOTAL_GET_TRIAGEM} requests em paralelo"
print_subheader "Resultados GET /triagem/me"
analyze_results "$PHASE_DIR" "GET /triagem/me"

# ══════════════════════════════════════════════════════════════════════════════
#  FASE 5: Stress — PUT /perfil/me (atualização concorrente)
# ══════════════════════════════════════════════════════════════════════════════

print_header "FASE 5: PUT /perfil/me — ${RPU} atualizações/usuário em paralelo"

PHASE_DIR="${RESULTS_DIR}/put-perfil"
mkdir -p "$PHASE_DIR"

REQ_ID=0
for i in "${!TOKENS[@]}"; do
    TOKEN="${TOKENS[$i]}"
    for r in $(seq 1 "$RPU"); do
        PESO=$(( (RANDOM % 80) + 50 ))
        NIVEL="${NIVEIS[$((RANDOM % 5))]}"
        (
            RESULT=$(curl -s -o /dev/null -w "%{http_code}\n%{time_total}" \
                -X PUT "${BASE_URL}/perfil/me" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer ${TOKEN}" \
                -d "{
                    \"pesoKg\": ${PESO}.0,
                    \"nivelAtividade\": \"${NIVEL}\"
                }" 2>/dev/null)
            echo "$RESULT" > "${PHASE_DIR}/req_${REQ_ID}_${r}.txt"
        ) &
        REQ_ID=$((REQ_ID + 1))
    done
done

wait
TOTAL_PUT_PERFIL=$((${#TOKENS[@]} * RPU))
echo "  Disparados: ${TOTAL_PUT_PERFIL} requests em paralelo"
print_subheader "Resultados PUT /perfil/me"
analyze_results "$PHASE_DIR" "PUT /perfil/me"

# ══════════════════════════════════════════════════════════════════════════════
#  FASE 6: Stress — PUT /triagem/me (atualização concorrente)
# ══════════════════════════════════════════════════════════════════════════════

print_header "FASE 6: PUT /triagem/me — ${RPU} atualizações/usuário em paralelo"

PHASE_DIR="${RESULTS_DIR}/put-triagem"
mkdir -p "$PHASE_DIR"

REQ_ID=0
for i in "${!TOKENS[@]}"; do
    TOKEN="${TOKENS[$i]}"
    for r in $(seq 1 "$RPU"); do
        OBJ="${OBJETIVOS[$((RANDOM % 5))]}"
        (
            RESULT=$(curl -s -o /dev/null -w "%{http_code}\n%{time_total}" \
                -X PUT "${BASE_URL}/triagem/me" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer ${TOKEN}" \
                -d "{
                    \"restricoesAlimentares\": \"Stress test round ${r}\",
                    \"alergias\": \"Nenhuma\",
                    \"condicoesSaude\": \"Saudavel\"
                }" 2>/dev/null)
            echo "$RESULT" > "${PHASE_DIR}/req_${REQ_ID}_${r}.txt"
        ) &
        REQ_ID=$((REQ_ID + 1))
    done
done

wait
TOTAL_PUT_TRIAGEM=$((${#TOKENS[@]} * RPU))
echo "  Disparados: ${TOTAL_PUT_TRIAGEM} requests em paralelo"
print_subheader "Resultados PUT /triagem/me"
analyze_results "$PHASE_DIR" "PUT /triagem/me"

# ══════════════════════════════════════════════════════════════════════════════
#  RESUMO FINAL
# ══════════════════════════════════════════════════════════════════════════════

TOTAL_REQUESTS=$((${#TOKENS[@]} + ${#TOKENS[@]} + TOTAL_GET_PERFIL + TOTAL_GET_TRIAGEM + TOTAL_PUT_PERFIL + TOTAL_PUT_TRIAGEM))

print_header "RESUMO FINAL"
echo ""
echo "  Usuários criados:     ${#TOKENS[@]}"
echo "  Total de requests:    ${TOTAL_REQUESTS}"
echo "  Resultados salvos:    ${RESULTS_DIR}"
echo ""
echo "  Breakdown por fase:"
echo "    POST /perfil/me:    ${#TOKENS[@]} requests"
echo "    POST /triagem/me:   ${#TOKENS[@]} requests"
echo "    GET  /perfil/me:    ${TOTAL_GET_PERFIL} requests"
echo "    GET  /triagem/me:   ${TOTAL_GET_TRIAGEM} requests"
echo "    PUT  /perfil/me:    ${TOTAL_PUT_PERFIL} requests"
echo "    PUT  /triagem/me:   ${TOTAL_PUT_TRIAGEM} requests"
echo ""
echo "  Teste concluído!"
echo ""
