package dev.spake404.epicfight.extraslots;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.skill.Skill;

public class ExtraSlotsMutualExclusionConfigScreen extends Screen {
	private static final int ROW_HEIGHT = 24;
	private static final int SKILL_ICON_SIZE = 16;
	private static final int PANEL_GAP = 8;
	private static final int PANEL_TOP = 58;
	private static final int PANEL_BOTTOM_PADDING = 52;
	private static final int GROUP_PANEL_WIDTH = 136;
	private static final int SELECTED_PANEL_WIDTH = 188;
	
	private final Screen parent;
	private final List<List<ResourceLocation>> groups;
	private final List<SkillOption> allSkills;
	private final Map<ResourceLocation, SkillOption> skillsById;
	private EditBox searchBox;
	private int selectedGroup;
	private int groupScroll;
	private int availableScroll;
	private int selectedScroll;
	private String cachedSearchQuery;
	private List<SkillOption> cachedFilteredSkills = List.of();
	
	public ExtraSlotsMutualExclusionConfigScreen(Screen parent) {
		super(Component.translatable(ExtraSlotsConfig.key("mutual_exclusions")));
		this.parent = parent;
		this.groups = loadGroups();
		this.allSkills = loadSkills();
		this.skillsById = this.allSkills.stream().collect(Collectors.toUnmodifiableMap(SkillOption::id, option -> option));
		this.selectedGroup = this.groups.isEmpty() ? -1 : 0;
	}
	
	@Override
	protected void init() {
		int centerX = this.width / 2;
		
		this.searchBox = new EditBox(this.font, centerX - 150, 31, 300, 20, Component.translatable(ExtraSlotsConfig.key("mutual_exclusion_search")));
		this.searchBox.setMaxLength(96);
		this.searchBox.setHint(Component.translatable(ExtraSlotsConfig.key("mutual_exclusion_search")));
		this.searchBox.setResponder(ignored -> this.availableScroll = 0);
		this.addRenderableWidget(this.searchBox);
		
		this.addRenderableWidget(Button.builder(Component.translatable(ExtraSlotsConfig.key("new_group")), button -> {
			this.groups.add(new ArrayList<>());
			this.selectedGroup = this.groups.size() - 1;
			this.groupScroll = Math.max(0, this.groups.size() - visibleRows() + 1);
			this.selectedScroll = 0;
		}).bounds(12, this.height - 42, 92, 20).build());
		
		this.addRenderableWidget(Button.builder(Component.translatable(ExtraSlotsConfig.key("delete_group")), button -> {
			if (this.selectedGroup >= 0 && this.selectedGroup < this.groups.size()) {
				this.groups.remove(this.selectedGroup);
				this.selectedGroup = this.groups.isEmpty() ? -1 : Mth.clamp(this.selectedGroup, 0, this.groups.size() - 1);
				this.groupScroll = clampScroll(this.groupScroll, this.groups.size());
				this.selectedScroll = 0;
			}
		}).bounds(108, this.height - 42, 92, 20).build());
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.returnToParent())
			.bounds(centerX + 8, this.height - 30, 120, 20).build());
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
			saveGroups();
			this.returnToParent();
		}).bounds(centerX - 128, this.height - 30, 120, 20).build());
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderDirtBackground(guiGraphics);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 16777215);
		
		int leftX = 12;
		int middleX = leftX + GROUP_PANEL_WIDTH + PANEL_GAP;
		int rightX = this.width - SELECTED_PANEL_WIDTH - 12;
		int middleW = Math.max(120, rightX - middleX - PANEL_GAP);
		int panelBottom = panelBottom();
		
		drawPanel(guiGraphics, leftX, PANEL_TOP, GROUP_PANEL_WIDTH, panelBottom, Component.translatable(ExtraSlotsConfig.key("groups")));
		drawPanel(guiGraphics, middleX, PANEL_TOP, middleW, panelBottom, Component.translatable(ExtraSlotsConfig.key("available_skills")));
		drawPanel(guiGraphics, rightX, PANEL_TOP, SELECTED_PANEL_WIDTH, panelBottom, Component.translatable(ExtraSlotsConfig.key("selected_skills")));
		
		drawGroups(guiGraphics, leftX, PANEL_TOP + 18, GROUP_PANEL_WIDTH, panelBottom);
		drawAvailableSkills(guiGraphics, middleX, PANEL_TOP + 18, middleW, panelBottom);
		drawSelectedSkills(guiGraphics, rightX, PANEL_TOP + 18, SELECTED_PANEL_WIDTH, panelBottom);
		
		if (this.groups.isEmpty()) {
			guiGraphics.drawCenteredString(this.font, Component.translatable(ExtraSlotsConfig.key("no_groups")), leftX + GROUP_PANEL_WIDTH / 2, PANEL_TOP + 42, 10526880);
		}
		
		guiGraphics.drawCenteredString(this.font, Component.translatable(ExtraSlotsConfig.key("mutual_exclusion_hint")), this.width / 2, this.height - 50, 10526880);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			if (clickGroup(mouseX, mouseY) || clickAvailableSkill(mouseX, mouseY) || clickSelectedSkill(mouseX, mouseY)) {
				return true;
			}
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int leftX = 12;
		int middleX = leftX + GROUP_PANEL_WIDTH + PANEL_GAP;
		int rightX = this.width - SELECTED_PANEL_WIDTH - 12;
		int middleW = Math.max(120, rightX - middleX - PANEL_GAP);
		
		if (isInside(mouseX, mouseY, leftX, PANEL_TOP, GROUP_PANEL_WIDTH, panelBottom())) {
			this.groupScroll = scroll(this.groupScroll, this.groups.size(), delta);
			return true;
		}
		
		if (isInside(mouseX, mouseY, middleX, PANEL_TOP, middleW, panelBottom())) {
			this.availableScroll = scroll(this.availableScroll, filteredSkills().size(), delta);
			return true;
		}
		
		if (isInside(mouseX, mouseY, rightX, PANEL_TOP, SELECTED_PANEL_WIDTH, panelBottom())) {
			this.selectedScroll = scroll(this.selectedScroll, selectedGroup().size(), delta);
			return true;
		}
		
		return super.mouseScrolled(mouseX, mouseY, delta);
	}
	
	@Override
	public void onClose() {
		this.returnToParent();
	}
	
	private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int bottom, Component title) {
		guiGraphics.fill(x, y, x + width, bottom, -1879048192);
		guiGraphics.fill(x, y, x + width, y + 17, -15658735);
		guiGraphics.drawString(this.font, title, x + 6, y + 5, 16777215);
	}
	
	private void drawGroups(GuiGraphics guiGraphics, int x, int y, int width, int bottom) {
		int rows = rowsBetween(y, bottom);
		for (int i = 0; i < rows && i + this.groupScroll < this.groups.size(); i++) {
			int groupIndex = i + this.groupScroll;
			int rowY = y + i * ROW_HEIGHT;
			if (groupIndex == this.selectedGroup) {
				guiGraphics.fill(x + 2, rowY, x + width - 2, rowY + ROW_HEIGHT - 1, -1070563328);
			}
			
			Component label = Component.translatable(ExtraSlotsConfig.key("group_entry"), groupIndex + 1, this.groups.get(groupIndex).size());
			guiGraphics.drawString(this.font, trim(label.getString(), width - 10), x + 6, rowY + 5, 16777215);
		}
	}
	
	private void drawAvailableSkills(GuiGraphics guiGraphics, int x, int y, int width, int bottom) {
		List<SkillOption> skills = filteredSkills();
		int rows = rowsBetween(y, bottom);
		Set<ResourceLocation> selected = Set.copyOf(selectedGroup());
		
		for (int i = 0; i < rows && i + this.availableScroll < skills.size(); i++) {
			SkillOption option = skills.get(i + this.availableScroll);
			int rowY = y + i * ROW_HEIGHT;
			boolean alreadySelected = selected.contains(option.id());
			int color = alreadySelected ? 8421504 : 16777215;
			
			if (alreadySelected) {
				guiGraphics.fill(x + 2, rowY, x + width - 2, rowY + ROW_HEIGHT - 1, 855638016);
			}
			
			drawSkillIcon(guiGraphics, option, x + 6, rowY + 4);
			int textX = x + 28;
			guiGraphics.drawString(this.font, trim(option.displayName().getString(), width - 34), textX, rowY + 3, color);
			guiGraphics.drawString(this.font, trim(option.id().toString(), width - 34), textX, rowY + 13, 10526880);
		}
		
		if (skills.isEmpty()) {
			guiGraphics.drawCenteredString(this.font, Component.translatable(ExtraSlotsConfig.key("no_matching_skills")), x + width / 2, y + 24, 10526880);
		}
	}
	
	private void drawSelectedSkills(GuiGraphics guiGraphics, int x, int y, int width, int bottom) {
		List<ResourceLocation> selected = selectedGroup();
		int rows = rowsBetween(y, bottom);
		
		for (int i = 0; i < rows && i + this.selectedScroll < selected.size(); i++) {
			ResourceLocation id = selected.get(i + this.selectedScroll);
			SkillOption option = optionById(id);
			int rowY = y + i * ROW_HEIGHT;
			String name = option == null ? id.toString() : option.displayName().getString();
			int textX = x + 6;
			
			if (option != null) {
				drawSkillIcon(guiGraphics, option, x + 6, rowY + 4);
				textX = x + 28;
			}

			guiGraphics.drawString(this.font, trim(name, width - (textX - x) - 6), textX, rowY + 3, 16777215);
			guiGraphics.drawString(this.font, trim(id.toString(), width - (textX - x) - 6), textX, rowY + 13, 10526880);
		}
		
		if (this.selectedGroup < 0) {
			guiGraphics.drawCenteredString(this.font, Component.translatable(ExtraSlotsConfig.key("select_or_create_group")), x + width / 2, y + 24, 10526880);
		} else if (selected.isEmpty()) {
			guiGraphics.drawCenteredString(this.font, Component.translatable(ExtraSlotsConfig.key("empty_group")), x + width / 2, y + 24, 10526880);
		}
	}
	
	private boolean clickGroup(double mouseX, double mouseY) {
		int x = 12;
		int y = PANEL_TOP + 18;
		if (!isInside(mouseX, mouseY, x, y, GROUP_PANEL_WIDTH, panelBottom())) {
			return false;
		}
		
		int index = this.groupScroll + ((int)mouseY - y) / ROW_HEIGHT;
		if (index >= 0 && index < this.groups.size()) {
			this.selectedGroup = index;
			this.selectedScroll = 0;
			return true;
		}
		
		return false;
	}
	
	private boolean clickAvailableSkill(double mouseX, double mouseY) {
		int leftX = 12;
		int middleX = leftX + GROUP_PANEL_WIDTH + PANEL_GAP;
		int rightX = this.width - SELECTED_PANEL_WIDTH - 12;
		int middleW = Math.max(120, rightX - middleX - PANEL_GAP);
		int y = PANEL_TOP + 18;
		if (!isInside(mouseX, mouseY, middleX, y, middleW, panelBottom())) {
			return false;
		}
		
		List<SkillOption> skills = filteredSkills();
		int index = this.availableScroll + ((int)mouseY - y) / ROW_HEIGHT;
		if (index >= 0 && index < skills.size()) {
			List<ResourceLocation> group = ensureSelectedGroup();
			ResourceLocation id = skills.get(index).id();
			if (!group.contains(id)) {
				group.add(id);
			}
			this.selectedScroll = clampScroll(this.selectedScroll, group.size());
			return true;
		}
		
		return false;
	}
	
	private boolean clickSelectedSkill(double mouseX, double mouseY) {
		int x = this.width - SELECTED_PANEL_WIDTH - 12;
		int y = PANEL_TOP + 18;
		if (!isInside(mouseX, mouseY, x, y, SELECTED_PANEL_WIDTH, panelBottom())) {
			return false;
		}
		
		List<ResourceLocation> selected = selectedGroup();
		int index = this.selectedScroll + ((int)mouseY - y) / ROW_HEIGHT;
		if (index >= 0 && index < selected.size()) {
			selected.remove(index);
			this.selectedScroll = clampScroll(this.selectedScroll, selected.size());
			return true;
		}
		
		return false;
	}
	
	private List<ResourceLocation> ensureSelectedGroup() {
		if (this.selectedGroup < 0 || this.selectedGroup >= this.groups.size()) {
			this.groups.add(new ArrayList<>());
			this.selectedGroup = this.groups.size() - 1;
		}
		
		return this.groups.get(this.selectedGroup);
	}
	
	private List<ResourceLocation> selectedGroup() {
		if (this.selectedGroup < 0 || this.selectedGroup >= this.groups.size()) {
			return List.of();
		}
		
		return this.groups.get(this.selectedGroup);
	}
	
	private List<SkillOption> filteredSkills() {
		String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
		if (query.equals(this.cachedSearchQuery)) {
			return this.cachedFilteredSkills;
		}

		this.cachedSearchQuery = query;
		if (query.isEmpty()) {
			this.cachedFilteredSkills = this.allSkills;
			return this.cachedFilteredSkills;
		}
		
		this.cachedFilteredSkills = this.allSkills.stream()
			.filter(option -> option.searchText().contains(query))
			.toList();
		return this.cachedFilteredSkills;
	}
	
	private SkillOption optionById(ResourceLocation id) {
		return this.skillsById.get(id);
	}
	
	private void saveGroups() {
		List<String> savedGroups = this.groups.stream()
			.map(group -> group.stream().distinct().map(ResourceLocation::toString).toList())
			.filter(group -> !group.isEmpty())
			.map(group -> String.join(",", group))
			.toList();
		
		ExtraSlotsConfig.MUTUAL_EXCLUSION_GROUPS.set(savedGroups);
		ExtraSlotsConfig.SPEC.save();
	}
	
	private int scroll(int current, int size, double delta) {
		return clampScroll(current + (delta < 0.0D ? 1 : -1), size);
	}
	
	private int clampScroll(int current, int size) {
		return Mth.clamp(current, 0, Math.max(0, size - visibleRows()));
	}
	
	private int visibleRows() {
		return Math.max(1, rowsBetween(PANEL_TOP + 18, panelBottom()));
	}
	
	private int rowsBetween(int y, int bottom) {
		return Math.max(1, (bottom - y - 2) / ROW_HEIGHT);
	}
	
	private int panelBottom() {
		return Math.max(PANEL_TOP + 72, this.height - PANEL_BOTTOM_PADDING);
	}
	
	private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int bottom) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < bottom;
	}
	
	private String trim(String text, int width) {
		return this.font.plainSubstrByWidth(text, Math.max(8, width));
	}

	private void drawSkillIcon(GuiGraphics guiGraphics, SkillOption option, int x, int y) {
		guiGraphics.blit(option.icon(), x, y, SKILL_ICON_SIZE, SKILL_ICON_SIZE, 0.0F, 0.0F, 32, 32, 32, 32);
	}
	
	private void returnToParent() {
		this.minecraft.setScreen(this.parent == this ? null : this.parent);
	}
	
	private static List<List<ResourceLocation>> loadGroups() {
		List<List<ResourceLocation>> loadedGroups = new ArrayList<>();
		for (String rawGroup : ExtraSlotsConfig.MUTUAL_EXCLUSION_GROUPS.get()) {
			List<ResourceLocation> group = new ArrayList<>();
			for (String rawId : rawGroup.split(",")) {
				ResourceLocation id = ResourceLocation.tryParse(rawId.trim());
				if (id != null && !group.contains(id)) {
					group.add(id);
				}
			}
			
			if (!group.isEmpty()) {
				loadedGroups.add(group);
			}
		}
		
		return loadedGroups;
	}
	
	private static List<SkillOption> loadSkills() {
		Set<ResourceLocation> seen = new LinkedHashSet<>();
		return SkillManager.getSkills(skill -> skill != null && !(skill instanceof ExtraSlotUnlockSkill) && skill.getRegistryName() != null)
			.stream()
			.filter(skill -> skill.getCategory() != null && skill.getCategory().learnable())
			.filter(skill -> seen.add(skill.getRegistryName()))
			.map(skill -> new SkillOption(skill.getRegistryName(), skill.getDisplayName(), skill.getSkillTexture()))
			.sorted(Comparator.comparing(option -> option.displayName().getString().toLowerCase(Locale.ROOT) + option.id()))
			.toList();
	}
	
	private record SkillOption(ResourceLocation id, Component displayName, ResourceLocation icon) {
		private String searchText() {
			return (this.displayName.getString() + " " + this.id).toLowerCase(Locale.ROOT);
		}
	}
}
