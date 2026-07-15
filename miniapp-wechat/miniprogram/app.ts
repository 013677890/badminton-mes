import { ensureLogin } from './services/auth'

App({
  onLaunch() {
    void ensureLogin()
  }
})
