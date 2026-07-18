import chalk from 'chalk'
import { runCommand, repoRoot } from './tool'

;(async () => {
  const root = await repoRoot()
  console.log('\n' + chalk.bold(chalk.blue('▶ Pre-commit 开始')) + '\n')

  // 对根目录进行 typecheck (这里不能使用 --filter "[HEAD^]"，因为他会导致子包也会被 typecheck 一遍，貌似和 -w 冲突了，待 pnpm 优化，当然也可以使用排除所有子包的办法，但有点呆)
  // 等同于指令：pnpm -w --if-present run typecheck
  await runCommand('pnpm', ['-w', '--if-present', 'run', 'typecheck'], '根目录 typecheck', root)

  // 对子包进行 typecheck，但是在工程体积比较大的情况下可能会比较耗时，等待 golang 重写后的 tsc 吧
  // 等同于指令：pnpm -r --if-present run typecheck
  await runCommand(
    'pnpm',
    // ['-r', '--filter', '...[HEAD^]', '--if-present', 'run', 'typecheck'], // 等同于指令：pnpm -r --filter "[HEAD^]" --if-present run typecheck，这个指令不准确，且一些场景下会导致报错，这里还是全量吧
    ['-r', '--if-present', 'run', 'typecheck'],
    '子包 typecheck',
    root
  )

  // lint staged 校验：oxlint、eslint、oxfmt
  await runCommand('pnpm', ['lint-staged'], '全局 lint-staged', root)
  console.log(chalk.bold(chalk.green('▶ Pre-commit 完成')))
})().catch(() => {
  console.error(chalk.bold(chalk.yellow('☹️ 任务终止')))
  process.exitCode = 1
})
