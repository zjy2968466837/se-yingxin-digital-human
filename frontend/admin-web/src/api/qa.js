import request from './request'

// 问答库分页查询
export function listQuestions(params) {
  return request.get('/qa/admin/questions', { params })
}

// 新建问答
export function createQuestion(data) {
  return request.post('/qa/admin/questions', data)
}

// 局部更新问答
export function updateQuestion(id, data) {
  return request.patch(`/qa/admin/questions/${id}`, data)
}

// 删除问答
export function deleteQuestion(id) {
  return request.delete(`/qa/admin/questions/${id}`)
}

// 未解答问题分页（status=1 待处理）
export function listUnanswered(params) {
  return request.get('/qa/admin/unanswered', { params: { status: 1, ...params } })
}
