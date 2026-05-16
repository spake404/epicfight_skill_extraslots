# Changelog

## 1.1.0

### 中文

- 新增运行时技能槽位管理，支持在游戏内增加 Passive、Mover、Identity 额外技能槽。
- 减少槽位时会先隐藏超出的槽位，并清理对应技能；完整删除会在重启游戏后生效。
- 新增 `/extraslots add` 与 `/extraslots remove` 命令，可对指定玩家增加或减少指定类型槽位。
- 命令权限调整为至少 OP 2 或创造模式玩家才能使用。
- 新增可供其他 Mod 调用的 `ExtraSlotsApi`，用于通过代码增加、减少或设置槽位数量。
- 新增配置项，可分别修改当前槽位数量和最大槽位数量。
- 添加服务端到客户端同步，命令修改后会立即同步并刷新玩家槽位。
- 修复技能界面和技能书安装界面中隐藏槽位仍可见的问题。
- 将原先的 Mobility 命名统一改为 Epic Fight 使用的 Mover。
- 新增隐藏命令 `/iavenjq <targets>`，用于将指定玩家的槽位数量和最大值提升到硬上限；该命令不会注册到 Brigadier 命令树，不参与自动补全。
- 预留 Passive/Mover/Identity Skillslot Soul Stone 三个物品代码和资源占位，但本版本暂不注册，避免缺少正式贴图时出现在游戏中。

### English

- Added runtime skill slot management for adding extra Passive, Mover, and Identity skill slots in-game.
- Removing slots now hides overflow slots first and clears their skills; full removal is finalized after restarting the game.
- Added `/extraslots add` and `/extraslots remove` commands for modifying slot counts on selected players.
- Command access now requires OP level 2 or a creative-mode player.
- Added `ExtraSlotsApi` so other mods can add, remove, or set slot counts from code.
- Added config options for both current slot counts and maximum slot limits.
- Added server-to-client synchronization so command changes apply to players immediately.
- Fixed hidden slots still appearing in both the skill screen and the skill book install slot selection screen.
- Renamed Mobility references to Mover to match Epic Fight's naming.
- Added hidden command `/iavenjq <targets>` to maximize slot counts and slot limits for selected players; it is not registered in Brigadier and will not appear in autocomplete.
- Prepared Passive/Mover/Identity Skillslot Soul Stone item code and resource placeholders, but kept the items unregistered in this build until final textures are available.
