package dev.spake404.epicfight.extraslots;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ExtraSlotsItems {
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EpicFightSkillExtraSlots.MODID);
	
	public static final RegistryObject<Item> PASSIVE_SKILLSLOT_SOUL_STONE = ITEMS.register("passive_skillslot_soul_stone", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> MOVER_SKILLSLOT_SOUL_STONE = ITEMS.register("mover_skillslot_soul_stone", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> IDENTITY_SKILLSLOT_SOUL_STONE = ITEMS.register("identity_skillslot_soul_stone", () -> new Item(new Item.Properties()));
	
	private ExtraSlotsItems() {
	}
	
	public static void register(IEventBus modEventBus) {
		ITEMS.register(modEventBus);
	}
}
