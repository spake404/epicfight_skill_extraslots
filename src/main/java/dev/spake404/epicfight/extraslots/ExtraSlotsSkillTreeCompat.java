package dev.spake404.epicfight.extraslots;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntSupplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class ExtraSlotsSkillTreeCompat {
	static final String EPICSKILLS_MODID = "epicskills";
	static final String TREE_NAME = "extraslot";
	private static final int FULL_VERIFY_INTERVAL_TICKS = 100;
	private static final String PACK_ID = EpicFightSkillExtraSlots.MODID + "_generated_skilltree";
	private static final int TREE_PRIORITY = 200;
	private static final int NODE_START_X = 20;
	private static final int NODE_SPACING_X = 90;
	private static final Map<SlotGroup, Map<Integer, Skill>> SKILLS = new EnumMap<>(SlotGroup.class);
	private static final Map<UUID, ExtraSlotCounts> LAST_SYNCED = new LinkedHashMap<>();
	private static final Map<UUID, ExtraSlotCounts> LAST_SYNCED_SOUL_STONES = new LinkedHashMap<>();
	private static final Map<UUID, Integer> SOUL_STONE_SYNC_TICKS = new LinkedHashMap<>();
	private static final Map<UUID, Integer> VERIFY_TICKS = new LinkedHashMap<>();
	private static final Map<UUID, Set<String>> CONFIRMED_UNLOCKS = new LinkedHashMap<>();
	private static final Set<UUID> DIRTY_PLAYERS = new HashSet<>();
	private static final Set<UUID> COSTS_INITIALIZED = new HashSet<>();

	private ExtraSlotsSkillTreeCompat() {
	}

	public static boolean isLoaded() {
		return ModList.get().isLoaded(EPICSKILLS_MODID);
	}

	public static void register(IEventBus modEventBus) {
		modEventBus.addListener(ExtraSlotsSkillTreeCompat::onSkillBuild);
		modEventBus.addListener(ExtraSlotsSkillTreeCompat::onAddPackFinders);
	}

	public static ExtraSlotCounts activeCounts(ServerPlayer player) {
		if (!isLoaded()) {
			return ExtraSlotCounts.configured();
		}

		return ExtraSlotsSkillTreeHooks.countsFor(player);
	}

	public static ExtraSlotCounts storedSoulStones(ServerPlayer player) {
		CompoundTag tag = soulStoneTag(player);
		return new ExtraSlotCounts(
			tag.getInt(SlotGroup.PASSIVE.storageKey()),
			tag.getInt(SlotGroup.MOVER.storageKey()),
			tag.getInt(SlotGroup.IDENTITY.storageKey())
		);
	}

	static void rememberSyncedSoulStones(ServerPlayer player, ExtraSlotCounts counts) {
		UUID uuid = player.getUUID();
		LAST_SYNCED_SOUL_STONES.put(uuid, counts);
		SOUL_STONE_SYNC_TICKS.remove(uuid);
	}

	public static int addSoulStone(ServerPlayer player, SlotGroup group, int amount) {
		CompoundTag tag = soulStoneTag(player);
		int count = Math.max(0, tag.getInt(group.storageKey()) + amount);
		tag.putInt(group.storageKey(), count);
		markSoulStonesDirty(player);
		markDirty(player);
		ExtraSlotsNetwork.syncSoulStones(player);
		return count;
	}

	public static boolean consumeSoulStone(ServerPlayer player, SlotGroup group) {
		CompoundTag tag = soulStoneTag(player);
		int count = tag.getInt(group.storageKey());

		if (count <= 0) {
			return false;
		}

		tag.putInt(group.storageKey(), count - 1);
		markSoulStonesDirty(player);
		markDirty(player);
		return true;
	}

	static Skill skill(SlotGroup group, int slotIndex) {
		Map<Integer, Skill> groupSkills = SKILLS.get(group);
		return groupSkills == null ? null : groupSkills.get(slotIndex);
	}

	private static void onSkillBuild(SkillBuildEvent event) {
		if (!isLoaded()) {
			return;
		}

		SkillBuildEvent.ModRegistryWorker worker = event.createRegistryWorker(EpicFightSkillExtraSlots.MODID);

		for (SlotGroup group : SlotGroup.values()) {
			Map<Integer, Skill> groupSkills = SKILLS.computeIfAbsent(group, ignored -> new LinkedHashMap<>());

			for (int slotIndex = group.baseSlots() + 1; slotIndex <= group.registeredMaxSlots(); slotIndex++) {
				String skillName = group.skillName(slotIndex);
				SkillBuilder<Skill> builder = Skill.createBuilder()
					.setCategory(ExtraSlotUnlockCategories.get(group, slotIndex))
					.setActivateType(Skill.ActivateType.ONE_SHOT)
					.setResource(Skill.Resource.NONE);
				int index = slotIndex;
				Skill skill = worker.build(skillName, configuredBuilder -> new ExtraSlotUnlockSkill(configuredBuilder, group, index), builder);
				groupSkills.put(slotIndex, skill);
			}
		}
	}

	private static void onAddPackFinders(AddPackFindersEvent event) {
		if (!isLoaded() || event.getPackType() != PackType.SERVER_DATA) {
			return;
		}

		Path packRoot = FMLPaths.CONFIGDIR.get().resolve(EpicFightSkillExtraSlots.MODID + "_skilltree_pack");

		try {
			writeGeneratedPack(packRoot);
		} catch (IOException ignored) {
			return;
		}

		event.addRepositorySource(consumer -> {
			Pack pack = Pack.readMetaAndCreate(
				PACK_ID,
				Component.literal("Epic Fight Skill Extra Slots Skill Tree"),
				true,
				id -> new PathPackResources(id, packRoot, false),
				PackType.SERVER_DATA,
				Pack.Position.TOP,
				PackSource.BUILT_IN
			);

			if (pack != null) {
				consumer.accept(pack);
			}
		});
	}

	private static void writeGeneratedPack(Path packRoot) throws IOException {
		Files.createDirectories(packRoot);
		write(packRoot.resolve("pack.mcmeta"), """
			{
			  "pack": {
			    "pack_format": 15,
			    "description": "Generated extra skill slot tree"
			  }
			}
			""");

		Path treePath = packRoot.resolve("data").resolve(EpicFightSkillExtraSlots.MODID).resolve("epicskills").resolve("tree").resolve(TREE_NAME + ".json");
		Path entryPath = packRoot.resolve("data").resolve(EpicFightSkillExtraSlots.MODID).resolve("epicskills").resolve("entry").resolve(TREE_NAME + ".json");
		Files.createDirectories(treePath.getParent());
		Files.createDirectories(entryPath.getParent());

		write(treePath, """
			{
			  "hidden": false,
			  "locked": false,
			  "menu_color": [
			    52,
			    94,
			    89
			  ],
			  "priority": %s
			}
			""".formatted(TREE_PRIORITY));
		write(entryPath, buildEntryJson());
	}

	private static String buildEntryJson() {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n  \"nodes\": [\n");
		boolean first = true;

		for (SlotGroup group : SlotGroup.values()) {
			for (int slotIndex = group.baseSlots() + 1; slotIndex <= group.startupMaxSlots(); slotIndex++) {
				if (!first) {
					builder.append(",\n");
				}

				first = false;
				appendNode(builder, group, slotIndex);
			}
		}

		builder.append("\n  ]\n}\n");
		return builder.toString();
	}

	private static void appendNode(StringBuilder builder, SlotGroup group, int slotIndex) {
		int extraIndex = slotIndex - group.baseSlots();
		int x = NODE_START_X + (extraIndex - 1) * NODE_SPACING_X;
		int y = group.rowY();

		builder.append("    {\n");
		builder.append("      \"ability_points\": 0,\n");
		builder.append("      \"hidden\": false,\n");

		if (slotIndex > group.baseSlots() + 1) {
			builder.append("      \"parents\": [\n");
			builder.append("        {\n");
			builder.append("          \"skill\": \"").append(EpicFightSkillExtraSlots.MODID).append(':').append(group.skillName(slotIndex - 1)).append("\"\n");
			builder.append("        }\n");
			builder.append("      ],\n");
		}

		builder.append("      \"position_in_screen\": [\n");
		builder.append("        ").append(x).append(",\n");
		builder.append("        ").append(y).append("\n");
		builder.append("      ],\n");
		builder.append("      \"skill\": \"").append(EpicFightSkillExtraSlots.MODID).append(':').append(group.skillName(slotIndex)).append("\"\n");
		builder.append("    }");
	}

	private static void write(Path path, String content) throws IOException {
		Files.writeString(path, content, StandardCharsets.UTF_8);
	}

	@Mod.EventBusSubscriber(modid = EpicFightSkillExtraSlots.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static final class Events {
		private Events() {
		}

		@SubscribeEvent
		public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
			if (!isLoaded() || event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
				return;
			}

			syncSoulStonesIfNeeded(player);

			if (!shouldVerify(player)) {
				return;
			}

			PlayerPatch<?> playerPatch = EpicFightCapabilities.getPlayerPatch(player);
			ExtraSlotsSkillTreeHooks.VerificationResult result = ExtraSlotsSkillTreeHooks.verifySoulStoneCosts(player, confirmedUnlocks(player), COSTS_INITIALIZED.add(player.getUUID()));

			if (playerPatch != null) {
				ExtraSlotsRuntimeExpander.moveUnlockMarkerSkillsToHiddenSlots(playerPatch.getSkillCapability(), player);
			}

			ExtraSlotCounts counts = activeCounts(player);
			applyCountsIfNeeded(player, playerPatch, counts, result.slotsChanged());

			if (result.soulStonesChanged()) {
				syncSoulStonesIfNeeded(player);
			}
		}

		@SubscribeEvent
		public static void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
			if (event.getEntity() instanceof ServerPlayer player) {
				clearPlayerState(player.getUUID());
			}
		}
	}

	private static boolean shouldVerify(ServerPlayer player) {
		UUID uuid = player.getUUID();

		if (DIRTY_PLAYERS.remove(uuid)) {
			VERIFY_TICKS.put(uuid, FULL_VERIFY_INTERVAL_TICKS);
			return true;
		}

		int ticks = VERIFY_TICKS.getOrDefault(uuid, 0);
		if (ticks <= 0) {
			VERIFY_TICKS.put(uuid, FULL_VERIFY_INTERVAL_TICKS);
			return true;
		}

		VERIFY_TICKS.put(uuid, ticks - 1);
		return false;
	}

	private static void applyCountsIfNeeded(ServerPlayer player, PlayerPatch<?> playerPatch, ExtraSlotCounts counts, boolean forceSync) {
		if (forceSync || !counts.equals(LAST_SYNCED.get(player.getUUID()))) {
			LAST_SYNCED.put(player.getUUID(), counts);

			if (playerPatch != null) {
				ExtraSkillSlots.applyConfiguredSlots();
				ExtraSlotsRuntimeExpander.expandAndClean(playerPatch, counts, player);
			}

			ExtraSlotsNetwork.sync(player, counts);
		}
	}

	private static void markSoulStonesDirty(ServerPlayer player) {
		SOUL_STONE_SYNC_TICKS.put(player.getUUID(), 1);
	}

	static void markDirty(ServerPlayer player) {
		DIRTY_PLAYERS.add(player.getUUID());
	}

	public static boolean isConfirmedUnlock(ServerPlayer player, SlotGroup group, int slotIndex) {
		return confirmedUnlocks(player).contains(unlockKey(group, slotIndex));
	}

	public static void confirmUnlock(ServerPlayer player, SlotGroup group, int slotIndex) {
		confirmedUnlocks(player).add(unlockKey(group, slotIndex));
		markDirty(player);
	}

	static void removeConfirmedUnlock(ServerPlayer player, SlotGroup group, int slotIndex) {
		confirmedUnlocks(player).remove(unlockKey(group, slotIndex));
		markDirty(player);
	}

	static String unlockKey(SlotGroup group, int slotIndex) {
		return group.name() + ":" + slotIndex;
	}

	private static void syncSoulStonesIfNeeded(ServerPlayer player) {
		UUID uuid = player.getUUID();
		int dirtyTicks = SOUL_STONE_SYNC_TICKS.getOrDefault(uuid, 0);

		if (dirtyTicks <= 0 && LAST_SYNCED_SOUL_STONES.containsKey(uuid)) {
			return;
		}

		ExtraSlotCounts counts = storedSoulStones(player);

		if (dirtyTicks > 0 || !counts.equals(LAST_SYNCED_SOUL_STONES.get(uuid))) {
			ExtraSlotsNetwork.syncSoulStones(player);
		}
	}

	private static Set<String> confirmedUnlocks(ServerPlayer player) {
		return CONFIRMED_UNLOCKS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
	}

	private static void clearPlayerState(UUID uuid) {
		LAST_SYNCED.remove(uuid);
		LAST_SYNCED_SOUL_STONES.remove(uuid);
		SOUL_STONE_SYNC_TICKS.remove(uuid);
		VERIFY_TICKS.remove(uuid);
		CONFIRMED_UNLOCKS.remove(uuid);
		DIRTY_PLAYERS.remove(uuid);
		COSTS_INITIALIZED.remove(uuid);
	}

	private static CompoundTag soulStoneTag(ServerPlayer player) {
		CompoundTag persistentData = player.getPersistentData();
		CompoundTag persisted;

		if (persistentData.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
			persisted = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
		} else {
			persisted = new CompoundTag();
			persistentData.put(Player.PERSISTED_NBT_TAG, persisted);
		}

		CompoundTag modData;

		if (persisted.contains(EpicFightSkillExtraSlots.MODID, Tag.TAG_COMPOUND)) {
			modData = persisted.getCompound(EpicFightSkillExtraSlots.MODID);
		} else {
			modData = new CompoundTag();
			persisted.put(EpicFightSkillExtraSlots.MODID, modData);
		}

		return modData;
	}

	public enum SlotGroup {
		PASSIVE(
			"passive",
			ExtraSlotsConfig.BASE_PASSIVE_SLOTS,
			ExtraSlotsConfig::startupMaxPassiveSlots,
			"passive_slot",
			ExtraSlotsItems.PASSIVE_SKILLSLOT_SOUL_STONE,
			ResourceLocation.fromNamespaceAndPath("epicfight", "textures/gui/skills/passive/technician.png"),
			30
		),
		MOVER(
			"mover",
			ExtraSlotsConfig.BASE_MOVER_SLOTS,
			ExtraSlotsConfig::startupMaxMoverSlots,
			"mover_slot",
			ExtraSlotsItems.MOVER_SKILLSLOT_SOUL_STONE,
			ResourceLocation.fromNamespaceAndPath("epicfight", "textures/gui/skills/mover/phantom_ascent.png"),
			150
		),
		IDENTITY(
			"identity",
			ExtraSlotsConfig.BASE_IDENTITY_SLOTS,
			ExtraSlotsConfig::startupMaxIdentitySlots,
			"identity_slot",
			ExtraSlotsItems.IDENTITY_SKILLSLOT_SOUL_STONE,
			ResourceLocation.fromNamespaceAndPath("epicfight", "textures/gui/skills/identity/revelation.png"),
			270
		);

		private final String id;
		private final int baseSlots;
		private final IntSupplier startupMaxSlots;
		private final String translationKeySuffix;
		private final RegistryObject<Item> soulStone;
		private final ResourceLocation icon;
		private final int rowY;

		SlotGroup(String id, int baseSlots, IntSupplier startupMaxSlots, String translationKeySuffix, RegistryObject<Item> soulStone, ResourceLocation icon, int rowY) {
			this.id = id;
			this.baseSlots = baseSlots;
			this.startupMaxSlots = startupMaxSlots;
			this.translationKeySuffix = translationKeySuffix;
			this.soulStone = soulStone;
			this.icon = icon;
			this.rowY = rowY;
		}

		int baseSlots() {
			return this.baseSlots;
		}

		int startupMaxSlots() {
			return this.startupMaxSlots.getAsInt();
		}

		int registeredMaxSlots() {
			return switch (this) {
				case PASSIVE -> ExtraSlotsConfig.BASE_PASSIVE_SLOTS + ExtraSlotsConfig.HARD_MAX_PASSIVE_SLOTS;
				case MOVER -> ExtraSlotsConfig.BASE_MOVER_SLOTS + ExtraSlotsConfig.HARD_MAX_MOVER_SLOTS;
				case IDENTITY -> ExtraSlotsConfig.BASE_IDENTITY_SLOTS + ExtraSlotsConfig.HARD_MAX_IDENTITY_SLOTS;
			};
		}

		String skillName(int slotIndex) {
			return this.id + "_slot_" + slotIndex;
		}

		String translationKeySuffix() {
			return this.translationKeySuffix;
		}

		public RegistryObject<Item> soulStone() {
			return this.soulStone;
		}

		ResourceLocation icon() {
			return this.icon;
		}

		int nodeOffsetX() {
			return this == MOVER ? 5 : this == IDENTITY ? 6 : 3;
		}

		int nodeOffsetY() {
			return this == MOVER ? 5 : this == IDENTITY ? 6 : 3;
		}

		int nodeTextureWidth() {
			return this == MOVER ? 42 : this == IDENTITY ? 44 : 38;
		}

		int nodeTextureHeight() {
			return this == MOVER ? 42 : this == IDENTITY ? 44 : 38;
		}

		boolean isEquipped(CapabilitySkill skills, Skill skill) {
			return skills != null && skill != null && skills.isEquipping(skill);
		}

		String storageKey() {
			return this.id + "_soul_stones";
		}

		int rowY() {
			return this.rowY;
		}
	}
}
