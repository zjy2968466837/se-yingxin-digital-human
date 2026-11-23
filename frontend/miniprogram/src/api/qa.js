// 问答相关接口（qa-service）
import { request } from './request'

/** 连通性自检（网关白名单，无需登录） */
export function pingQa() {
  return request({ url: '/qa/ping' })
}

/** 创建问答会话 → { sessionId }（雪花 ID 以字符串返回，避免 JS 精度丢失） */
export function createQaSession() {
  return request({ url: '/qa/sessions', method: 'POST' })
}

/** 提问：{ sessionId, question } → { answer, hitType, confidence, suggestions[], costMs } */
export function askQuestion(data) {
  return request({ url: '/qa/ask', method: 'POST', data })
}

/** 热门问题 TOP5 */
export function hotQuestions() {
  return request({ url: '/qa/hot' })
}

/** 会话历史消息 */
export function listMessages(sessionId) {
  return request({ url: `/qa/sessions/${sessionId}/messages` })
}
