declare module 'jsBridge' {
  export function closeAppByOs(): void
  export function getPlatform(): string
  export function getSystemInfo(): Promise<Record<string, any>>
}
