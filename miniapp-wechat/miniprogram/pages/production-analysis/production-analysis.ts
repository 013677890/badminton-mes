import { getProductionSummary } from '../../services/report'
// 每次进入页面查询当天零点到当前时刻的生产汇总，日期范围由本地时间计算后转 ISO。
Page({ data: { period: '今日', summary: {} }, onShow() { /* 进入前台时重新读取，避免停留期间数据过期。 */ const now = new Date(); const start = new Date(now.getFullYear(), now.getMonth(), now.getDate()).toISOString(); getProductionSummary(start, now.toISOString()).then(summary => this.setData({ summary })).catch(error => wx.showToast({ title: error.message, icon: 'none' })) } })
