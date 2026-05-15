package yesman.epicfight.api.utils;

import java.util.Collection;
import java.util.List;

public class ExtendableEnumManager<T extends ExtendableEnum> {
	public void registerEnumCls(String modid, Class<? extends ExtendableEnum> cls) {
	}
	
	public int assign(T value) {
		return 0;
	}
	
	public Collection<T> universalValues() {
		return List.of();
	}
}
