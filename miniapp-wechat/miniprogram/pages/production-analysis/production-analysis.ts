import { getProductionSummary } from '../../services/report'
Page({ data: { period: '今日', summary: {} }, onShow() { const now = new Date(); const start = new Date(now.getFullYear(), now.getMonth(), now.getDate()).toISOString(); getProductionSummary(start, now.toISOString()).then(summary => this.setData({ summary })).catch(error => wx.showToast({ title: error.message, icon: 'none' })) } })
