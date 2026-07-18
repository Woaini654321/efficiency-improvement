export function getEnCnName({ enName, legalName }: { enName: string; legalName: string }) {
  return enName && legalName && enName !== legalName
    ? `${enName} ${legalName}`
    : enName || legalName
}

export function getEnCnDeptName({ enName, cnName }: { enName: string; cnName: string }) {
  return enName && cnName && enName !== cnName ? `${enName}${cnName}` : enName || cnName
}
