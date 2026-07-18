export const getOurCompanyStr = () => {
  const str = '$q(u#e@c1*^t0`~el' // 混淆下公司名，避免给外部公司部署时代码中直接存在原公司名
  const letters = str.match(/[a-zA-Z]/g)
  return letters?.join('')
}
