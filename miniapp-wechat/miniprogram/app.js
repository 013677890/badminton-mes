"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const auth_1 = require("./services/auth");
App({
    onLaunch() {
        void (0, auth_1.ensureLogin)();
    }
});
