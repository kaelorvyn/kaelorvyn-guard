# project-0009-KaelorvynGuard-反作弊插件

## 项目说明

Kaelorvyn 服务器服务端反作弊插件，Paper 1.21.11 + PacketEvents 2.13.0。
纯服务端检测，不要求玩家安装任何客户端组件，不截屏、不扫盘、不采 HWID。

- 编号：0009
- 开始日期：2026-08-14
- 状态：v1.0.0 正式版，待灰度部署
- 产物：`outputs\KaelorvynGuard-1.0.1.jar` + `outputs\KaelorvynGuardProxy-1.0.0.jar`

## 版本状态

- v1.0：核心引擎 + 基础检查族 + AI/记忆 + 通报（已构建）
- v1.1：扩展防护网（已构建，测试通过）
- v1.2：外挂行为补齐（已构建，测试通过）
- v1.3：连点器四信号 + 更多行为检查（已构建，测试通过）
- v1.4：信任分级 + backtrack z-score + 软惩罚（已构建，测试通过）
- v1.5.0-preview：AuraBot 诱饵 + PacketOrder + MariaDB 四表 + 代理端模块（已构建，测试通过）
- v1.0.0：正式版（含以上全部功能 + 服务器插件兼容性适配，已构建，测试通过）
- v1.0.1：新增公开 `banPlayer(Player, String, int)` 供生存分流模组审计直接封号；
  AI 响应兼容 Markdown 代码围栏；中文类别新增“客户端模组违规”；
  击退检测默认关闭；挥动包不再计入封包洪水/动画刷包判定，攻击包单 tick 上限
  仅作洪水兜底；不再按客户端攻击/挥动包速度判定，只认服务端真实结果：
  `EntityDamageByEntityEvent` 与 `BlockBreakEvent`；快速挖掘/瞬挖/错工具/
  多方块破坏/挖空气/无挥动默认关闭；新增 `kickNoWarning`，模组审计踢出
  不计警告、不封号，只提示移除作弊模组；爬梯子/藤蔓/脚手架按方块识别豁免，
  攀爬期间 airTicks 清零，不再误报“长时间悬空”。

## 功能清单

- 三层架构：实时规则引擎 → 统计 + Agnes AI 长期记忆复核 → 警告通报/踢出/封禁对接 KaelorvynBan
- 移动模拟：按 1.21.11 常量计算合法移动包络（疾跑/潜行/药水/信标/冰/粘液/灵魂沙/水等）
- 检查族：Flight、Speed、NoFall/GroundSpoof、Phase、Timer、BadPackets、Reach、Aim、AutoClicker、KillAura、Scaffold、FastBreak、MultiBreak、InventoryMacro
- 封包指纹库：PacketFly y-420、双移动包、Meteor AntiKick -0.0313 等
- 抗体免疫库：确认后的外挂会话特征向量持久化，按相似度匹配已知外挂家族
- AI 复核：候选事件/周期快照 → Agnes API 判定，JSONL 证据日志
- 长期记忆：玩家警告计数、AI 判定历史、记忆摘要（JSON 存储，可切 MariaDB）
- 通报：进服零容忍通知（含累计封禁人数）、踢出前 Title/ActionBar、中文断开界面
- 管理：`/kg` 指令、OP+UUID 可信名单、按玩家/类别统计

## v1.1 新增检查（对照表）

| 检查 | 检测内容 | 参考来源 |
|---|---|---|
| AIM_SNAP | 旋转瞬移 %360 大跳变 | GrimAC AimModulo360 |
| TIMER/TICK_TIMER | 移动包时钟 + CLIENT_TICK_END | GrimAC Timer/TickTimer |
| REACH | 命中盒历史按 ping 回放 + 射线 | GrimAC Reach + ALICE LatencyCompensator |
| BLINK | 战斗期间移动包静默后恢复 | GrimAC Blink 对抗思路 |
| NO_VELOCITY | 收到击退后无位移 | GrimAC KnockbackHandler |
| MULTI_AURA | 短时间内攻击多个目标 | Meteor KillAura maxTargets |
| NO_SWING | 攻击/挖矿无挥动 | Intave NoSwingHeuristic |
| AUTO_SWITCH | 攻击瞬间自动切刀 | Intave ToolSwitchHeuristic |
| ELYTRA_FLY | 鞘翅超速 | GrimAC Elytra 检查族 |
| VEHICLE_FLY | 载具超速 | GrimAC Vehicle 检查族 |
| FAST_CLIMB | 攀爬超速 | 外挂 FastClimb 原理 |
| FAST_FALL | 下落超速 | 外挂 ReverseStep 原理 |
| SCAFFOLD | 脚下连续放置 + 放置速率 | GrimAC Scaffolding + Meteor Scaffold |
| FAST_PLACE | 快速放置 | GrimAC Scaffolding |
| XRAY_STAT | 挖矿目标异常集中（行为统计） | 外挂 XRay 原理 |
| 品牌采集 | minecraft:brand 记录到统计 | GrimAC ClientBrand |
| AIR_JUMP | 空中二次跳跃 | Meteor AirJump |
| SPIDER | 贴墙持续上升 | Wurst Spider |
| JESUS | 水面/岩浆面行走 | Wurst Jesus |
| NO_SLOWDOWN | 使用物品时未减速 | Wurst NoSlowdown |
| CRITICALS | 攻击前微偏移移动包 | Wurst Criticals 包序 |
| THROUGH_WALLS | 隔墙攻击 | GrimAC Reach 思路 |
| AIR_PLACE | 无支撑面放置方块 | Wurst AirPlace |
| INSTANT_MINE | 瞬挖 | GrimAC FastBreak + SpeedMine |
| PACKET_SPAM | 单 tick 封包洪水 | GrimAC Crash 检查族思路 |
| BAD_PACKETS | 非法 pitch/重复槽位 | GrimAC BadPackets |
| AUTO_CLICKER | 连点器四信号：CPS/变异系数/熵/完美重复间隔（左右键分开） | Intave ClickPatterns + ALICE |
| ELYTRA_NO_ITEM | 无鞘翅装备却滑翔 | GrimAC ElytraA |
| ELYTRA_BOOST | 鞘翅无烟花加速 | Meteor ElytraBoost |
| ATTACK_WHILE_MINING | 挖矿中立即攻击 | Intave AttackReduceIgnore |
| AIR_LIQUID_BREAK | 挖掘空气/液体方块 | GrimAC Breaking.AirLiquidBreak |
| ANIMATION_SPAM | 挥动包远多于攻击包 | GrimAC BadPackets |
| CRASH_A | 背包点击洪水 | GrimAC Crash 检查族 |
| CHAT / CHAT_SPAM | 换行符消息 / 聊天刷屏 | Scythe ChatB 思路 |
| NO_VELOCITY 增强 | 按预期击退速度比例检测位移不足 | GrimAC KnockbackHandler |
| BACKTRACK | 延迟尖峰 z-score（疑似位置回滚） | Intave backtrack 检测 |
| TrustFactor | 每玩家 0-5 信任等级，低信任更容易触发 | Intave TrustFactor |
| MITIGATION | 可疑玩家伤害削弱（软惩罚） | AnGuard/Intave 缓解策略 |
| AURA_BOT | 隐形假玩家实体诱饵，攻击即实锤 | ALICE AuraBotManager |
| PACKET_ORDER | 背包点击顺序异常 | GrimAC PacketOrderA |
| BAD_PACKETS | 非法热键栏槽位 | GrimAC BadPacketsB |
| MariaDB 四表 | kg_warnings/kg_players/kg_ai_verdicts/kg_events | 长期记忆/警告/判定/事件 |
| 代理端模块 | 进服/切服通知 + [KG] 踢出重定向大厅 | KaelorvynGuardProxy |
| REACH 延迟复检 | 攻击后下一帧用新命中盒再验证 | Intave 攻击扣留思路 |
| MULTI_PLACE | 单 tick 多次放置 | GrimAC Scaffolding.MultiPlace |
| WRONG_TOOL_FAST | 错误工具却破坏过快 | GrimAC Breaking.WrongBreak |
| PACKET_ORDER | 放置后立即挖掘同位置（放置宏） | GrimAC PacketOrder 家族 |
| BAD_PACKETS | 使用物品期间攻击 / 单 tick 多次旋转包 | GrimAC BadPackets 家族 |
| 代理配置化 | lobby 与通知文案可配置 | KaelorvynGuardProxy config.properties |

参考来源按 GPL-3.0 私人内部使用许可落地，代码内均已注明出处注释。

## 构建

中文路径会导致 Gradle 测试类加载失败，构建必须在 ASCII 临时目录执行：

```powershell
$env:JAVA_HOME='D:\Java\jdk-25'
$env:PACKETEVENTS_JAR='D:/MC/server/[25568]lifesteal生存/plugins/packetevents-spigot-2.13.0.jar'
# 复制工程到 C:\Users\Administrator\Documents\Codex\kg-build-tmp 后：
.\gradlew.bat clean build --no-daemon
```

产物 `build\libs\KaelorvynGuard-1.0.0.jar` 拷回项目 `outputs\`。

## 部署

1. 将 `outputs\KaelorvynGuard-1.0.0.jar` 放入 `D:\MC\server\[25568]lifesteal生存\plugins\`
2. 确认 `packetevents-spigot-2.13.0.jar` 已在 plugins（已安装）
3. 首次启动生成 `plugins\KaelorvynGuard\config.yml`，按需配置：
   - `ai.api-key`：留空则读环境变量 `AGNES_API_KEY`
   - `storage.database.*`：MariaDB 连接（长期记忆/警告库/抗体库）
   - `storage.kaelorvynban.config-file`：可指向代理端 KaelorvynBan config.properties 自动读库
   - `punish.kick` / `punish.ban`：**默认 false，灰度只记录告警**
4. 灰度确认无误报后，再把 `punish.kick`、`punish.ban` 打开

## 指令

- `/kg reload` 重载配置
- `/kg info` 查看状态（封包模式/抗体库/AI）
- `/kg stats <玩家>` 查看检测统计
- `/kg review <玩家>` 手动提交 AI 复核
- `/kg warn <玩家> <原因>` 手动警告
- `/kg notice` 测试进服通知
- `/kg testkick` 测试踢出界面
- `/kg antibody <玩家> <家族>` 确认抗体
- `/kg trust <玩家>` 加入可信名单

## 权限

- `kg.admin`：管理指令（默认 OP）
- `kg.alerts`：接收告警（默认 OP）
- `kg.bypass`：绕过全部检测
- `kg.notice.bypass`：不接收进服通知

## 已知边界

- v1 指纹库只内置少量已解包外挂特征，后续抓到新样本走"解包 → 加指纹"流程
- 封禁对接依赖 KaelorvynBan 数据库结构，写入失败自动回退 Bukkit 封禁名单
- 代理端（Velocity）的大厅重定向与切服通知为后续补丁
- 所有阈值默认宽松，先灰度后收紧
