<#
.SYNOPSIS
    sales-lead-hub-web 前端启动脚本（QMonoX Monorepo / pnpm / vite）。

.DESCRIPTION
    默认 `pnpm dev:web-app` 启动开发服务（含 predev self-check 门禁），dev 地址 http://localhost:8080/web。
    首次或缺 node_modules 时自动 `pnpm install`（走公司内部 npm 私服）。加 -Build 则做生产构建。

.PARAMETER Install
    强制先执行 `pnpm install`（依赖有变动时用）。

.PARAMETER Build
    生产构建（pnpm build:web-app），产物在 apps/web-app/dist。

.PARAMETER SkipCheck
    跳过内部 npm 私服连通性预检。

.EXAMPLE
    ./start-web.ps1            # 开发热跑
    ./start-web.ps1 -Install   # 先装依赖再热跑
    ./start-web.ps1 -Build     # 生产构建
#>
[CmdletBinding()]
param(
    [switch]$Install,
    [switch]$Build,
    [switch]$SkipCheck
)

$ErrorActionPreference = 'Stop'

# 脚本所在目录即前端仓库根，保证任意位置调用都能定位
$ProjectDir = $PSScriptRoot
Set-Location $ProjectDir

# 前端 dev 固定端口（与 apps/web-app/env/.env 的 VITE_PORT 一致）
$DevPort = 8080

# —— 释放目标端口 ——
# 重跑脚本时，先递归杀掉上次遗留在该端口的进程树（含 pnpm/node/esbuild 子进程），
# 保证每次都绑回固定端口；否则旧 dev 还占着 8080，Vite 会自动漂移到 8081/8082...
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

# —— 工具校验 ——
foreach ($tool in 'node', 'pnpm') {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        Write-Host "缺少 $tool，请先安装（pnpm 需 10.x，可 npm i -g pnpm@10.34.5）。" -ForegroundColor Red
        exit 1
    }
}

# —— 内部 npm 私服预检（仅安装依赖时需要）——
$NexusHost = '192.168.10.107'
$NexusPort = 8081

# 是否需要安装/修复依赖：
#   1) 显式 -Install
#   2) 根 node_modules 缺失（从未安装）
#   3) web-app 的 dev 命令 q-cli-run 不可解析 —— 目录搬迁/改名后 pnpm 的
#      workspace 软链(junction)常断链，根 store 还在但 apps/web-app/node_modules 为空，
#      此时 pnpm install 会用现有 store 秒级重建软链。
$webAppBin = Join-Path $ProjectDir 'apps\web-app\node_modules\.bin\q-cli-run'
$linksBroken = -not (Test-Path $webAppBin)
if ($linksBroken -and (Test-Path (Join-Path $ProjectDir 'node_modules'))) {
    Write-Host "检测到 workspace 依赖链接缺失/断链（q-cli-run 不可解析），将执行 pnpm install 修复。" -ForegroundColor Yellow
}
$needInstall = $Install -or (-not (Test-Path (Join-Path $ProjectDir 'node_modules'))) -or $linksBroken

if ($needInstall -and -not $SkipCheck) {
    Write-Host "内部 npm 私服预检（需在公司内网/VPN）..." -ForegroundColor Cyan
    $ok = Test-NetConnection -ComputerName $NexusHost -Port $NexusPort -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($ok) {
        Write-Host ("  [OK]   Nexus {0}:{1}" -f $NexusHost, $NexusPort) -ForegroundColor Green
    } else {
        Write-Host ("  [FAIL] Nexus {0}:{1} 不可达，无法安装依赖。请确认已连内网/VPN；已装好依赖可加 -SkipCheck。" -f $NexusHost, $NexusPort) -ForegroundColor Red
        exit 1
    }
}

# —— 安装依赖 ——
if ($needInstall) {
    Write-Host "安装依赖（pnpm install）..." -ForegroundColor Cyan
    & pnpm install
    if ($LASTEXITCODE -ne 0) { Write-Host "pnpm install 失败，退出码 $LASTEXITCODE" -ForegroundColor Red; exit $LASTEXITCODE }
}

# —— 启动 ——
if ($Build) {
    Write-Host "生产构建（pnpm build:web-app）..." -ForegroundColor Cyan
    & pnpm build:web-app
} else {
    Write-Host ("开发热跑（pnpm dev:web-app）-> http://localhost:{0}/web" -f $DevPort) -ForegroundColor Cyan
    Clear-Port $DevPort
    & pnpm dev:web-app
}
