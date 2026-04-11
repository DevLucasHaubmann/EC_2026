$ErrorActionPreference = "Continue"
$base = "http://localhost:8080"

function Invoke-Api {
    param([string]$Method, [string]$Path, [string]$Body, [string]$Token)
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    try {
        $params = @{
            Uri = "$base$Path"
            Method = $Method
            ContentType = "application/json"
            Headers = $headers
        }
        if ($Body) { $params["Body"] = $Body }
        $response = Invoke-RestMethod @params
        return @{ Status = 200; Data = $response }
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        $detail = $_.ErrorDetails.Message
        return @{ Status = $code; Data = $detail }
    }
}

Write-Host "`n=============================="
Write-Host "  TESTES DE PERFIL - TUKAN"
Write-Host "==============================`n"

# --- Setup: Registrar usuario de teste ---
Write-Host ">>> Setup: Registrando usuario de teste..."
$reg = Invoke-Api -Method "POST" -Path "/auth/register" -Body '{"nome":"Perfil Test User","email":"perfil.test.run@tukan.com","senha":"123456"}'
if ($reg.Status -eq 200) {
    $token = $reg.Data.accessToken
    Write-Host "[OK] Usuario registrado. Token obtido.`n"
} else {
    # Tentar login se ja existe
    Write-Host "[INFO] Registro falhou ($($reg.Status)), tentando login..."
    $login = Invoke-Api -Method "POST" -Path "/auth/login" -Body '{"email":"perfil.test.run@tukan.com","senha":"123456"}'
    if ($login.Status -eq 200) {
        $token = $login.Data.accessToken
        Write-Host "[OK] Login realizado. Token obtido.`n"
    } else {
        Write-Host "[FALHA] Nao foi possivel autenticar: $($login.Data)"
        exit 1
    }
}

# --- TESTE 1: POST /perfil/me - Criar perfil ---
Write-Host "--- TESTE 1: POST /perfil/me (criar perfil) ---"
$t1 = Invoke-Api -Method "POST" -Path "/perfil/me" -Token $token -Body '{"dataNascimento":"2000-05-15","sexo":"MASCULINO","pesoKg":75.5,"alturaCm":175.0,"nivelAtividade":"MODERADO"}'
if ($t1.Status -eq 200 -or $t1.Status -eq 201) {
    $perfil = $t1.Data.perfil
    if ($perfil) {
        Write-Host "[OK] Perfil criado. ID=$($perfil.id)"
        Write-Host "     onboardingRequired=$($t1.Data.onboardingRequired), nextStep=$($t1.Data.nextStep)"
    } else {
        # Pode ser que veio direto sem wrapper
        Write-Host "[OK] Response: $($t1.Data | ConvertTo-Json -Depth 3 -Compress)"
    }
} else {
    Write-Host "[STATUS $($t1.Status)] $($t1.Data)"
}
Write-Host ""

# --- TESTE 2: POST /perfil/me duplicado - Deve dar 409 ---
Write-Host "--- TESTE 2: POST /perfil/me (duplicado, espera 409) ---"
$t2 = Invoke-Api -Method "POST" -Path "/perfil/me" -Token $token -Body '{"dataNascimento":"2000-05-15","sexo":"MASCULINO","pesoKg":75.5,"alturaCm":175.0,"nivelAtividade":"MODERADO"}'
if ($t2.Status -eq 409) {
    Write-Host "[OK] Corretamente rejeitou perfil duplicado (409)."
} else {
    Write-Host "[FALHA] Esperava 409, recebeu $($t2.Status): $($t2.Data)"
}
Write-Host ""

# --- TESTE 3: GET /perfil/me - Consultar proprio perfil ---
Write-Host "--- TESTE 3: GET /perfil/me (consultar perfil) ---"
$t3 = Invoke-Api -Method "GET" -Path "/perfil/me" -Token $token
if ($t3.Status -eq 200) {
    Write-Host "[OK] Perfil retornado: peso=$($t3.Data.pesoKg)kg, altura=$($t3.Data.alturaCm)cm, atividade=$($t3.Data.nivelAtividade)"
} else {
    Write-Host "[FALHA] Status $($t3.Status): $($t3.Data)"
}
Write-Host ""

# --- TESTE 4: PUT /perfil/me - Atualizar peso/altura/atividade ---
Write-Host "--- TESTE 4: PUT /perfil/me (atualizar campos permitidos) ---"
$t4 = Invoke-Api -Method "PUT" -Path "/perfil/me" -Token $token -Body '{"pesoKg":80.0,"alturaCm":176.0,"nivelAtividade":"INTENSO"}'
if ($t4.Status -eq 200) {
    Write-Host "[OK] Atualizado: peso=$($t4.Data.pesoKg)kg, altura=$($t4.Data.alturaCm)cm, atividade=$($t4.Data.nivelAtividade)"
} else {
    Write-Host "[FALHA] Status $($t4.Status): $($t4.Data)"
}
Write-Host ""

# --- TESTE 5: PUT /perfil/me - Atualizar parcial (so peso) ---
Write-Host "--- TESTE 5: PUT /perfil/me (atualizar apenas peso) ---"
$t5 = Invoke-Api -Method "PUT" -Path "/perfil/me" -Token $token -Body '{"pesoKg":82.3}'
if ($t5.Status -eq 200) {
    Write-Host "[OK] Peso atualizado: peso=$($t5.Data.pesoKg)kg (altura e atividade devem manter: $($t5.Data.alturaCm)cm, $($t5.Data.nivelAtividade))"
} else {
    Write-Host "[FALHA] Status $($t5.Status): $($t5.Data)"
}
Write-Host ""

# --- TESTE 6: Validacao - peso fora da faixa ---
Write-Host "--- TESTE 6: PUT /perfil/me (peso=5.0 - fora da faixa, espera 400) ---"
$t6 = Invoke-Api -Method "PUT" -Path "/perfil/me" -Token $token -Body '{"pesoKg":5.0}'
if ($t6.Status -eq 400) {
    Write-Host "[OK] Corretamente rejeitou peso fora da faixa (400)."
} else {
    Write-Host "[FALHA] Esperava 400, recebeu $($t6.Status): $($t6.Data)"
}
Write-Host ""

# --- TESTE 7: Validacao - altura fora da faixa ---
Write-Host "--- TESTE 7: POST /perfil/me (altura=9999 - fora da faixa, espera 400) ---"
$t7 = Invoke-Api -Method "PUT" -Path "/perfil/me" -Token $token -Body '{"alturaCm":9999.0}'
if ($t7.Status -eq 400) {
    Write-Host "[OK] Corretamente rejeitou altura fora da faixa (400)."
} else {
    Write-Host "[FALHA] Esperava 400, recebeu $($t7.Status): $($t7.Data)"
}
Write-Host ""

# --- TESTE 8: GET /perfil (admin) sem token admin - espera 403 ---
Write-Host "--- TESTE 8: GET /perfil (user tentando admin, espera 403) ---"
$t8 = Invoke-Api -Method "GET" -Path "/perfil" -Token $token
if ($t8.Status -eq 403) {
    Write-Host "[OK] Acesso negado corretamente (403)."
} else {
    Write-Host "[FALHA] Esperava 403, recebeu $($t8.Status): $($t8.Data)"
}
Write-Host ""

# --- TESTE 9: GET /perfil/me sem token - espera 401 ---
Write-Host "--- TESTE 9: GET /perfil/me (sem token, espera 401) ---"
$t9 = Invoke-Api -Method "GET" -Path "/perfil/me"
if ($t9.Status -eq 401) {
    Write-Host "[OK] Sem token retornou 401."
} else {
    Write-Host "[FALHA] Esperava 401, recebeu $($t9.Status): $($t9.Data)"
}
Write-Host ""

# --- TESTE 10: Testar com admin (se possivel) ---
Write-Host "--- TESTE 10: Login como admin e testar CRUD admin ---"
$adminLogin = Invoke-Api -Method "POST" -Path "/auth/login" -Body '{"email":"admin@tukan.com","senha":"123456"}'
if ($adminLogin.Status -eq 200) {
    $adminToken = $adminLogin.Data.accessToken
    Write-Host "[OK] Admin autenticado."

    # GET /perfil com paginacao
    Write-Host "  > GET /perfil?page=0&size=10 (listar paginado)"
    $t10a = Invoke-Api -Method "GET" -Path "/perfil?page=0&size=10" -Token $adminToken
    if ($t10a.Status -eq 200) {
        Write-Host "  [OK] Listagem paginada retornou. totalElements=$($t10a.Data.totalElements)"
    } else {
        Write-Host "  [FALHA] Status $($t10a.Status): $($t10a.Data)"
    }

    # PUT /perfil/{id} - admin atualiza campo restrito (sexo)
    if ($perfil -and $perfil.id) {
        Write-Host "  > PUT /perfil/$($perfil.id) (admin altera sexo)"
        $t10b = Invoke-Api -Method "PUT" -Path "/perfil/$($perfil.id)" -Token $adminToken -Body '{"sexo":"FEMININO","dataNascimento":"1999-01-01"}'
        if ($t10b.Status -eq 200) {
            Write-Host "  [OK] Admin atualizou: sexo=$($t10b.Data.sexo), nascimento=$($t10b.Data.dataNascimento)"
        } else {
            Write-Host "  [FALHA] Status $($t10b.Status): $($t10b.Data)"
        }
    }
} else {
    Write-Host "[SKIP] Admin nao encontrado ($($adminLogin.Status)). Testes admin pulados."
}
Write-Host ""

# --- Limpeza ---
Write-Host "--- LIMPEZA ---"
Write-Host "[INFO] Usuario de teste (perfil.test.run@tukan.com) permanece no banco para verificacao manual."
Write-Host ""
Write-Host "=============================="
Write-Host "  TESTES FINALIZADOS"
Write-Host "=============================="
