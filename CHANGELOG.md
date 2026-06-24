# Changelog

## 3.0.0

### 中文

- 新增可配置的技能互斥组系统。每个互斥组中的技能同一时间只能装备一个。
- 新增游戏内互斥组编辑界面，可自动列出已注册的 Epic Fight 技能，并支持搜索、创建组、添加和移除技能。
- 互斥组编辑界面现在会显示技能图标，便于识别技能。
- 互斥组配置使用 Forge config 保存，方便整合包或服务器管理员直接编辑。
- 装备互斥技能时会使用 Epic Fight 风格的确认提示：确认后自动卸下冲突技能，再装备新技能。
- 修复在 Epic Fight: Skill Tree 界面装备或替换技能时，互斥检查不弹提示、直接失败的问题。
- 修复 Skill Tree 解锁技能后自动装备可能绕过互斥检查的问题。
- 优化互斥组检测性能，缓存已解析的配置并减少装备检查时的临时对象创建。
- 魂石合成表中的技能书已改为附魔书。
- 当未安装 Epic Fight: Skill Tree 时，魂石可直接右键使用并增加对应技能槽位。
- 当未安装 Epic Fight: Skill Tree 时，会加载不依赖 Skill Tree ability stone 的默认魂石配方。
- 当安装 Epic Fight: Skill Tree 时，魂石仍会按原设计存入 Skill Tree 路径。
- 调整 Skill Tree 中魂石数量显示层级，使其与技能石显示层级一致。
- 修复与 Epic Fight: Better Skill Menu 在未安装 Skill Tree 时可能出现的兼容崩溃。

### English

- Added configurable skill mutual-exclusion groups. Only one skill from each group can be equipped at a time.
- Added an in-game mutual-exclusion editor that automatically lists registered Epic Fight skills and supports search, group creation, adding skills, and removing skills.
- The mutual-exclusion editor now displays skill icons for easier identification.
- Mutual-exclusion groups are saved through Forge config so modpack and server maintainers can edit them directly.
- Equipping a conflicting skill now uses an Epic Fight-style confirmation prompt; confirming automatically unequips the conflicting skill before equipping the new one.
- Fixed Epic Fight: Skill Tree equip and replace flows not showing the mutual-exclusion prompt and failing directly.
- Fixed Skill Tree post-unlock auto-equip potentially bypassing mutual-exclusion checks.
- Optimized mutual-exclusion checks by caching parsed config data and reducing temporary allocations during equip checks.
- Soul Stone recipes now use enchanted books instead of skill books.
- When Epic Fight: Skill Tree is not installed, Soul Stones can be used directly to add the corresponding extra skill slot.
- When Epic Fight: Skill Tree is not installed, default Soul Stone recipes no longer depend on Skill Tree ability stones.
- When Epic Fight: Skill Tree is installed, Soul Stones still follow the Skill Tree storage and unlock path.
- Adjusted the Soul Stone count render layer in Skill Tree so it matches the ability stone count layer.
- Fixed a compatibility crash that could occur with Epic Fight: Better Skill Menu when Skill Tree was not installed.

## 2.3.0

### 中文

- 修复在 Epic Fight: Skill Tree 中学习普通技能时，技能可能被自动安装到未解锁额外技能槽的问题。
- 未解锁的 `PASSIVE4..64`、`MOVER2..64`、`IDENTITY2..64` 现在会保留在底层 `skillContainers` 中，但会从 Epic Fight 的可用分类索引中移除，避免被 `getFirstEmptyContainer` 选中。
- 解锁或关闭额外技能槽后会立即刷新可用槽位索引，并同步清理被禁用槽位中的技能。
- 改进额外槽位识别逻辑：即使同名槽位由其他 Epic Fight 槽位扩展 mod 注册，也会被正确识别和管理。
- 保留 Better Skill Menu 开发依赖，并确认本次槽位索引修复与 Better Skill Menu 兼容层可正常构建。

### English

- Fixed an issue where learning normal skills through Epic Fight: Skill Tree could automatically install them into extra slots that were not unlocked yet.
- Locked `PASSIVE4..64`, `MOVER2..64`, and `IDENTITY2..64` slots now remain in the underlying `skillContainers` array but are removed from Epic Fight's active category index, preventing `getFirstEmptyContainer` from selecting them.
- Refreshes the active slot index immediately after unlocking or closing extra slots, and synchronizes cleanup for disabled slots.
- Improved extra slot detection so same-name slots registered by other Epic Fight slot expansion mods can still be recognized and managed correctly.
- Kept the Better Skill Menu development dependency and verified that this slot-index fix builds with the Better Skill Menu compatibility layer.

## 2.2.0

### 中文

- 修复与其他 Epic Fight 槽位扩展类 mod 共存时可能崩溃的问题。
- 额外技能槽注册前现在会检查 Epic Fight 全局 SkillSlot 枚举中是否已经存在同名槽位。
- 如果 `passive4`、`mover2`、`identity2` 等槽位已被其他 mod 注册，本 mod 会跳过对应名称，避免重复注册导致 `Enum name already exists in skill_slot` 崩溃。

### English

- Fixed a crash that could occur when running alongside other Epic Fight skill slot expansion mods.
- Extra skill slots now check Epic Fight's global SkillSlot enum before registration.
- If slots such as `passive4`, `mover2`, or `identity2` are already registered by another mod, this mod skips those names to avoid `Enum name already exists in skill_slot` crashes.

## 2.1.0

### English

- Optimized Epic Fight: Skill Tree compatibility to avoid full skill tree scans on every player tick.
- Added dirty-state tracking so unlock and Soul Stone changes are handled immediately without constant polling.
- Reduced fallback verification to once every 100 ticks per player.
- Avoided idle-tick Soul Stone NBT reads after the client has received its initial Soul Stone sync.
- Batched slot and Soul Stone synchronization so a single verification pass does not send repeated packets.
- Cleared per-player cached sync state on logout.

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
