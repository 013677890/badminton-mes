"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const auth_1 = require("../../services/auth");
Page({ data: { ticket: '', userNo: '', password: '' }, onLoad(query) { this.setData({ ticket: query.ticket || '' }); }, input(event) { this.setData({ [event.currentTarget.dataset.key]: event.detail.value }); }, bind() { const { ticket, userNo, password } = this.data; if (!ticket || !userNo || !password) {
        wx.showToast({ title: '请填写完整', icon: 'none' });
        return;
    } wx.showLoading({ title: '绑定中' }); (0, auth_1.bindAccount)(ticket, userNo, password).then(() => wx.reLaunch({ url: '/pages/dashboard/dashboard' })).catch(error => wx.showToast({ title: error.message, icon: 'none' })).finally(() => wx.hideLoading()); } });
