import { getDashboard } from '../../services/report'
import { Overview, Task } from '../../types/api'
// 看板进入前台立即加载，并每分钟刷新；离开页面后清理定时器避免后台持续请求。
Page({ data: { overview: {} as Overview, tasks: [] as Task[], lastRefreshTime: '', dataStatus: '' }, onShow() { /* 前台进入时先取一次最新快照，再启动周期刷新。 */ this.load(); this.timer = setInterval(() => this.load(), 60000) }, onHide() { /* 页面隐藏后停止定时器，回到前台会重新建立。 */ clearInterval(this.timer) }, load() { /* 只读请求失败不清空旧数据，提示用户当前刷新失败。 */ getDashboard().then(result => this.setData({ overview: result.overview, tasks: result.tasks, lastRefreshTime: result.overview.lastRefreshTime, dataStatus: result.overview.dataStatus })).catch(error => wx.showToast({ title: error.message, icon: 'none' })) }, timer: 0 as unknown as number })
