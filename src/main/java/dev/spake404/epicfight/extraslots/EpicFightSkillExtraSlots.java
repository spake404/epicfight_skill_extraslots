package dev.spake404.epicfight.extraslots;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import yesman.epicfight.skill.SkillSlot;

@Mod(EpicFightSkillExtraSlots.MODID)
public class EpicFightSkillExtraSlots {
	public static final String MODID = "epicfight_skill_extraslots";
	
	public EpicFightSkillExtraSlots(FMLJavaModLoadingContext context) {
		context.registerConfig(ModConfig.Type.COMMON, ExtraSlotsConfig.SPEC);
		ExtraSlotsNetwork.register();
		SkillSlot.ENUM_MANAGER.registerEnumCls(MODID, ExtraSkillSlots.class);
		
		if (FMLEnvironment.dist == Dist.CLIENT) {
			context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory(ExtraSlotsConfigScreen::new));
		}
	}
}
