#!/bin/bash
# =============================================================================
# Script de testes manuais da API Tukan - Auth + Users + Sessions
# Testa todos os caminhos felizes e nao-felizes
# Cobertura completa: registro, login, refresh, logout, autorizacao, banco
# =============================================================================

BASE="http://localhost:8080"
PASS=0
FAIL=0
TOTAL=0
TIMESTAMP=$(date +%s)
TEST_EMAIL="teste_${TIMESTAMP}@tukan.com"
TEST_EMAIL2="teste2_${TIMESTAMP}@tukan.com"
MYSQL_CMD="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe"

# Cores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

assert_status() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    local body="$4"
    TOTAL=$((TOTAL + 1))

    if [ "$actual" = "$expected" ]; then
        PASS=$((PASS + 1))
        echo -e "${GREEN}[PASS]${NC} $test_name (HTTP $actual)"
    else
        FAIL=$((FAIL + 1))
        echo -e "${RED}[FAIL]${NC} $test_name (esperado: $expected, recebido: $actual)"
        echo -e "       Body: $body"
    fi
}

assert_status_and_field() {
    local test_name="$1"
    local expected_status="$2"
    local actual_status="$3"
    local body="$4"
    local field="$5"
    TOTAL=$((TOTAL + 1))

    if [ "$actual_status" != "$expected_status" ]; then
        FAIL=$((FAIL + 1))
        echo -e "${RED}[FAIL]${NC} $test_name (esperado HTTP $expected_status, recebido: $actual_status)"
        echo -e "       Body: $body"
        return
    fi

    local has_field
    has_field=$(echo "$body" | grep -o "\"$field\"")
    if [ -n "$has_field" ]; then
        PASS=$((PASS + 1))
        echo -e "${GREEN}[PASS]${NC} $test_name (HTTP $actual_status, campo '$field' presente)"
    else
        FAIL=$((FAIL + 1))
        echo -e "${RED}[FAIL]${NC} $test_name (HTTP $actual_status, campo '$field' AUSENTE)"
        echo -e "       Body: $body"
    fi
}

assert_body_contains() {
    local test_name="$1"
    local expected_status="$2"
    local actual_status="$3"
    local body="$4"
    local substring="$5"
    TOTAL=$((TOTAL + 1))

    if [ "$actual_status" != "$expected_status" ]; then
        FAIL=$((FAIL + 1))
        echo -e "${RED}[FAIL]${NC} $test_name (esperado HTTP $expected_status, recebido: $actual_status)"
        echo -e "       Body: $body"
        return
    fi

    if echo "$body" | grep -q "$substring"; then
        PASS=$((PASS + 1))
        echo -e "${GREEN}[PASS]${NC} $test_name (HTTP $actual_status, body contem '$substring')"
    else
        FAIL=$((FAIL + 1))
        echo -e "${RED}[FAIL]${NC} $test_name (HTTP $actual_status, body NAO contem '$substring')"
        echo -e "       Body: $body"
    fi
}

do_request() {
    local method="$1"
    local url="$2"
    local data="$3"
    local token="$4"
    local extra_headers="$5"

    local args=(-s -w "\n__HTTP_STATUS__%{http_code}" -X "$method")
    args+=(-H "Content-Type: application/json")

    if [ -n "$token" ]; then
        args+=(-H "Authorization: Bearer $token")
    fi
    if [ -n "$extra_headers" ]; then
        args+=(-H "$extra_headers")
    fi
    if [ -n "$data" ]; then
        args+=(-d "$data")
    fi

    curl "${args[@]}" "$url" 2>/dev/null
}

parse_response() {
    local raw="$1"
    BODY=$(echo "$raw" | sed '$d' | tr -d '\n')
    STATUS=$(echo "$raw" | grep "__HTTP_STATUS__" | sed 's/__HTTP_STATUS__//')
}

extract_json_field() {
    local json="$1"
    local field="$2"
    echo "$json" | sed -n "s/.*\"$field\":\s*\"\\([^\"]*\\)\".*/\\1/p"
}

db_query() {
    "$MYSQL_CMD" -u root -pLucas_123 -N -e "$1" tukan_db 2>/dev/null
}

echo ""
echo -e "${CYAN}=================================================================${NC}"
echo -e "${CYAN}  TUKAN API - Bateria Completa de Testes${NC}"
echo -e "${CYAN}  $(date)${NC}"
echo -e "${CYAN}=================================================================${NC}"
echo ""

# =========================================================
echo -e "${YELLOW}=== A. REGISTRO - Caminhos nao-felizes ===${NC}"
# =========================================================

# T01: Corpo vazio
parse_response "$(do_request POST "$BASE/auth/register" '{}')"
assert_status "T01: Register com corpo vazio" "400" "$STATUS" "$BODY"

# T02: Email invalido (sem @)
parse_response "$(do_request POST "$BASE/auth/register" '{"nome":"Teste","email":"invalido","senha":"123456"}')"
assert_status "T02: Register com email invalido" "400" "$STATUS" "$BODY"

# T03: Senha curta (< 6)
parse_response "$(do_request POST "$BASE/auth/register" '{"nome":"Teste","email":"x@x.com","senha":"123"}')"
assert_status "T03: Register com senha curta" "400" "$STATUS" "$BODY"

# T04: Nome em branco
parse_response "$(do_request POST "$BASE/auth/register" '{"nome":"","email":"x@x.com","senha":"123456"}')"
assert_status "T04: Register com nome em branco" "400" "$STATUS" "$BODY"

# T05: Sem campo email
parse_response "$(do_request POST "$BASE/auth/register" '{"nome":"Teste","senha":"123456"}')"
assert_status "T05: Register sem campo email" "400" "$STATUS" "$BODY"

# T06: Nome com mais de 100 caracteres
LONG_NAME=$(python3 -c "print('A' * 101)" 2>/dev/null || python -c "print('A' * 101)" 2>/dev/null || printf 'A%.0s' {1..101})
parse_response "$(do_request POST "$BASE/auth/register" "{\"nome\":\"$LONG_NAME\",\"email\":\"long@test.com\",\"senha\":\"123456\"}")"
assert_status "T06: Register com nome > 100 chars" "400" "$STATUS" "$BODY"

# T07: Sem campo senha
parse_response "$(do_request POST "$BASE/auth/register" '{"nome":"Teste","email":"x@x.com"}')"
assert_status "T07: Register sem campo senha" "400" "$STATUS" "$BODY"

# T08: Sem campo nome
parse_response "$(do_request POST "$BASE/auth/register" '{"email":"x@x.com","senha":"123456"}')"
assert_status "T08: Register sem campo nome" "400" "$STATUS" "$BODY"

# T09: JSON malformado
parse_response "$(do_request POST "$BASE/auth/register" 'nao e json')"
assert_status "T09: Register com JSON malformado" "400" "$STATUS" "$BODY"

# T10: Validar formato ErrorResponse no 400
assert_status_and_field "T10: ErrorResponse tem campo 'status' no 400" "400" "$STATUS" "$BODY" "status"

# =========================================================
echo ""
echo -e "${YELLOW}=== A. REGISTRO - Caminho feliz ===${NC}"
# =========================================================

# T11: Registro valido
parse_response "$(do_request POST "$BASE/auth/register" "{\"nome\":\"Teste Tukan\",\"email\":\"$TEST_EMAIL\",\"senha\":\"senha123\"}")"
assert_status_and_field "T11: Register valido" "201" "$STATUS" "$BODY" "accessToken"
REG_ACCESS=$(extract_json_field "$BODY" "accessToken")
REG_REFRESH=$(extract_json_field "$BODY" "refreshToken")

# T12: Verificar que accessToken nao esta vazio
TOTAL=$((TOTAL + 1))
if [ -n "$REG_ACCESS" ] && [ ${#REG_ACCESS} -gt 20 ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T12: Access token recebido (${#REG_ACCESS} chars)"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T12: Access token vazio ou invalido"
fi

# T13: Verificar que refreshToken nao esta vazio
TOTAL=$((TOTAL + 1))
if [ -n "$REG_REFRESH" ] && [ ${#REG_REFRESH} -gt 10 ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T13: Refresh token recebido (${#REG_REFRESH} chars)"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T13: Refresh token vazio ou invalido"
fi

# T14: Verificar campo type = Bearer
TOTAL=$((TOTAL + 1))
TOKEN_TYPE=$(extract_json_field "$BODY" "type")
if [ "$TOKEN_TYPE" = "Bearer" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T14: Token type = Bearer"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T14: Token type esperado 'Bearer', recebido '$TOKEN_TYPE'"
fi

# T15: Verificar campo message
assert_body_contains "T15: Register retorna mensagem de sucesso" "201" "$STATUS" "$BODY" "sucesso"

# =========================================================
echo ""
echo -e "${YELLOW}=== A. REGISTRO - Duplicidade e normalizacao ===${NC}"
# =========================================================

# T16: Registro com email duplicado
parse_response "$(do_request POST "$BASE/auth/register" "{\"nome\":\"Outro\",\"email\":\"$TEST_EMAIL\",\"senha\":\"senha123\"}")"
assert_status "T16: Register com email duplicado" "409" "$STATUS" "$BODY"

# T17: Registro com email em maiusculas (deve normalizar e conflitar)
UPPER_EMAIL=$(echo "$TEST_EMAIL" | tr '[:lower:]' '[:upper:]')
parse_response "$(do_request POST "$BASE/auth/register" "{\"nome\":\"Outro\",\"email\":\"$UPPER_EMAIL\",\"senha\":\"senha123\"}")"
assert_status "T17: Register com email uppercase (deve conflitar)" "409" "$STATUS" "$BODY"

# T18: Registro com email com espacos (validacao rejeita antes do trim)
parse_response "$(do_request POST "$BASE/auth/register" "{\"nome\":\"Outro\",\"email\":\"  $TEST_EMAIL  \",\"senha\":\"senha123\"}")"
assert_status "T18: Register com email com espacos (formato invalido)" "400" "$STATUS" "$BODY"

# =========================================================
echo ""
echo -e "${YELLOW}=== F. BANCO - Verificacao pos-registro ===${NC}"
# =========================================================

# T19: Verificar usuario no banco
TOTAL=$((TOTAL + 1))
DB_USER_COUNT=$(db_query "SELECT COUNT(*) FROM usuario WHERE email='$(echo $TEST_EMAIL | tr '[:upper:]' '[:lower:]')';")
if [ "$DB_USER_COUNT" = "1" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T19: Usuario salvo no banco corretamente"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T19: Usuario nao encontrado no banco (count=$DB_USER_COUNT)"
fi

# T20: Verificar tipo e status do usuario no banco
TOTAL=$((TOTAL + 1))
DB_USER_INFO=$(db_query "SELECT tipo, status FROM usuario WHERE email='$(echo $TEST_EMAIL | tr '[:upper:]' '[:lower:]')';")
if echo "$DB_USER_INFO" | grep -q "USER" && echo "$DB_USER_INFO" | grep -q "ATIVO"; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T20: Usuario com tipo=USER e status=ATIVO no banco"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T20: Tipo/status incorreto no banco: $DB_USER_INFO"
fi

# T21: Verificar sessao criada no registro
TOTAL=$((TOTAL + 1))
DB_SESSION_COUNT=$(db_query "SELECT COUNT(*) FROM sessao_usuario su JOIN usuario u ON su.usuario_id = u.id WHERE u.email='$(echo $TEST_EMAIL | tr '[:upper:]' '[:lower:]')';")
if [ "$DB_SESSION_COUNT" -ge "1" ] 2>/dev/null; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T21: Sessao criada no banco apos registro (count=$DB_SESSION_COUNT)"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T21: Sessao NAO encontrada no banco apos registro (count=$DB_SESSION_COUNT)"
fi

# T22: Verificar que senha esta hashada (nao e o texto plano)
TOTAL=$((TOTAL + 1))
DB_SENHA=$(db_query "SELECT senha FROM usuario WHERE email='$(echo $TEST_EMAIL | tr '[:upper:]' '[:lower:]')';")
if echo "$DB_SENHA" | grep -q '^\$2' ; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T22: Senha armazenada com BCrypt hash"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T22: Senha NAO esta com BCrypt hash: $DB_SENHA"
fi

# =========================================================
echo ""
echo -e "${YELLOW}=== B. LOGIN - Caminhos nao-felizes ===${NC}"
# =========================================================

# T23: Login sem corpo
parse_response "$(do_request POST "$BASE/auth/login" '{}')"
assert_status "T23: Login com corpo vazio" "400" "$STATUS" "$BODY"

# T24: Login com email inexistente
parse_response "$(do_request POST "$BASE/auth/login" '{"email":"naoexiste@tukan.com","senha":"senha123"}')"
assert_status "T24: Login com email inexistente" "401" "$STATUS" "$BODY"

# T25: Login com senha errada
parse_response "$(do_request POST "$BASE/auth/login" "{\"email\":\"$TEST_EMAIL\",\"senha\":\"senhaERRADA\"}")"
assert_status "T25: Login com senha errada" "401" "$STATUS" "$BODY"

# T26: Login com email invalido (validacao)
parse_response "$(do_request POST "$BASE/auth/login" '{"email":"invalido","senha":"senha123"}')"
assert_status "T26: Login com email invalido (formato)" "400" "$STATUS" "$BODY"

# T27: Login com JSON malformado
parse_response "$(do_request POST "$BASE/auth/login" 'isso nao e json')"
assert_status "T27: Login com JSON malformado" "400" "$STATUS" "$BODY"

# T28: Login sem campo senha
parse_response "$(do_request POST "$BASE/auth/login" '{"email":"teste@teste.com"}')"
assert_status "T28: Login sem campo senha" "400" "$STATUS" "$BODY"

# =========================================================
echo ""
echo -e "${YELLOW}=== C. LOGIN - Caminho feliz ===${NC}"
# =========================================================

# T29: Login valido
parse_response "$(do_request POST "$BASE/auth/login" "{\"email\":\"$TEST_EMAIL\",\"senha\":\"senha123\"}")"
assert_status_and_field "T29: Login valido" "200" "$STATUS" "$BODY" "accessToken"
LOGIN_ACCESS=$(extract_json_field "$BODY" "accessToken")
LOGIN_REFRESH=$(extract_json_field "$BODY" "refreshToken")

# T30: Login com email em maiusculas (normalizacao)
parse_response "$(do_request POST "$BASE/auth/login" "{\"email\":\"$UPPER_EMAIL\",\"senha\":\"senha123\"}")"
assert_status_and_field "T30: Login com email uppercase" "200" "$STATUS" "$BODY" "accessToken"

# T31: Login retorna mensagem de sucesso
assert_body_contains "T31: Login retorna mensagem de sucesso" "200" "$STATUS" "$BODY" "sucesso"

# T32: Verificar nova sessao criada apos login
TOTAL=$((TOTAL + 1))
DB_SESSION_COUNT_AFTER_LOGIN=$(db_query "SELECT COUNT(*) FROM sessao_usuario su JOIN usuario u ON su.usuario_id = u.id WHERE u.email='$(echo $TEST_EMAIL | tr '[:upper:]' '[:lower:]')';")
if [ "$DB_SESSION_COUNT_AFTER_LOGIN" -gt "$DB_SESSION_COUNT" ] 2>/dev/null; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T32: Nova sessao criada no banco apos login (antes=$DB_SESSION_COUNT, depois=$DB_SESSION_COUNT_AFTER_LOGIN)"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T32: Sessao NAO foi criada apos login (antes=$DB_SESSION_COUNT, depois=$DB_SESSION_COUNT_AFTER_LOGIN)"
fi

# =========================================================
echo ""
echo -e "${YELLOW}=== D. REFRESH TOKEN - Caminhos nao-felizes ===${NC}"
# =========================================================

# T33: Refresh sem corpo
parse_response "$(do_request POST "$BASE/auth/refresh" '{}')"
assert_status "T33: Refresh com corpo vazio" "400" "$STATUS" "$BODY"

# T34: Refresh com token invalido
parse_response "$(do_request POST "$BASE/auth/refresh" '{"refreshToken":"token_invalido_qualquer"}')"
assert_status "T34: Refresh com token invalido" "401" "$STATUS" "$BODY"

# T35: Refresh com JSON malformado
parse_response "$(do_request POST "$BASE/auth/refresh" 'nao e json')"
assert_status "T35: Refresh com JSON malformado" "400" "$STATUS" "$BODY"

# =========================================================
echo ""
echo -e "${YELLOW}=== D. REFRESH TOKEN - Caminho feliz ===${NC}"
# =========================================================

# T36: Refresh valido (usando o token do login)
parse_response "$(do_request POST "$BASE/auth/refresh" "{\"refreshToken\":\"$LOGIN_REFRESH\"}")"
assert_status_and_field "T36: Refresh valido" "200" "$STATUS" "$BODY" "accessToken"
NEW_ACCESS=$(extract_json_field "$BODY" "accessToken")
NEW_REFRESH=$(extract_json_field "$BODY" "refreshToken")

# T37: Novo access token e diferente do anterior
TOTAL=$((TOTAL + 1))
if [ "$NEW_ACCESS" != "$LOGIN_ACCESS" ] && [ -n "$NEW_ACCESS" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T37: Novo access token e diferente do anterior"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T37: Access tokens deveriam ser diferentes"
fi

# T38: Novo refresh token e diferente do anterior
TOTAL=$((TOTAL + 1))
if [ "$NEW_REFRESH" != "$LOGIN_REFRESH" ] && [ -n "$NEW_REFRESH" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T38: Novo refresh token e diferente do anterior (rotacao)"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T38: Refresh tokens deveriam ser diferentes (rotacao)"
fi

# =========================================================
echo ""
echo -e "${YELLOW}=== D. REFRESH TOKEN - Deteccao de roubo ===${NC}"
# =========================================================

# T39: Reuso do refresh token antigo (deve revogar tudo - deteccao de roubo)
parse_response "$(do_request POST "$BASE/auth/refresh" "{\"refreshToken\":\"$LOGIN_REFRESH\"}")"
assert_status "T39: Reuso de refresh token (deteccao de roubo)" "401" "$STATUS" "$BODY"

# T40: O novo refresh token tambem deve ser revogado (todas as sessoes foram revogadas)
parse_response "$(do_request POST "$BASE/auth/refresh" "{\"refreshToken\":\"$NEW_REFRESH\"}")"
assert_status "T40: Novo refresh apos revogacao em massa" "401" "$STATUS" "$BODY"

# T41: Verificar no banco que sessoes foram revogadas
TOTAL=$((TOTAL + 1))
DB_ACTIVE=$(db_query "SELECT COUNT(*) FROM sessao_usuario su JOIN usuario u ON su.usuario_id = u.id WHERE u.email='$(echo $TEST_EMAIL | tr '[:upper:]' '[:lower:]')' AND su.revogado_em IS NULL;")
if [ "$DB_ACTIVE" = "0" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T41: Todas as sessoes revogadas no banco apos deteccao de roubo"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T41: Ainda ha $DB_ACTIVE sessoes ativas no banco"
fi

# =========================================================
echo ""
echo -e "${YELLOW}=== E. ACESSO A ENDPOINTS PROTEGIDOS ===${NC}"
# =========================================================

# Fazer novo login para ter tokens validos
parse_response "$(do_request POST "$BASE/auth/login" "{\"email\":\"$TEST_EMAIL\",\"senha\":\"senha123\"}")"
VALID_ACCESS=$(extract_json_field "$BODY" "accessToken")
VALID_REFRESH=$(extract_json_field "$BODY" "refreshToken")

# T42: Acesso sem token (401)
parse_response "$(do_request GET "$BASE/users")"
assert_status "T42: GET /users sem token" "401" "$STATUS" "$BODY"

# T43: Resposta 401 tem formato ErrorResponse
assert_status_and_field "T43: 401 retorna ErrorResponse com campo 'status'" "401" "$STATUS" "$BODY" "status"

# T44: Acesso com token invalido (401)
parse_response "$(do_request GET "$BASE/users" "" "token.invalido.aqui")"
assert_status "T44: GET /users com token invalido" "401" "$STATUS" "$BODY"

# T45: Acesso com token valido mas sem role ADMIN (403)
parse_response "$(do_request GET "$BASE/users" "" "$VALID_ACCESS")"
assert_status "T45: GET /users com USER (sem ADMIN)" "403" "$STATUS" "$BODY"

# T46: Resposta 403 tem formato ErrorResponse
assert_status_and_field "T46: 403 retorna ErrorResponse com campo 'status'" "403" "$STATUS" "$BODY" "status"

# T47: Acesso a rota especifica de user sem token
parse_response "$(do_request GET "$BASE/users/1")"
assert_status "T47: GET /users/1 sem token" "401" "$STATUS" "$BODY"

# T48: Acesso a endpoint inexistente sem token (deve ser 401, nao 404)
parse_response "$(do_request GET "$BASE/api/nao-existe")"
assert_status "T48: Endpoint inexistente sem token" "401" "$STATUS" "$BODY"

# =========================================================
echo ""
echo -e "${YELLOW}=== E. LOGOUT ===${NC}"
# =========================================================

# T49: Logout sem corpo
parse_response "$(do_request POST "$BASE/auth/logout" '{}')"
assert_status "T49: Logout com corpo vazio" "400" "$STATUS" "$BODY"

# T50: Logout com token invalido
parse_response "$(do_request POST "$BASE/auth/logout" '{"refreshToken":"token_invalido"}')"
assert_status "T50: Logout com token invalido" "401" "$STATUS" "$BODY"

# T51: Logout valido
parse_response "$(do_request POST "$BASE/auth/logout" "{\"refreshToken\":\"$VALID_REFRESH\"}")"
assert_status "T51: Logout valido" "204" "$STATUS" "$BODY"

# T52: Refresh apos logout (token revogado)
parse_response "$(do_request POST "$BASE/auth/refresh" "{\"refreshToken\":\"$VALID_REFRESH\"}")"
assert_status "T52: Refresh apos logout" "401" "$STATUS" "$BODY"

# T53: Logout idempotente (mesmo token novamente)
parse_response "$(do_request POST "$BASE/auth/logout" "{\"refreshToken\":\"$VALID_REFRESH\"}")"
assert_status "T53: Logout idempotente (token ja revogado)" "204" "$STATUS" "$BODY"

# T54: Verificar sessao revogada no banco
TOTAL=$((TOTAL + 1))
DB_REVOKED=$(db_query "SELECT COUNT(*) FROM sessao_usuario su JOIN usuario u ON su.usuario_id = u.id WHERE u.email='$(echo $TEST_EMAIL | tr '[:upper:]' '[:lower:]')' AND su.revogado_em IS NOT NULL;")
DB_TOTAL_S=$(db_query "SELECT COUNT(*) FROM sessao_usuario su JOIN usuario u ON su.usuario_id = u.id WHERE u.email='$(echo $TEST_EMAIL | tr '[:upper:]' '[:lower:]')';")
if [ "$DB_REVOKED" = "$DB_TOTAL_S" ] && [ "$DB_TOTAL_S" -gt "0" ] 2>/dev/null; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T54: Todas as $DB_TOTAL_S sessoes estao revogadas no banco"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T54: Revogadas=$DB_REVOKED / Total=$DB_TOTAL_S"
fi

# =========================================================
echo ""
echo -e "${YELLOW}=== E. FLUXO COMPLETO: Register -> Login -> Refresh -> Logout ===${NC}"
# =========================================================

# T55: Registro de segundo usuario
parse_response "$(do_request POST "$BASE/auth/register" "{\"nome\":\"User Dois\",\"email\":\"$TEST_EMAIL2\",\"senha\":\"senha456\"}")"
assert_status_and_field "T55: Register segundo usuario" "201" "$STATUS" "$BODY" "accessToken"
U2_ACCESS=$(extract_json_field "$BODY" "accessToken")
U2_REFRESH=$(extract_json_field "$BODY" "refreshToken")

# T56: Login do segundo usuario (apos registro)
parse_response "$(do_request POST "$BASE/auth/login" "{\"email\":\"$TEST_EMAIL2\",\"senha\":\"senha456\"}")"
assert_status_and_field "T56: Login segundo usuario" "200" "$STATUS" "$BODY" "accessToken"
U2_LOGIN_REFRESH=$(extract_json_field "$BODY" "refreshToken")

# T57: Refresh do segundo usuario
parse_response "$(do_request POST "$BASE/auth/refresh" "{\"refreshToken\":\"$U2_LOGIN_REFRESH\"}")"
assert_status_and_field "T57: Refresh segundo usuario" "200" "$STATUS" "$BODY" "accessToken"
U2_NEW_REFRESH=$(extract_json_field "$BODY" "refreshToken")

# T58: Logout do segundo usuario
parse_response "$(do_request POST "$BASE/auth/logout" "{\"refreshToken\":\"$U2_NEW_REFRESH\"}")"
assert_status "T58: Logout segundo usuario" "204" "$STATUS" "$BODY"

# T59: Refresh apos logout do segundo usuario
parse_response "$(do_request POST "$BASE/auth/refresh" "{\"refreshToken\":\"$U2_NEW_REFRESH\"}")"
assert_status "T59: Refresh apos logout do segundo user" "401" "$STATUS" "$BODY"

# T60: Segundo usuario no banco
TOTAL=$((TOTAL + 1))
DB_U2=$(db_query "SELECT COUNT(*) FROM usuario WHERE email='$(echo $TEST_EMAIL2 | tr '[:upper:]' '[:lower:]')';")
if [ "$DB_U2" = "1" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T60: Segundo usuario salvo no banco"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T60: Segundo usuario NAO encontrado no banco (count=$DB_U2)"
fi

# =========================================================
echo ""
echo -e "${YELLOW}=== E. SESSOES MULTIPLAS ===${NC}"
# =========================================================

# Novo login para ter token valido
parse_response "$(do_request POST "$BASE/auth/login" "{\"email\":\"$TEST_EMAIL\",\"senha\":\"senha123\"}")"
MULTI_REFRESH1=$(extract_json_field "$BODY" "refreshToken")

parse_response "$(do_request POST "$BASE/auth/login" "{\"email\":\"$TEST_EMAIL\",\"senha\":\"senha123\"}")"
MULTI_REFRESH2=$(extract_json_field "$BODY" "refreshToken")

# T61: Multiplos logins geram refresh tokens distintos
TOTAL=$((TOTAL + 1))
if [ "$MULTI_REFRESH1" != "$MULTI_REFRESH2" ] && [ -n "$MULTI_REFRESH1" ] && [ -n "$MULTI_REFRESH2" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T61: Multiplos logins geram refresh tokens distintos"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T61: Multiplos logins devem gerar tokens distintos"
fi

# T62/T63: Ambas sessoes funcionam independentemente
parse_response "$(do_request POST "$BASE/auth/refresh" "{\"refreshToken\":\"$MULTI_REFRESH1\"}")"
assert_status_and_field "T62: Refresh sessao 1 funciona" "200" "$STATUS" "$BODY" "accessToken"

parse_response "$(do_request POST "$BASE/auth/refresh" "{\"refreshToken\":\"$MULTI_REFRESH2\"}")"
assert_status_and_field "T63: Refresh sessao 2 funciona" "200" "$STATUS" "$BODY" "accessToken"

# =========================================================
echo ""
echo -e "${YELLOW}=== E. EDGE CASES ===${NC}"
# =========================================================

# T64: Content-Type errado
TOTAL=$((TOTAL + 1))
RAW=$(curl -s -w "\n__HTTP_STATUS__%{http_code}" -X POST "$BASE/auth/login" -H "Content-Type: text/plain" -d "nao e json" 2>/dev/null)
BODY=$(echo "$RAW" | sed '$d' | tr -d '\n')
STATUS=$(echo "$RAW" | grep "__HTTP_STATUS__" | sed 's/__HTTP_STATUS__//')
if [ "$STATUS" = "415" ] || [ "$STATUS" = "400" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T64: Content-Type errado (HTTP $STATUS)"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T64: Content-Type errado (esperado: 415 ou 400, recebido: $STATUS)"
fi

# T65: Metodo GET em endpoint POST
parse_response "$(do_request GET "$BASE/auth/login")"
TOTAL=$((TOTAL + 1))
if [ "$STATUS" = "405" ] || [ "$STATUS" = "401" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T65: GET em endpoint POST (HTTP $STATUS)"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T65: GET em endpoint POST (esperado 405, recebido: $STATUS)"
fi

# T66: Body vazio (sem dados nenhum)
TOTAL=$((TOTAL + 1))
RAW=$(curl -s -w "\n__HTTP_STATUS__%{http_code}" -X POST "$BASE/auth/register" -H "Content-Type: application/json" 2>/dev/null)
BODY=$(echo "$RAW" | sed '$d' | tr -d '\n')
STATUS=$(echo "$RAW" | grep "__HTTP_STATUS__" | sed 's/__HTTP_STATUS__//')
if [ "$STATUS" = "400" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T66: Request sem body (HTTP $STATUS)"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T66: Request sem body (esperado: 400, recebido: $STATUS)"
fi

# T67: Senha com exatamente 6 caracteres (limite minimo - deve funcionar)
EDGE_EMAIL="edge_${TIMESTAMP}@tukan.com"
parse_response "$(do_request POST "$BASE/auth/register" "{\"nome\":\"Edge\",\"email\":\"$EDGE_EMAIL\",\"senha\":\"123456\"}")"
assert_status_and_field "T67: Register com senha de 6 chars (minimo)" "201" "$STATUS" "$BODY" "accessToken"

# T68: Nome com exatamente 100 caracteres (limite maximo - deve funcionar)
NAME_100=$(python3 -c "print('B' * 100)" 2>/dev/null || python -c "print('B' * 100)" 2>/dev/null || printf 'B%.0s' {1..100})
EDGE_EMAIL2="edge2_${TIMESTAMP}@tukan.com"
parse_response "$(do_request POST "$BASE/auth/register" "{\"nome\":\"$NAME_100\",\"email\":\"$EDGE_EMAIL2\",\"senha\":\"123456\"}")"
assert_status_and_field "T68: Register com nome de 100 chars (maximo)" "201" "$STATUS" "$BODY" "accessToken"

# T69: Senha com 5 caracteres (abaixo do minimo)
parse_response "$(do_request POST "$BASE/auth/register" '{"nome":"Teste","email":"short@test.com","senha":"12345"}')"
assert_status "T69: Register com senha de 5 chars (abaixo do minimo)" "400" "$STATUS" "$BODY"

# =========================================================
echo ""
echo -e "${YELLOW}=== F. CONSISTENCIA DE BANCO - Verificacao final ===${NC}"
# =========================================================

# T70: Contar total de usuarios criados neste teste
TOTAL=$((TOTAL + 1))
DB_TEST_USERS=$(db_query "SELECT COUNT(*) FROM usuario WHERE email LIKE '%${TIMESTAMP}%';")
EXPECTED_USERS=4  # TEST_EMAIL, TEST_EMAIL2, edge, edge2
if [ "$DB_TEST_USERS" = "$EXPECTED_USERS" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T70: Total de usuarios de teste no banco = $DB_TEST_USERS (esperado $EXPECTED_USERS)"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T70: Total de usuarios de teste = $DB_TEST_USERS (esperado $EXPECTED_USERS)"
fi

# T71: Nenhum usuario de teste tem senha em texto plano
TOTAL=$((TOTAL + 1))
DB_PLAIN=$(db_query "SELECT COUNT(*) FROM usuario WHERE email LIKE '%${TIMESTAMP}%' AND senha NOT LIKE '\$2%';")
if [ "$DB_PLAIN" = "0" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T71: Todos os usuarios de teste tem senha BCrypt"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T71: $DB_PLAIN usuario(s) com senha em texto plano!"
fi

# T72: Todas sessoes dos usuarios de teste tem hash de token
TOTAL=$((TOTAL + 1))
DB_NO_HASH=$(db_query "SELECT COUNT(*) FROM sessao_usuario su JOIN usuario u ON su.usuario_id = u.id WHERE u.email LIKE '%${TIMESTAMP}%' AND (su.refresh_token_hash IS NULL OR LENGTH(su.refresh_token_hash) != 64);")
if [ "$DB_NO_HASH" = "0" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}[PASS]${NC} T72: Todos os refresh tokens armazenados como SHA-256 (64 chars hex)"
else
    FAIL=$((FAIL + 1))
    echo -e "${RED}[FAIL]${NC} T72: $DB_NO_HASH sessoes sem hash valido"
fi

# =========================================================
echo ""
echo -e "${CYAN}=================================================================${NC}"
echo -e "${CYAN}  RESULTADOS${NC}"
echo -e "${CYAN}=================================================================${NC}"
echo ""
echo -e "  Total:  $TOTAL"
echo -e "  ${GREEN}Pass:   $PASS${NC}"
echo -e "  ${RED}Fail:   $FAIL${NC}"
echo ""

if [ "$FAIL" -eq 0 ]; then
    echo -e "${GREEN}  *** TODOS OS TESTES PASSARAM ***${NC}"
else
    echo -e "${RED}  *** $FAIL TESTE(S) FALHARAM ***${NC}"
fi

echo ""
echo -e "${CYAN}=================================================================${NC}"
