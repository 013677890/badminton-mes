"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.loginWithWechat = loginWithWechat;
exports.bindAccount = bindAccount;
exports.ensureLogin = ensureLogin;
exports.logout = logout;
const http_1 = require("./http");
async function loginWithWechat() {
    const code = await new Promise((resolve, reject) => wx.login({ success: result => resolve(result.code), fail: reject }));
    const result = await (0, http_1.request)({ url: '/api/system/mini_app/auth/login', method: 'POST', data: { code } });
    if (result.token)
        wx.setStorageSync('mes_token', result.token);
    if (result.userId)
        wx.setStorageSync('mes_user', result);
    return result;
}
async function bindAccount(bindTicket, userNo, password) {
    const result = await (0, http_1.request)({ url: '/api/system/mini_app/auth/bind', method: 'POST', data: { bindTicket, userNo, password } });
    if (result.token) {
        wx.setStorageSync('mes_token', result.token);
        wx.setStorageSync('mes_user', result);
    }
    return result;
}
async function ensureLogin() {
    if (wx.getStorageSync('mes_token'))
        return;
    const result = await loginWithWechat();
    if (result.bindingRequired)
        wx.reLaunch({ url: `/pages/bind-account/bind-account?ticket=${result.bindTicket || ''}` });
    else
        wx.reLaunch({ url: '/pages/dashboard/dashboard' });
}
function logout() {
    return (0, http_1.request)({ url: '/api/system/auth/logout', method: 'POST' }).catch(() => undefined).then(() => { wx.clearStorageSync(); wx.reLaunch({ url: '/pages/login/login' }); });
}
