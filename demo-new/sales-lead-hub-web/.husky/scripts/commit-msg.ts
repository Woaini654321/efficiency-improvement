import chalk from 'chalk'
import { runCommand, repoRoot } from './tool'
;(async () => {
  const root = await repoRoot()
  console.log('\n' + chalk.bold(chalk.blue('▶ Commit-lint 开始')) + '\n')

  // 提交信息格式校验
  // 等同于指令：pnpm commitlint --edit "$1"
  await runCommand(
    'pnpm',
    ['commitlint', '--edit', '.git/COMMIT_EDITMSG'],
    '提交信息格式校验',
    root
  )

  console.log(chalk.bold(chalk.green('▶ Commit-lint 完成')))
})().catch(() => {
  console.log('')
  console.error('❌ 提交信息不符合规范（Conventional Commits）')
  console.log('')
  console.log('👉 规范格式：<type>(<scope>): <subject>')
  console.log('   示例：')
  console.log('     feat(ui): 新增按钮组件')
  console.log('     fix(auth): 修复登录状态丢失')
  console.log('     frame-docs: 更新 README')
  console.log('')
  console.log(
    '📌 常用 type：feat | fix | frame-docs | style | refactor | perf | test | build | ci | chore | revert'
  )
  console.log('🔗 参考文档（中文）：https://www.conventionalcommits.org/zh-hans/')
  console.log('')
  process.exitCode = 1
})
