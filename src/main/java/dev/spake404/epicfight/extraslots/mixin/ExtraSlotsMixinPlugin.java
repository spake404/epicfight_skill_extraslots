package dev.spake404.epicfight.extraslots.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class ExtraSlotsMixinPlugin implements IMixinConfigPlugin {
	private static final String BETTER_SKILL_MENU_RANGED_COMPAT = "dev.spake404.epicfight.extraslots.mixin.BetterSkillMenuRangedCompatMixin";
	private static final String BETTER_SKILL_MENU_SKILL_UI = "dev.spake404.epicfight.extraslots.mixin.BetterSkillMenuSkillUiMixin";
	private static final String EXTRA_SLOTS_UNLOCK_REQUEST = "dev.spake404.epicfight.extraslots.mixin.ExtraSlotsServerBoundUnlockSkillRequestMixin";
	private static final String EXTRA_SLOTS_SKILL_INFO_SCREEN = "dev.spake404.epicfight.extraslots.mixin.ExtraSlotsSkillInfoScreenMixin";
	private static final String BETTER_SKILL_MENU_SKILL_UI_TARGET = "org.merlin204.bsm.client.gui.SkillUI";
	private static final String BETTER_SKILL_MENU_INFO_TARGET = "org.merlin204.bsm.client.gui.SkillInfoWidgetGroup";
	private static final String EPICSKILLS_UNLOCK_REQUEST_TARGET = "com.yesman.epicskills.network.server.ServerBoundUnlockSkillRequest";
	private static final String EPICSKILLS_SKILL_INFO_SCREEN_TARGET = "com.yesman.epicskills.client.gui.screen.SkillInfoScreen";
	
	@Override
	public void onLoad(String mixinPackage) {
	}
	
	@Override
	public String getRefMapperConfig() {
		return null;
	}
	
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (BETTER_SKILL_MENU_RANGED_COMPAT.equals(mixinClassName)) {
			return BETTER_SKILL_MENU_INFO_TARGET.equals(targetClassName) && !epicFightHasRangedWeaponCategory();
		}
		
		if (BETTER_SKILL_MENU_SKILL_UI.equals(mixinClassName)) {
			return BETTER_SKILL_MENU_SKILL_UI_TARGET.equals(targetClassName);
		}
		
		if (EXTRA_SLOTS_UNLOCK_REQUEST.equals(mixinClassName)) {
			return EPICSKILLS_UNLOCK_REQUEST_TARGET.equals(targetClassName);
		}

		if (EXTRA_SLOTS_SKILL_INFO_SCREEN.equals(mixinClassName)) {
			return EPICSKILLS_SKILL_INFO_SCREEN_TARGET.equals(targetClassName);
		}
		
		return true;
	}
	
	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}
	
	@Override
	public List<String> getMixins() {
		return null;
	}
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
	
	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
	
	private static boolean epicFightHasRangedWeaponCategory() {
		try {
			Class<?> weaponCategories = Class.forName("yesman.epicfight.world.capabilities.item.CapabilityItem$WeaponCategories", false, ExtraSlotsMixinPlugin.class.getClassLoader());
			weaponCategories.getField("RANGED");
			return true;
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return false;
		}
	}
}
