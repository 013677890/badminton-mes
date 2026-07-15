"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const auth_1 = require("../../services/auth");
Page({ data: { user: {} }, onShow() { this.setData({ user: wx.getStorageSync('mes_user') || {} }); }, logout: auth_1.logout });
