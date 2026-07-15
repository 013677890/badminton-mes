"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const report_1 = require("../../services/report");
Page({ data: { batchCode: '', trace: { processHistories: [], workReports: [], repairRecords: [], warnings: [], dataCompleteness: '' } }, input(event) { this.setData({ batchCode: event.detail.value }); }, scan() { wx.scanCode({ success: result => { this.setData({ batchCode: result.result }); this.query(); } }); }, query() { if (!this.data.batchCode) {
        wx.showToast({ title: '请输入批次码', icon: 'none' });
        return;
    } (0, report_1.getTrace)(this.data.batchCode).then(trace => this.setData({ trace })).catch(error => wx.showToast({ title: error.message, icon: 'none' })); } });
