# Changelog

## 3.0

### 中文

- 新增可自定义的技能互斥组功能。每个互斥组中的技能同一时间只能装备一个。
- 新增游戏内互斥组配置界面，可自动列出已注册的 Epic Fight 技能，支持搜索、创建组、删除组、添加技能、移除技能和双击重命名组。
- 互斥组界面现在会显示技能图标、技能名称和技能 ID，组列表会显示当前组内技能数量。
- 互斥组配置使用 Forge config 保存，整合包作者和服务器管理员可以直接编辑配置文件。
- 装备互斥技能时会使用 Epic Fight 风格确认提示。确认后会自动卸下冲突技能，再安装新技能。
- 修复在 Epic Fight: Skill Tree 界面装备、替换或解锁技能时互斥检测不弹提示、直接失败或被绕过的问题。
- 优化互斥检测性能，缓存已解析的互斥组配置，减少装备检查时的临时对象创建。
- 重做并优化配置界面与互斥组编辑界面，支持 GUI 缩放 4 和 Auto，修复文字越界、按钮重叠、提示文字不换行等显示问题。
- 魂石合成表中的技能书改为附魔书。
- 未安装 Epic Fight: Skill Tree 时，魂石可以直接右键使用并增加对应技能槽位。
- 未安装 Epic Fight: Skill Tree 时，默认魂石配方不再依赖 Skill Tree 的 ability stone。
- 安装 Epic Fight: Skill Tree 时，魂石仍然保持原有的 Skill Tree 储存与解锁流程。
- 调整 Skill Tree 中魂石数量显示层级，使其与技能石数量显示层级一致。
- 修复安装 Epic Fight: Better Skill Menu 但未安装 Skill Tree 时可能出现的兼容崩溃。

- 优化 Skill Tree 顶部魂石显示布局，使魂石之间的距离会跟随 Ability Stone / Ability Points 的当前间距，并适配不同 GUI 大小。

### English

- Added configurable skill mutual-exclusion groups. Only one skill from each group can be equipped at a time.
- Added an in-game mutual-exclusion editor that automatically lists registered Epic Fight skills and supports search, group creation, group deletion, adding skills, removing skills, and double-click group renaming.
- The mutual-exclusion editor now displays skill icons, skill names, skill IDs, and per-group skill counts.
- Mutual-exclusion groups are saved through Forge config so modpack authors and server administrators can edit them directly.
- Equipping a conflicting skill now uses an Epic Fight-style confirmation prompt. Confirming automatically unequips the conflicting skill before installing the new one.
- Fixed Epic Fight: Skill Tree equip, replace, and unlock flows not showing the mutual-exclusion prompt, failing directly, or bypassing checks.
- Optimized mutual-exclusion checks by caching parsed group config data and reducing temporary allocations during equip checks.
- Reworked and polished the config and mutual-exclusion editor screens, including support for GUI scale 4 and Auto, fixing text overflow, overlapping buttons, and non-wrapping hint text.
- Soul Stone recipes now use enchanted books instead of skill books.
- When Epic Fight: Skill Tree is not installed, Soul Stones can be used directly to add the corresponding extra skill slot.
- When Epic Fight: Skill Tree is not installed, default Soul Stone recipes no longer depend on Skill Tree ability stones.
- When Epic Fight: Skill Tree is installed, Soul Stones still follow the original Skill Tree storage and unlock path.
- Adjusted the Soul Stone count render layer in Skill Tree so it matches the ability stone count layer.
- Improved the Skill Tree top-bar Soul Stone layout so Soul Stone spacing follows the Ability Stone/Ability Points spacing across different GUI sizes.
- Fixed a compatibility crash that could occur when Epic Fight: Better Skill Menu was installed without Epic Fight: Skill Tree.
