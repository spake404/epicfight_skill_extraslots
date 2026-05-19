package dev.spake404.epicfight.extraslots;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.world.item.SkillBookItem;

public final class ExtraSlotsItems {
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EpicFightSkillExtraSlots.MODID);
	private static final ResourceKey<CreativeModeTab> EPICFIGHT_ITEMS_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("epicfight", "items"));
	private static final ResourceLocation GAIN_ABILITY_POINTS_SOUND = ResourceLocation.fromNamespaceAndPath("epicskills", "ui.gain_ability_points");
	
	public static final RegistryObject<Item> PASSIVE_SKILLSLOT_SOUL_STONE = ITEMS.register("passive_skillslot_soul_stone", () -> new SoulStoneItem("passive", ExtraSlotsSkillTreeCompat.SlotGroup.PASSIVE));
	public static final RegistryObject<Item> MOVER_SKILLSLOT_SOUL_STONE = ITEMS.register("mover_skillslot_soul_stone", () -> new SoulStoneItem("mover", ExtraSlotsSkillTreeCompat.SlotGroup.MOVER));
	public static final RegistryObject<Item> IDENTITY_SKILLSLOT_SOUL_STONE = ITEMS.register("identity_skillslot_soul_stone", () -> new SoulStoneItem("identity", ExtraSlotsSkillTreeCompat.SlotGroup.IDENTITY));
	
	private ExtraSlotsItems() {
	}
	
	public static void register(IEventBus modEventBus) {
		ITEMS.register(modEventBus);
		modEventBus.addListener(EventPriority.LOWEST, ExtraSlotsItems::onBuildCreativeTab);
	}
	
	private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
		List<ItemStack> hiddenSkillBooks = new ArrayList<>();
		
		for (Map.Entry<ItemStack, CreativeModeTab.TabVisibility> entry : event.getEntries()) {
			if (isExtraSlotSkillBook(entry.getKey())) {
				hiddenSkillBooks.add(entry.getKey());
			}
		}
		
		hiddenSkillBooks.forEach(event.getEntries()::remove);
		
		if (event.getTabKey().equals(EPICFIGHT_ITEMS_TAB)) {
			event.accept(PASSIVE_SKILLSLOT_SOUL_STONE);
			event.accept(MOVER_SKILLSLOT_SOUL_STONE);
			event.accept(IDENTITY_SKILLSLOT_SOUL_STONE);
		}
	}
	
	private static boolean isExtraSlotSkillBook(ItemStack stack) {
		return stack.getItem() instanceof SkillBookItem && SkillBookItem.getContainSkill(stack) instanceof ExtraSlotUnlockSkill;
	}
	
	private static final class SoulStoneItem extends Item {
		private final String group;
		private final ExtraSlotsSkillTreeCompat.SlotGroup slotGroup;
		
		private SoulStoneItem(String group, ExtraSlotsSkillTreeCompat.SlotGroup slotGroup) {
			super(new Item.Properties());
			this.group = group;
			this.slotGroup = slotGroup;
		}
		
		@Override
		public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(GAIN_ABILITY_POINTS_SOUND);
			
			if (sound != null) {
				player.playSound(sound, 1.0F, 1.0F);
			}
			
			if (level.isClientSide()) {
				return InteractionResultHolder.success(stack);
			}
			
			if (player instanceof ServerPlayer serverPlayer) {
				ExtraSlotsSkillTreeCompat.addSoulStone(serverPlayer, this.slotGroup, 1);
				
				if (!serverPlayer.getAbilities().instabuild) {
					stack.shrink(1);
				}
			}
			
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		}
		
		@Override
		public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
			tooltip.add(Component.translatable("item." + EpicFightSkillExtraSlots.MODID + "." + this.group + "_skillslot_soul_stone.desc"));
		}
	}
}
