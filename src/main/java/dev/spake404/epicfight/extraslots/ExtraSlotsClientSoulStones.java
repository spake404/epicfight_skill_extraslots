package dev.spake404.epicfight.extraslots;

public final class ExtraSlotsClientSoulStones {
	private static ExtraSlotCounts counts = new ExtraSlotCounts(0, 0, 0);
	
	private ExtraSlotsClientSoulStones() {
	}
	
	public static void set(int passiveStones, int moverStones, int identityStones) {
		counts = new ExtraSlotCounts(passiveStones, moverStones, identityStones);
	}
	
	public static int get(ExtraSlotsSkillTreeCompat.SlotGroup group) {
		return switch (group) {
			case PASSIVE -> counts.passiveSlots();
			case MOVER -> counts.moverSlots();
			case IDENTITY -> counts.identitySlots();
		};
	}
}
