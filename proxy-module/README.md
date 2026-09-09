# KaelorvynGuardProxy（Velocity 代理端模块）

配合 Paper 端 KaelorvynGuard 使用：

- 进服/切服时统一发送"珍惜账号 / 零容忍"安全通知
- Paper 端踢出原因带 `[KG]` 标记时，自动重定向玩家到 `lobby` 而不是掉线

## 构建

```powershell
$env:JAVA_HOME='D:\Java\jdk-25'
.\gradlew.bat clean build --no-daemon
```

产物 `build\libs\KaelorvynGuardProxy-1.5.0-preview.jar`，放入代理端
`D:\MC\server\[25565] 代理端\plugins\` 后重启代理生效。

## 配置

- 配置：`plugins/kaelorvynguardproxy/config.properties`
  - `lobby=lobby` 大厅服名
  - `notice-message=<gold>...</gold>` 进服通知（MiniMessage 格式）
- 权限 `kg.notice.bypass` 可跳过通知
