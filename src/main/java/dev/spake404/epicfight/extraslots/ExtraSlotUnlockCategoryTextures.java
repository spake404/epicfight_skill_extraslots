package dev.spake404.epicfight.extraslots;

import java.util.LinkedHashMap;
import java.util.Map;

import com.yesman.epicskills.client.gui.screen.CategorySlotTexture;

public final class ExtraSlotUnlockCategoryTextures implements CategorySlotTexture {
	private static final Map<String, ExtraSlotUnlockCategoryTextures> REGISTERED = new LinkedHashMap<>();
	private static ExtraSlotUnlockCategoryTextures[] values;
	
	private final String name;
	private final int offsetX;
	private final int offsetY;
	private final int texWidth;
	private final int texHeight;
	private final int id;
	
	private ExtraSlotUnlockCategoryTextures(ExtraSlotUnlockCategories category) {
		this.name = category.toString();
		this.offsetX = category.group().nodeOffsetX();
		this.offsetY = category.group().nodeOffsetY();
		this.texWidth = category.group().nodeTextureWidth();
		this.texHeight = category.group().nodeTextureHeight();
		this.id = CategorySlotTexture.ENUM_MANAGER.assign(this);
	}
	
	public static ExtraSlotUnlockCategoryTextures[] values() {
		ensureTextures();
		return values.clone();
	}
	
	private static synchronized void ensureTextures() {
		for (ExtraSlotUnlockCategories category : ExtraSlotUnlockCategories.values()) {
			REGISTERED.computeIfAbsent(category.toString(), ignored -> new ExtraSlotUnlockCategoryTextures(category));
		}
		
		values = REGISTERED.values().toArray(ExtraSlotUnlockCategoryTextures[]::new);
	}
	
	@Override
	public int offsetX() {
		return this.offsetX;
	}
	
	@Override
	public int offsetY() {
		return this.offsetY;
	}
	
	@Override
	public int texWidth() {
		return this.texWidth;
	}
	
	@Override
	public int texHeight() {
		return this.texHeight;
	}
	
	@Override
	public int universalOrdinal() {
		return this.id;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
}
