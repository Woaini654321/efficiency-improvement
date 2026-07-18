export const getFrontEndAppConfEnvs = () => {
  const arr: { name: string; value: string }[] = []
  for (const key in import.meta.env) {
    if (key.startsWith('VITE_FRONT_END_APP_CONF_')) {
      arr.push({
        name: key.replace('VITE_FRONT_END_APP_CONF_', ''),
        value: import.meta.env[key] as string
      })
    }
  }
  return arr
}
