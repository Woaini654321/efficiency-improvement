<#
.SYNOPSIS
    sales-lead-hub-server 后端启动脚本（Quectel-code / Spring Boot 2.7.18 / Java 8）。

.DESCRIPTION
    默认走 `mvn spring-boot:run` 开发热跑；加 -Package 则先打包成 jar 再用 java -jar 启动。
    启动前会校验 JDK8 与三个内网依赖（Nexus / MySQL / UAA）是否可达，避免起到一半才报错。

.PARAMETER Package
    先 `mvn clean package -DskipTests` 打包，再运行 dist/sales-lead-hub-server.jar（贴近生产的跑法）。

.PARAMETER SkipCheck
    跳过内网连通性预检（已确认在内网时可加，省几秒）。

.EXAMPLE
    ./start-server.ps1              # 开发热跑
    ./start-server.ps1 -Package     # 打包后以 jar 启动
#>
[CmdletBinding()]
param(
    [switch]$Package,
    [switch]$SkipCheck
)

$ErrorActionPreference = 'Stop'

# 中文输出防乱码（PS 5.1 控制台默认非 UTF-8）
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8; $OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

# 脚本所在目录即项目根，保证在任意位置调用都能正确定位
$ProjectDir = $PSScriptRoot
Set-Location $ProjectDir

# 后端固定端口（application.yml 的 server.port）
$ServerPort = 8081

# —— 释放目标端口 ——
# 重跑脚本时，先递归杀掉上次遗留在该端口的进程树（含 maven/java 子进程），
# 保证每次都绑回固定端口，避免旧进程占用导致端口冲突或启动失败。
function Stop-ProcessTree($ProcId) {
    Get-CimInstance Win32_Process -Filter "ParentProcessId=$ProcId" -ErrorAction SilentlyContinue |
        ForEach-Object { Stop-ProcessTree $_.ProcessId }
    try { Stop-Process -Id $ProcId -Force -ErrorAction Stop } catch {}
}
function Clear-Port($Port) {
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) { return }
    foreach ($procId in ($conns | ForEach-Object OwningProcess | Sort-Object -Unique)) {
        $p = Get-Process -Id $procId -ErrorAction SilentlyContinue
        Write-Host ("  端口 {0} 被占用 -> 结束上次残留进程树 pid={1} ({2})" -f $Port, $procId, $p.ProcessName) -ForegroundColor Yellow
        Stop-ProcessTree $procId
    }
    Start-Sleep -Seconds 1
}

# —— 固定使用 Java 8（pom parent 锁定 Java8，用错版本编译期就崩）——
$Jdk8Home = 'D:\Tools\jdk8'
if (Test-Path $Jdk8Home) {
    $env:JAVA_HOME = $Jdk8Home
    $env:PATH = "$Jdk8Home\bin;$env:PATH"
} else {
    # 不静默降级：JDK8 路径缺失时明确告警，避免误用 PATH 上的高版本 JDK 到编译期才崩
    Write-Host "[WARN] 未找到 JDK8：$Jdk8Home —— 将回退到 PATH 上的 java，若非 1.8，quectel-code-parent 锁 Java8 会在编译期报错。" -ForegroundColor Yellow
}

# —— 工具预检（缺 java/mvn 直接退出，别起一半才报错）——
foreach ($tool in 'java', 'mvn') {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        Write-Host "缺少 $tool，请先安装并配置 PATH。" -ForegroundColor Red
        exit 1
    }
}

# —— 内网依赖预检 ——
function Test-Endpoint($Name, $HostName, $Port) {
    $ok = Test-NetConnection -ComputerName $HostName -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($ok) {
        Write-Host ("  [OK]   {0,-8} {1}:{2}" -f $Name, $HostName, $Port) -ForegroundColor Green
    } else {
        Write-Host ("  [FAIL] {0,-8} {1}:{2}" -f $Name, $HostName, $Port) -ForegroundColor Red
    }
    return $ok
}

if (-not $SkipCheck) {
    Write-Host "内网依赖预检（需在公司内网/VPN）..." -ForegroundColor Cyan
    $r1 = Test-Endpoint 'Nexus' '192.168.10.107' 8081   # Maven 私服
    $r2 = Test-Endpoint 'MySQL' '192.168.10.28'  3306    # 开发库
    $r3 = Test-Endpoint 'UAA'   '192.168.10.27'  8088    # SSO 网关
    if (-not ($r1 -and $r2 -and $r3)) {
        Write-Host "`n有内网依赖不可达。请确认已连公司内网/VPN；确无问题可加 -SkipCheck 跳过。" -ForegroundColor Yellow
        exit 1
    }
    Write-Host ""
}

# —— 启动 ——
# 启动前释放 8081：清掉上次残留的服务，保证本次能绑回固定端口。
Clear-Port $ServerPort

if ($Package) {
    Write-Host "打包（mvn clean package -DskipTests）..." -ForegroundColor Cyan
    & mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { Write-Host "打包失败，退出码 $LASTEXITCODE" -ForegroundColor Red; exit $LASTEXITCODE }

    $Jar = Join-Path $ProjectDir 'dist\sales-lead-hub-server.jar'
    if (-not (Test-Path $Jar)) { Write-Host "未找到产物：$Jar" -ForegroundColor Red; exit 1 }

    Write-Host "以 jar 启动：$Jar（端口 8081）" -ForegroundColor Cyan
    & java -jar $Jar
} else {
    Write-Host "开发热跑（mvn spring-boot:run，端口 8081）..." -ForegroundColor Cyan
    & mvn spring-boot:run
}
