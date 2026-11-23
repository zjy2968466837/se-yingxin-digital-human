import request from './request'

// 数据看板总览：KPI / 分时报到趋势 / 学院进度 / 热门问题 / 问答命中分布
export function getOverview() {
  return request.get('/admin/stats/overview')
}
