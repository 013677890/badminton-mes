# Docker 拉取、构建与微信小程序联调命令

本文命令默认在项目根目录执行：

```powershell
cd D:\SoftwareAnalysis\badmintonProject\badminton-mes
```

## 1. 前置检查

```powershell
docker info
docker compose config --quiet
docker compose ps
```

确认项目根目录存在本地 `.env`。该文件包含数据库和微信小程序密钥，已被 Git 忽略，禁止提交到远程仓库。

## 2. 拉取镜像

拉取 Compose 文件声明的全部可拉取镜像：

```powershell
docker compose pull
```

单独拉取当前项目使用的 MySQL 和 Redis：

```powershell
docker pull mysql:8.4
docker pull redis:8.0-alpine
```

构建后端时同时尝试拉取 Dockerfile 中更新的基础镜像：

```powershell
docker compose build --pull backend
```

如果 Docker Hub 出现 `TLS handshake timeout`，先分别重试基础镜像，再重新构建：

```powershell
docker pull mysql:8.4
docker pull redis:8.0-alpine
docker compose build --pull --progress=plain backend
```

Dockerfile 的基础镜像标签应以实际文件为准：

```powershell
Get-Content backend-java\Dockerfile
```

看到 `FROM` 后，可手动执行对应的 `docker pull <镜像:标签>`。

## 3. 构建后端镜像

普通构建：

```powershell
docker compose build backend
```

显示完整日志并拉取最新基础镜像：

```powershell
docker compose build --pull --progress=plain backend
```

完全忽略缓存重新构建：

```powershell
docker compose build --no-cache --pull --progress=plain backend
```

查看构建后的镜像：

```powershell
docker images badminton-mes/backend
```

## 4. 启动服务

启动 MySQL 和 Redis：

```powershell
docker compose up -d mysql redis
docker compose ps
```

使用新镜像重建并启动后端容器：

```powershell
docker compose up -d --build --force-recreate backend
```

启动全部服务：

```powershell
docker compose up -d --build
```

查看后端日志：

```powershell
docker compose logs -f --tail=200 backend
```

日志中应出现 Spring Boot 启动成功、Flyway 校验成功以及端口 8080 启动完成。按 `Ctrl+C` 只退出日志跟踪，不会停止容器。

## 5. 健康检查

```powershell
docker compose ps
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/api/system/auth/registration_roles
```

当前 WLAN 地址为 `172.25.96.19` 时，局域网检查命令为：

```powershell
Invoke-WebRequest -UseBasicParsing http://172.25.96.19:8080/api/system/auth/registration_roles
```

如果 WLAN 地址变化，使用以下命令重新查看：

```powershell
ipconfig
```

应选择带默认网关的 `Wireless LAN adapter WLAN` 下的 IPv4 地址，不要选择 VMware、Docker、VPN 或其他虚拟网卡地址。

## 6. Windows 防火墙

如果 `127.0.0.1:8080` 可以访问，但 WLAN 地址无法访问，需要使用“以管理员身份运行”的 PowerShell 执行：

```powershell
.\scripts\setup-miniapp-firewall.ps1
```

脚本会自动请求管理员权限、删除可能存在的同名错误规则，并重新创建以下受限规则：

- 只允许 TCP 8080；
- 只允许 Private、Public 网络配置；
- 只允许本地子网 `LocalSubnet` 访问；
- 自动识别 WLAN IPv4；
- 创建 `WLAN_IP:8080 → 127.0.0.1:8080` 的 Windows `portproxy`，兼容 Docker Desktop 只暴露 localhost 的情况；
- 可以重复运行，不会累积同名规则。

如果 PowerShell 的脚本执行策略阻止运行，可以执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-miniapp-firewall.ps1
```

如果自动识别的 WLAN 地址不正确，可以显式指定：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-miniapp-firewall.ps1 -WlanAddress 172.25.96.19
```

检查端口代理：

```powershell
netsh interface portproxy show v4tov4
```

也可以使用不含反引号续行的一行命令手动创建：

```powershell
New-NetFirewallRule -DisplayName "Badminton MES Backend 8080" -Description "允许同一局域网访问羽毛球 MES 后端" -Direction Inbound -Action Allow -Enabled True -Profile Private,Public -Protocol TCP -LocalPort 8080 -RemoteAddress LocalSubnet
```

检查规则：

```powershell
Get-NetFirewallRule -DisplayName "Badminton MES Backend 8080"
```

不再需要时删除规则：

```powershell
Remove-NetFirewallRule -DisplayName "Badminton MES Backend 8080"
```

## 7. 微信开发者工具配置

小程序默认 API 地址由以下文件维护：

```text
miniapp-wechat/miniprogram/services/config.ts
```

真机当前默认地址：

```text
http://172.25.96.19:18080
```

微信开发者工具自动使用：

```text
http://127.0.0.1:8080
```

局域网端口 18080 由 `scripts/start-miniapp-lan-proxy.ps1` 转发到 Docker 的 `127.0.0.1:8080`，不关闭也不修改 Clash。

开发机 IP 变化后，可以在微信开发者工具控制台临时覆盖：

```javascript
wx.setStorageSync('miniapp_api_base_url', 'http://新的WLAN地址:8080')
```

恢复代码中的默认地址：

```javascript
wx.removeStorageSync('miniapp_api_base_url')
```

确保使用真实 API 模式：

```javascript
wx.setStorageSync('miniapp_data_source', 'api')
```

数据库重建后，旧 Token 已失效，应清除登录状态并重新登录：

```javascript
wx.removeStorageSync('mes_token')
wx.removeStorageSync('mes_profile')
```

开发者工具中还需要勾选“不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”。真机普通版本仍受微信合法域名规则限制，局域网 HTTP 更适合开发者工具真机调试。

## 8. 小程序构建

```powershell
cd miniapp-wechat
npm run typecheck
npm run build
```

构建成功后，在微信开发者工具中执行“清缓存并重新编译”，避免旧 JavaScript 和旧本地 API 地址继续生效。

## 9. 数据库备份

项目本地备份目录已加入 `.gitignore`：

```text
database-backups/
```

使用 MySQL 容器内的应用账号导出：

```powershell
New-Item -ItemType Directory -Force database-backups | Out-Null
docker exec mes-mysql sh -c "mysqldump -u\`$MYSQL_USER -p\`$MYSQL_PASSWORD --single-transaction --routines --triggers --events --no-tablespaces --default-character-set=utf8mb4 \`$MYSQL_DATABASE --result-file=/tmp/badminton_mes_backup.sql"
docker cp mes-mysql:/tmp/badminton_mes_backup.sql database-backups\badminton_mes_backup.sql
Get-Content database-backups\badminton_mes_backup.sql -Tail 5
```

只有末尾出现 `Dump completed` 才表示导出完整结束。

## 10. 常用排障命令

```powershell
docker compose ps
docker compose logs --tail=300 backend
docker inspect mes-backend
docker stats --no-stream
netstat -ano | Select-String ":8080"
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/api/system/auth/registration_roles
```

如果修改了后端代码但行为仍然是旧版本，通常是旧容器仍在运行。执行：

```powershell
docker compose build --no-cache --pull backend
docker compose up -d --force-recreate backend
docker compose logs -f --tail=200 backend
```

如果修改了小程序 TypeScript 但开发者工具仍显示旧行为，执行：

```powershell
cd miniapp-wechat
npm run build
```

然后在微信开发者工具中清缓存并重新编译。
