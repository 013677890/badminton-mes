"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.request = request;
// 本地 Spring Boot 后端：server.port 默认 8080，docker-compose 同样映射到宿主机 8080。
const BASE_URL = 'http://127.0.0.1:8080';
function request(options) {
    const token = wx.getStorageSync('mes_token');
    return new Promise((resolve, reject) => {
        wx.request({
            ...options,
            url: `${BASE_URL}${options.url}`,
            header: { 'Content-Type': 'application/json', ...(options.header || {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
            success: response => {
                const body = response.data;
                if (response.statusCode === 401) {
                    wx.removeStorageSync('mes_token');
                    wx.reLaunch({ url: '/pages/login/login' });
                    reject(new Error('登录已失效'));
                    return;
                }
                if (response.statusCode < 200 || response.statusCode >= 300 || body.code !== '00000') {
                    reject(new Error(body.userTip || body.message || '请求失败'));
                    return;
                }
                resolve(body.data);
            },
            fail: reject
        });
    });
}
