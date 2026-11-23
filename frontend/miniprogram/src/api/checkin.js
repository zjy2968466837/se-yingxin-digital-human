// 报到相关接口（checkin-service）
import { request } from './request'

/** 报到步骤模板（含办理地点/时间/联系人） */
export function listSteps() {
  return request({ url: '/checkin/steps' })
}

/** 本人报到进度：{ steps[], doneCount, totalCount, percent } */
export function fetchProgress() {
  return request({ url: '/checkin/progress' })
}

/** 步骤打卡（幂等，重复打卡返回 isDuplicated=true） */
export function checkinStep(stepCode) {
  return request({ url: `/checkin/steps/${stepCode}/checkin`, method: 'POST' })
}

/** 报到凭证：全部步骤完成后签发 */
export function fetchVoucher() {
  return request({ url: '/checkin/voucher' })
}
