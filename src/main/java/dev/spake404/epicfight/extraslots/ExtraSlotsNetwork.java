package dev.spake404.epicfight.extraslots;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ExtraSlotsNetwork {
	private static final String PROTOCOL_VERSION = "1";
	private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(EpicFightSkillExtraSlots.MODID, "main"),
		() -> PROTOCOL_VERSION,
		PROTOCOL_VERSION::equals,
		PROTOCOL_VERSION::equals
	);
	
	private ExtraSlotsNetwork() {
	}
	
	public static void register() {
		CHANNEL.registerMessage(0, SyncSlotsPacket.class, SyncSlotsPacket::encode, SyncSlotsPacket::decode, SyncSlotsPacket::handle);
	}
	
	public static void sync(ServerPlayer player) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSlotsPacket(ExtraSlotsConfig.passiveSlots(), ExtraSlotsConfig.moverSlots(), ExtraSlotsConfig.identitySlots()));
	}
	
	@Mod.EventBusSubscriber(modid = EpicFightSkillExtraSlots.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static final class Events {
		private Events() {
		}
		
		@SubscribeEvent
		public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
			if (event.getEntity() instanceof ServerPlayer player) {
				sync(player);
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
}
