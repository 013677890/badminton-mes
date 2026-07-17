"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const report_1 = require("../../services/report");
Page({ data: { overview: {}, tasks: [], lastRefreshTime: '', dataStatus: '' }, onShow() { this.load(); this.timer = setInterval(() => this.load(), 60000); }, onHide() { clearInterval(this.timer); }, load() { (0, report_1.getDashboard)().then(result => this.setData({ overview: result.overview, tasks: result.tasks, lastRefreshTime: result.overview.lastRefreshTime, dataStatus: result.overview.dataStatus })).catch(error => wx.showToast({ title: error.message, icon: 'none' })); }, timer: 0 });
