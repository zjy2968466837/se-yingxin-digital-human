import request from './request'

// 报到步骤模板
export function listSteps() {
  return request.get('/checkin/steps')
}

// 某步骤所需材料清单
export function listMaterials(stepCode) {
  return request.get(`/checkin/steps/${stepCode}/materials`)
}
