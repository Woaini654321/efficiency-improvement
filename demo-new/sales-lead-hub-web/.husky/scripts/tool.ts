import { execa } from 'execa'
import chalk from 'chalk'

const tick = chalk.green('✔')
const cross = chalk.red('✖')

function nowMs() {
  return Date.now()
}

function printCost(start: number, end: number, label: string) {
  console.log(chalk.dim(`⏱ ${label} 用时：${end - start} ms`))
}

export async function repoRoot(): Promise<string> {
  const { stdout } = await execa('git', ['rev-parse', '--show-toplevel'], { stdio: 'pipe' })
  return stdout.trim()
}

export async function runCommand(cmd: string, args: string[], label: string, cwd: string) {
  const start = nowMs()
  console.log(chalk.yellow(chalk.bold(`• ${label}`)))
  try {
    await execa(cmd, args, { stdio: 'inherit', cwd })
    printCost(start, nowMs(), label)
    console.log(`${tick} ${label} ${chalk.green('通过')}\n`)
  } catch (err: any) {
    console.log(`${cross} ${label} ${chalk.red('失败')}`)
    if (err?.shortMessage) console.log(chalk.dim(err.shortMessage))
    throw err
  }
}
