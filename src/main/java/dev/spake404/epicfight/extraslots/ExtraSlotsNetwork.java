package dev.spake404.epicfight.extraslots;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPSetRemotePlayerSkill;
import yesman.epicfight.network.server.SPSetSkillContainerValue;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

public final class ExtraSlotsNetwork {
	private static final String PROTOCOL_VERSION = "1";
	private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		ResourceLocation.fromNamespaceAndPath(EpicFightSkillExtraSlots.MODID, "main"),
		() -> PROTOCOL_VERSION,
		PROTOCOL_VERSION::equals,
		PROTOCOL_VERSION::equals
	);
	
	private ExtraSlotsNetwork() {
	}
	
	public static void register() {
		CHANNEL.registerMessage(0, SyncSlotsPacket.class, SyncSlotsPacket::encode, SyncSlotsPacket::decode, SyncSlotsPacket::handle);
		CHANNEL.registerMessage(1, ClientBoundSetSoulStonesPacket.class, ClientBoundSetSoulStonesPacket::encode, ClientBoundSetSoulStonesPacket::decode, ClientBoundSetSoulStonesPacket::handle);
		CHANNEL.registerMessage(2, ServerBoundConfirmMutualExclusionReplacePacket.class, ServerBoundConfirmMutualExclusionReplacePacket::encode, ServerBoundConfirmMutualExclusionReplacePacket::decode, ServerBoundConfirmMutualExclusionReplacePacket::handle);
	}

	public static void sendConfirmedSkillChange(SkillSlot slot, int skillBookSlotIndex, Skill skill) {
		CHANNEL.sendToServer(new ServerBoundConfirmMutualExclusionReplacePacket(slot, skillBookSlotIndex, skill));
	}
	
	public static void sync(ServerPlayer player) {
		ExtraSlotCounts counts = ExtraSlotsSkillTreeCompat.activeCounts(player);
		sync(player, counts);
	}
	
	static void sync(ServerPlayer player, ExtraSlotCounts counts) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSlotsPacket(counts.passiveSlots(), counts.moverSlots(), counts.identitySlots()));
	}

	public static void syncSkillContainer(ServerPlayer player, SkillContainer container) {
		if (player == null || container == null) {
			return;
		}

		EpicFightNetworkManager.sendToPlayer(container.createSyncPacketToLocalPlayer(), player);
		EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(container.createSyncPacketToRemotePlayer(), player);
	}
	
	public static void syncSoulStones(ServerPlayer player) {
		ExtraSlotCounts counts = ExtraSlotsSkillTreeCompat.storedSoulStones(player);
		ExtraSlotsSkillTreeCompat.rememberSyncedSoulStones(player, counts);
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientBoundSetSoulStonesPacket(counts.passiveSlots(), counts.moverSlots(), counts.identitySlots()));
	}
	
	@Mod.EventBusSubscriber(modid = EpicFightSkillExtraSlots.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static final class Events {
		private Events() {
		}
		
		@SubscribeEvent
		public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
			if (event.getEntity() instanceof ServerPlayer player) {
				sync(player);
				syncSoulStones(player);
			}
		}
	}
	
	private record SyncSlotsPacket(int passiveSlots, int moverSlots, int identitySlots) {
		private static void encode(SyncSlotsPacket packet, FriendlyByteBuf buffer) {
			buffer.writeVarInt(packet.passiveSlots);
			buffer.writeVarInt(packet.moverSlots);
			buffer.writeVarInt(packet.identitySlots);
		}
		
		private static SyncSlotsPacket decode(FriendlyByteBuf buffer) {
			return new SyncSlotsPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
		}
		
		private static void handle(SyncSlotsPacket packet, java.util.function.Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ExtraSlotsClientRuntime.applyRemoteCounts(packet.passiveSlots, packet.moverSlots, packet.identitySlots)));
			context.setPacketHandled(true);
		}
	}
	
	private record ClientBoundSetSoulStonesPacket(int passiveStones, int moverStones, int identityStones) {
		private static void encode(ClientBoundSetSoulStonesPacket packet, FriendlyByteBuf buffer) {
			buffer.writeVarInt(packet.passiveStones);
			buffer.writeVarInt(packet.moverStones);
			buffer.writeVarInt(packet.identityStones);
		}
		
		private static ClientBoundSetSoulStonesPacket decode(FriendlyByteBuf buffer) {
			return new ClientBoundSetSoulStonesPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
		}
		
		private static void handle(ClientBoundSetSoulStonesPacket packet, java.util.function.Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ExtraSlotsClientRuntime.applySoulStoneCounts(packet.passiveStones, packet.moverStones, packet.identityStones)));
			context.setPacketHandled(true);
		}
	}

	private record ServerBoundConfirmMutualExclusionReplacePacket(SkillSlot slot, int skillBookSlotIndex, Skill skill) {
		private static void encode(ServerBoundConfirmMutualExclusionReplacePacket packet, FriendlyByteBuf buffer) {
			buffer.writeInt(packet.slot.universalOrdinal());
			buffer.writeInt(packet.skillBookSlotIndex);
			buffer.writeRegistryId(RegistryManager.ACTIVE.getRegistry(SkillManager.SKILL_REGISTRY_KEY), packet.skill);
		}

		private static ServerBoundConfirmMutualExclusionReplacePacket decode(FriendlyByteBuf buffer) {
			SkillSlot slot = (SkillSlot)SkillSlot.ENUM_MANAGER.get(buffer.readInt());
			int skillBookSlotIndex = buffer.readInt();
			Skill skill = buffer.readRegistryId();
			return new ServerBoundConfirmMutualExclusionReplacePacket(slot, skillBookSlotIndex, skill);
		}

		private static void handle(ServerBoundConfirmMutualExclusionReplacePacket packet, java.util.function.Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> {
				ServerPlayer player = context.getSender();
				if (player == null || packet.slot == null || packet.skill == null) {
					return;
				}

				EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class)
					.ifPresent(playerPatch -> handleConfirmedReplace(player, playerPatch, packet));
			});
			context.setPacketHandled(true);
		}

		private static void handleConfirmedReplace(ServerPlayer player, ServerPlayerPatch playerPatch, ServerBoundConfirmMutualExclusionReplacePacket packet) {
			SkillContainer target = playerPatch.getSkill(packet.slot);
			if (target == null || (target.onReplaceCooldown() && packet.skillBookSlotIndex < 0)) {
				syncSkillContainer(player, target);
				return;
			}

			SkillCategory category = packet.skill.getCategory();
			if (category == null || category != target.getSlot().category()) {
				syncSkillContainer(player, target);
				return;
			}

			ExtraSlotsMutualExclusions.Conflict conflict = ExtraSlotsMutualExclusions.findConflict(playerPatch.getSkillCapability(), packet.skill, target).orElse(null);
			if (conflict != null && !conflict.container().setSkill(null)) {
				syncSkillContainer(player, conflict.container());
				syncSkillContainer(player, target);
				return;
			}

			boolean changed = target.setSkill(packet.skill);
			if (!changed) {
				if (conflict != null) {
					syncSkillContainer(player, conflict.container());
				}
				syncSkillContainer(player, target);
				return;
			}

			if (category.learnable()) {
				playerPatch.getSkillCapability().addLearnedSkill(packet.skill);
			}

			consumeSkillBook(player, packet.skillBookSlotIndex);
			setReplaceCooldown(player, target);
			if (conflict != null) {
				syncSkillContainer(player, conflict.container());
			}
			syncSkillContainer(player, target);
		}

		private static void consumeSkillBook(ServerPlayer player, int skillBookSlotIndex) {
			if (skillBookSlotIndex < 0 || player.isCreative()) {
				return;
			}

			Inventory inventory = player.getInventory();
			if (skillBookSlotIndex < inventory.getContainerSize()) {
				inventory.removeItem(inventory.getItem(skillBookSlotIndex));
			}
		}

		private static void setReplaceCooldown(ServerPlayer player, SkillContainer target) {
			target.setReplaceCooldown(EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(player.level()));
			EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.replaceCooldown(target.getSlot(), target.getReplaceCooldown(), player.getId()), player);
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(new SPSetRemotePlayerSkill(player.getId(), target.getSlot(), target.getSkill()), player);
		}
	}
	
}
