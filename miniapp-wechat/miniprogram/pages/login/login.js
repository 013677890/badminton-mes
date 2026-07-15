"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const auth_1 = require("../../services/auth");
Page({ login() { wx.showLoading({ title: '登录中' }); (0, auth_1.ensureLogin)().catch(error => wx.showToast({ title: error.message, icon: 'none' })).finally(() => wx.hideLoading()); } });
