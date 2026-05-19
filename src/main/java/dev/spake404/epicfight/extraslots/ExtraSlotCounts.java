package dev.spake404.epicfight.extraslots;

record ExtraSlotCounts(int passiveSlots, int moverSlots, int identitySlots) {
	static ExtraSlotCounts configured() {
		return new ExtraSlotCounts(ExtraSlotsConfig.passiveSlots(), ExtraSlotsConfig.moverSlots(), ExtraSlotsConfig.identitySlots());
	}
	
	static ExtraSlotCounts base() {
		return new ExtraSlotCounts(ExtraSlotsConfig.BASE_PASSIVE_SLOTS, ExtraSlotsConfig.BASE_MOVER_SLOTS, ExtraSlotsConfig.BASE_IDENTITY_SLOTS);
	}
}
