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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.skill.Skill;

public class ExtraSlotsMutualExclusionConfigScreen extends Screen {
	private static final int ROW_HEIGHT = 34;
	private static final int GROUP_ROW_HEIGHT = 32;
	private static final int SKILL_ICON_SIZE = 20;
	private static final int PANEL_MARGIN = 20;
	private static final int PANEL_GAP = 28;
	private static final int COMPACT_PANEL_MARGIN = 8;
	private static final int COMPACT_PANEL_GAP = 8;
	private static final int COMPACT_WIDTH = 700;
	private static final int PANEL_TOP = 78;
	private static final int PANEL_BOTTOM_PADDING = 72;
	private static final int HINT_LINE_HEIGHT = 10;
	private static final int MIN_GROUP_PANEL_WIDTH = 210;
	private static final int MIN_SELECTED_PANEL_WIDTH = 250;
	private static final int SEARCH_HEIGHT = 20;
	private static final int DOUBLE_CLICK_MS = 300;
	private static final int PANEL_FILL = 0xA0000000;
	private static final int PANEL_HEADER_FILL = 0x70000000;
	private static final int PANEL_BORDER = 0xFF777777;
	private static final int ROW_HOVER = 0x35FFFFFF;
	private static final int ROW_SELECTED = 0xAA2E7D00;
	private static final int ROW_DISABLED = 0x33000000;

	private final Screen parent;
	private final List<GroupData> groups;
	private final List<SkillOption> allSkills;
	private final Map<ResourceLocation, SkillOption> skillsById;
	private EditBox searchBox;
	private EditBox renameBox;
	private int selectedGroup;
	private int groupScroll;
	private int availableScroll;
	private int selectedScroll;
	private int renamingGroup = -1;
	private int lastClickedGroup = -1;
	private long lastGroupClickTime;
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
		int margin = panelMargin();
		int searchWidth = Mth.clamp(this.width - margin * 2 - 80, 140, 560);
		int searchX = centerX - searchWidth / 2;
		int searchY = 42;

		this.addRenderableWidget(Button.builder(Component.literal("-"), button -> removeSelectedGroup())
			.bounds(searchX - 30, searchY, 24, SEARCH_HEIGHT).build());

		this.searchBox = new EditBox(this.font, searchX, searchY, searchWidth, SEARCH_HEIGHT, Component.translatable(ExtraSlotsConfig.key("mutual_exclusion_search")));
		this.searchBox.setMaxLength(96);
		this.searchBox.setHint(Component.translatable(ExtraSlotsConfig.key("mutual_exclusion_search")));
		this.searchBox.setResponder(ignored -> this.availableScroll = 0);
		this.addRenderableWidget(this.searchBox);

		this.addRenderableWidget(Button.builder(Component.literal("+"), button -> addGroup())
			.bounds(searchX + searchWidth + 6, searchY, 24, SEARCH_HEIGHT).build());

		Layout layout = layout();
		int buttonGap = compactLayout() ? 6 : 10;
		int groupButtonWidth = (layout.groupWidth() - buttonGap) / 2;
		this.addRenderableWidget(Button.builder(Component.translatable(ExtraSlotsConfig.key("new_group")), button -> addGroup())
			.bounds(layout.leftX(), this.height - 28, groupButtonWidth, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable(ExtraSlotsConfig.key("delete_group")), button -> removeSelectedGroup())
			.bounds(layout.leftX() + groupButtonWidth + buttonGap, this.height - 28, groupButtonWidth, 20).build());

		int actionButtonWidth = compactLayout() ? Math.max(76, Math.min(118, (this.width - layout.leftX() - layout.groupWidth() - 28) / 2)) : 150;
		int actionGap = compactLayout() ? 8 : 28;
		int actionX = compactLayout() ? this.width - panelMargin() - actionButtonWidth * 2 - actionGap : centerX - 164;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
			finishRenaming();
			saveGroups();
			this.returnToParent();
		}).bounds(actionX, this.height - 28, actionButtonWidth, 20).build());

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.returnToParent())
			.bounds(actionX + actionButtonWidth + actionGap, this.height - 28, actionButtonWidth, 20).build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderDirtBackground(guiGraphics);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, 0xFFFFFF);

		Layout layout = layout();
		drawPanel(guiGraphics, layout.leftX(), PANEL_TOP, layout.groupWidth(), layout.panelBottom(), Component.translatable(ExtraSlotsConfig.key("groups")));
		drawPanel(guiGraphics, layout.middleX(), PANEL_TOP, layout.middleWidth(), layout.panelBottom(), Component.translatable(ExtraSlotsConfig.key("available_skills")));
		drawPanel(guiGraphics, layout.rightX(), PANEL_TOP, layout.selectedWidth(), layout.panelBottom(), Component.translatable(ExtraSlotsConfig.key("selected_skills")));

		drawGroups(guiGraphics, layout, mouseX, mouseY);
		drawAvailableSkills(guiGraphics, layout, mouseX, mouseY);
		drawSelectedSkills(guiGraphics, layout, mouseX, mouseY);

		if (this.groups.isEmpty()) {
			guiGraphics.drawCenteredString(this.font, Component.translatable(ExtraSlotsConfig.key("no_groups")), layout.leftX() + layout.groupWidth() / 2, PANEL_TOP + 52, 0xA0A0A0);
		}

		drawHint(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.renameBox != null && this.renameBox.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		if (this.renameBox != null && !isInside(mouseX, mouseY, this.renameBox.getX(), this.renameBox.getY(), this.renameBox.getWidth(), this.renameBox.getY() + this.renameBox.getHeight())) {
			finishRenaming();
		}

		if (button == 0) {
			if (clickGroup(mouseX, mouseY) || clickAvailableSkill(mouseX, mouseY) || clickSelectedSkill(mouseX, mouseY)) {
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		Layout layout = layout();
		if (isInside(mouseX, mouseY, layout.leftX(), PANEL_TOP, layout.groupWidth(), layout.panelBottom())) {
			finishRenaming();
			this.groupScroll = scroll(this.groupScroll, this.groups.size(), visibleGroupRows(), delta);
			return true;
		}

		if (isInside(mouseX, mouseY, layout.middleX(), PANEL_TOP, layout.middleWidth(), layout.panelBottom())) {
			this.availableScroll = scroll(this.availableScroll, filteredSkills().size(), visibleSkillRows(), delta);
			return true;
		}

		if (isInside(mouseX, mouseY, layout.rightX(), PANEL_TOP, layout.selectedWidth(), layout.panelBottom())) {
			this.selectedScroll = scroll(this.selectedScroll, selectedGroup().skills().size(), visibleSkillRows(), delta);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.renameBox != null) {
			if (keyCode == 257 || keyCode == 335) {
				finishRenaming();
				return true;
			}

			if (keyCode == 256) {
				finishRenaming();
				return true;
			}
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		this.returnToParent();
	}

	private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int bottom, Component title) {
		guiGraphics.fill(x, y, x + width, bottom, PANEL_FILL);
		guiGraphics.fill(x, y, x + width, y + 28, PANEL_HEADER_FILL);
		guiGraphics.fill(x, y, x + width, y + 1, PANEL_BORDER);
		guiGraphics.fill(x, bottom - 1, x + width, bottom, PANEL_BORDER);
		guiGraphics.fill(x, y, x + 1, bottom, PANEL_BORDER);
		guiGraphics.fill(x + width - 1, y, x + width, bottom, PANEL_BORDER);
		guiGraphics.drawString(this.font, trim(title.getString(), width - 24), x + 12, y + 10, 0xFFFFFF);
	}

	private void drawGroups(GuiGraphics guiGraphics, Layout layout, int mouseX, int mouseY) {
		int x = layout.leftX();
		int y = PANEL_TOP + 34;
		int rows = visibleGroupRows();
		for (int i = 0; i < rows && i + this.groupScroll < this.groups.size(); i++) {
			int groupIndex = i + this.groupScroll;
			GroupData group = this.groups.get(groupIndex);
			int rowY = y + i * GROUP_ROW_HEIGHT;
			if (groupIndex == this.selectedGroup) {
				guiGraphics.fill(x + 8, rowY, x + layout.groupWidth() - 8, rowY + GROUP_ROW_HEIGHT - 2, ROW_SELECTED);
			} else if (isInside(mouseX, mouseY, x + 8, rowY, layout.groupWidth() - 16, rowY + GROUP_ROW_HEIGHT - 2)) {
				guiGraphics.fill(x + 8, rowY, x + layout.groupWidth() - 8, rowY + GROUP_ROW_HEIGHT - 2, ROW_HOVER);
			}

			if (groupIndex == this.renamingGroup && this.renameBox != null) {
				continue;
			}

			String skillCount = Component.translatable(ExtraSlotsConfig.key("group_skill_count"), group.skills().size()).getString();
			String label = Component.translatable(ExtraSlotsConfig.key("group_entry"), group.displayName(groupIndex), skillCount).getString();
			guiGraphics.drawString(this.font, trim(label, layout.groupWidth() - 24), x + 18, rowY + 11, 0xFFFFFF);
		}
	}

	private void drawAvailableSkills(GuiGraphics guiGraphics, Layout layout, int mouseX, int mouseY) {
		List<SkillOption> skills = filteredSkills();
		int x = layout.middleX();
		int y = PANEL_TOP + 34;
		int rows = visibleSkillRows();
		Set<ResourceLocation> selected = Set.copyOf(selectedGroup().skills());

		for (int i = 0; i < rows && i + this.availableScroll < skills.size(); i++) {
			SkillOption option = skills.get(i + this.availableScroll);
			int rowY = y + i * ROW_HEIGHT;
			boolean alreadySelected = selected.contains(option.id());
			if (alreadySelected) {
				guiGraphics.fill(x + 8, rowY, x + layout.middleWidth() - 8, rowY + ROW_HEIGHT - 2, ROW_DISABLED);
			} else if (isInside(mouseX, mouseY, x + 8, rowY, layout.middleWidth() - 16, rowY + ROW_HEIGHT - 2)) {
				guiGraphics.fill(x + 8, rowY, x + layout.middleWidth() - 8, rowY + ROW_HEIGHT - 2, ROW_HOVER);
			}

			drawSkillIcon(guiGraphics, option, x + 12, rowY + 7);
			int color = alreadySelected ? 0x8A8A8A : 0xFFFFFF;
			int textX = x + 42;
			guiGraphics.drawString(this.font, trim(option.displayName().getString(), layout.middleWidth() - 54), textX, rowY + 5, color);
			guiGraphics.drawString(this.font, trim(option.id().toString(), layout.middleWidth() - 54), textX, rowY + 17, 0xA0A0A0);
		}

		if (skills.isEmpty()) {
			guiGraphics.drawCenteredString(this.font, Component.translatable(ExtraSlotsConfig.key("no_matching_skills")), x + layout.middleWidth() / 2, y + 30, 0xA0A0A0);
		}
	}

	private void drawSelectedSkills(GuiGraphics guiGraphics, Layout layout, int mouseX, int mouseY) {
		List<ResourceLocation> selected = selectedGroup().skills();
		int x = layout.rightX();
		int y = PANEL_TOP + 34;
		int rows = visibleSkillRows();

		for (int i = 0; i < rows && i + this.selectedScroll < selected.size(); i++) {
			ResourceLocation id = selected.get(i + this.selectedScroll);
			SkillOption option = optionById(id);
			int rowY = y + i * ROW_HEIGHT;
			if (isInside(mouseX, mouseY, x + 8, rowY, layout.selectedWidth() - 16, rowY + ROW_HEIGHT - 2)) {
				guiGraphics.fill(x + 8, rowY, x + layout.selectedWidth() - 8, rowY + ROW_HEIGHT - 2, ROW_HOVER);
			}

			String name = option == null ? id.toString() : option.displayName().getString();
			int textX = x + 14;
			if (option != null) {
				drawSkillIcon(guiGraphics, option, x + 12, rowY + 7);
				textX = x + 42;
			}

			guiGraphics.drawString(this.font, trim(name, layout.selectedWidth() - (textX - x) - 12), textX, rowY + 5, 0xFFFFFF);
			guiGraphics.drawString(this.font, trim(id.toString(), layout.selectedWidth() - (textX - x) - 12), textX, rowY + 17, 0xA0A0A0);
		}

		if (this.selectedGroup < 0) {
			guiGraphics.drawCenteredString(this.font, Component.translatable(ExtraSlotsConfig.key("select_or_create_group")), x + layout.selectedWidth() / 2, y + 30, 0xA0A0A0);
		} else if (selected.isEmpty()) {
			guiGraphics.drawCenteredString(this.font, Component.translatable(ExtraSlotsConfig.key("empty_group")), x + layout.selectedWidth() / 2, y + 30, 0xA0A0A0);
		}
	}

	private boolean clickGroup(double mouseX, double mouseY) {
		Layout layout = layout();
		int y = PANEL_TOP + 34;
		if (!isInside(mouseX, mouseY, layout.leftX(), y, layout.groupWidth(), layout.panelBottom())) {
			return false;
		}

		int index = this.groupScroll + ((int)mouseY - y) / GROUP_ROW_HEIGHT;
		if (index >= 0 && index < this.groups.size()) {
			long now = System.currentTimeMillis();
			if (index == this.lastClickedGroup && now - this.lastGroupClickTime <= DOUBLE_CLICK_MS) {
				this.selectedGroup = index;
				this.selectedScroll = 0;
				beginRenaming(index);
			} else {
				this.selectedGroup = index;
				this.selectedScroll = 0;
			}

			this.lastClickedGroup = index;
			this.lastGroupClickTime = now;
			return true;
		}

		return false;
	}

	private boolean clickAvailableSkill(double mouseX, double mouseY) {
		Layout layout = layout();
		int y = PANEL_TOP + 34;
		if (!isInside(mouseX, mouseY, layout.middleX(), y, layout.middleWidth(), layout.panelBottom())) {
			return false;
		}

		List<SkillOption> skills = filteredSkills();
		int index = this.availableScroll + ((int)mouseY - y) / ROW_HEIGHT;
		if (index >= 0 && index < skills.size()) {
			GroupData group = ensureSelectedGroup();
			ResourceLocation id = skills.get(index).id();
			if (!group.skills().contains(id)) {
				group.skills().add(id);
			}
			this.selectedScroll = clampScroll(this.selectedScroll, group.skills().size(), visibleSkillRows());
			return true;
		}

		return false;
	}

	private boolean clickSelectedSkill(double mouseX, double mouseY) {
		Layout layout = layout();
		int y = PANEL_TOP + 34;
		if (!isInside(mouseX, mouseY, layout.rightX(), y, layout.selectedWidth(), layout.panelBottom())) {
			return false;
		}

		List<ResourceLocation> selected = selectedGroup().skills();
		int index = this.selectedScroll + ((int)mouseY - y) / ROW_HEIGHT;
		if (index >= 0 && index < selected.size()) {
			selected.remove(index);
			this.selectedScroll = clampScroll(this.selectedScroll, selected.size(), visibleSkillRows());
			return true;
		}

		return false;
	}

	private void addGroup() {
		finishRenaming();
		this.groups.add(new GroupData(defaultGroupName(this.groups.size()), new ArrayList<>()));
		this.selectedGroup = this.groups.size() - 1;
		this.groupScroll = Math.max(0, this.groups.size() - visibleGroupRows());
		this.selectedScroll = 0;
	}

	private void removeSelectedGroup() {
		finishRenaming();
		if (this.selectedGroup >= 0 && this.selectedGroup < this.groups.size()) {
			this.groups.remove(this.selectedGroup);
			this.selectedGroup = this.groups.isEmpty() ? -1 : Mth.clamp(this.selectedGroup, 0, this.groups.size() - 1);
			this.groupScroll = clampScroll(this.groupScroll, this.groups.size(), visibleGroupRows());
			this.selectedScroll = 0;
		}
	}

	private void beginRenaming(int groupIndex) {
		finishRenaming();
		if (groupIndex < 0 || groupIndex >= this.groups.size()) {
			return;
		}

		Layout layout = layout();
		int rowY = PANEL_TOP + 34 + (groupIndex - this.groupScroll) * GROUP_ROW_HEIGHT;
		if (rowY < PANEL_TOP + 34 || rowY + GROUP_ROW_HEIGHT > layout.panelBottom()) {
			return;
		}

		GroupData group = this.groups.get(groupIndex);
		this.renamingGroup = groupIndex;
		this.renameBox = new EditBox(this.font, layout.leftX() + 12, rowY + 6, layout.groupWidth() - 24, 20, Component.literal(group.displayName(groupIndex)));
		this.renameBox.setMaxLength(48);
		this.renameBox.setValue(group.displayName(groupIndex));
		this.renameBox.setResponder(value -> group.setName(sanitizeGroupName(value)));
		this.renameBox.setFocused(true);
		this.renameBox.setHighlightPos(0);
		this.addRenderableWidget(this.renameBox);
		this.setFocused(this.renameBox);
	}

	private void finishRenaming() {
		if (this.renameBox != null) {
			if (this.renamingGroup >= 0 && this.renamingGroup < this.groups.size()) {
				GroupData group = this.groups.get(this.renamingGroup);
				if (group.name().isBlank()) {
					group.setName(defaultGroupName(this.renamingGroup));
				}
			}

			this.removeWidget(this.renameBox);
			this.renameBox = null;
			this.renamingGroup = -1;
		}
	}

	private GroupData ensureSelectedGroup() {
		if (this.selectedGroup < 0 || this.selectedGroup >= this.groups.size()) {
			this.groups.add(new GroupData(defaultGroupName(this.groups.size()), new ArrayList<>()));
			this.selectedGroup = this.groups.size() - 1;
		}

		return this.groups.get(this.selectedGroup);
	}

	private GroupData selectedGroup() {
		if (this.selectedGroup < 0 || this.selectedGroup >= this.groups.size()) {
			return GroupData.EMPTY;
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
			.map(this::encodeGroup)
			.toList();

		ExtraSlotsConfig.MUTUAL_EXCLUSION_GROUPS.set(savedGroups);
		ExtraSlotsConfig.SPEC.save();
	}

	private String encodeGroup(GroupData group) {
		List<String> uniqueSkillIds = group.skills().stream()
			.distinct()
			.map(ResourceLocation::toString)
			.toList();
		return sanitizeGroupName(group.name()) + "|" + String.join(",", uniqueSkillIds);
	}

	private int scroll(int current, int size, int visibleRows, double delta) {
		return clampScroll(current + (delta < 0.0D ? 1 : -1), size, visibleRows);
	}

	private int clampScroll(int current, int size, int visibleRows) {
		return Mth.clamp(current, 0, Math.max(0, size - visibleRows));
	}

	private int visibleGroupRows() {
		return Math.max(1, (panelBottom() - (PANEL_TOP + 34) - 2) / GROUP_ROW_HEIGHT);
	}

	private int visibleSkillRows() {
		return Math.max(1, (panelBottom() - (PANEL_TOP + 34) - 2) / ROW_HEIGHT);
	}

	private int panelBottom() {
		return Math.max(PANEL_TOP + 104, this.height - PANEL_BOTTOM_PADDING);
	}

	private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int bottom) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < bottom;
	}

	private String trim(String text, int width) {
		return this.font.plainSubstrByWidth(text, Math.max(8, width));
	}

	private void drawHint(GuiGraphics guiGraphics) {
		int hintX = panelMargin();
		int hintWidth = Math.max(80, this.width - hintX * 2);
		List<FormattedCharSequence> lines = this.font.split(Component.translatable(ExtraSlotsConfig.key("mutual_exclusion_hint")), hintWidth);
		int hintY = this.height - 30 - lines.size() * HINT_LINE_HEIGHT - 8;
		for (int i = 0; i < lines.size(); i++) {
			FormattedCharSequence line = lines.get(i);
			guiGraphics.drawString(this.font, line, this.width / 2 - this.font.width(line) / 2, hintY + i * HINT_LINE_HEIGHT, 0xB8B8B8);
		}
	}

	private void drawSkillIcon(GuiGraphics guiGraphics, SkillOption option, int x, int y) {
		guiGraphics.blit(option.icon(), x, y, SKILL_ICON_SIZE, SKILL_ICON_SIZE, 0.0F, 0.0F, 32, 32, 32, 32);
	}

	private Layout layout() {
		int panelBottom = panelBottom();
		int margin = panelMargin();
		int gap = panelGap();
		int contentWidth = Math.max(240, this.width - margin * 2 - gap * 2);
		int groupWidth;
		int middleWidth;
		int selectedWidth;
		if (compactLayout()) {
			groupWidth = Mth.clamp(contentWidth / 3, 96, 170);
			int minimumSkillPanelWidth = 88;
			if (contentWidth - groupWidth < minimumSkillPanelWidth * 2) {
				groupWidth = Math.max(80, contentWidth - minimumSkillPanelWidth * 2);
			}

			int remainingWidth = contentWidth - groupWidth;
			middleWidth = remainingWidth / 2;
			selectedWidth = remainingWidth - middleWidth;
		} else {
			groupWidth = Mth.clamp(this.width / 5, MIN_GROUP_PANEL_WIDTH, 280);
			selectedWidth = Mth.clamp(this.width / 4, MIN_SELECTED_PANEL_WIDTH, 360);
			middleWidth = contentWidth - groupWidth - selectedWidth;
			if (middleWidth < 220) {
				groupWidth = Mth.clamp(this.width / 5, 170, 220);
				selectedWidth = Mth.clamp(this.width / 4, 200, 280);
				middleWidth = contentWidth - groupWidth - selectedWidth;
			}
		}

		middleWidth = Math.max(72, middleWidth);
		selectedWidth = Math.max(72, selectedWidth);
		int leftX = margin;
		int middleX = leftX + groupWidth + gap;
		int rightX = middleX + middleWidth + gap;
		return new Layout(leftX, middleX, rightX, groupWidth, middleWidth, selectedWidth, panelBottom);
	}

	private boolean compactLayout() {
		return this.width < COMPACT_WIDTH;
	}

	private int panelMargin() {
		return compactLayout() ? COMPACT_PANEL_MARGIN : PANEL_MARGIN;
	}

	private int panelGap() {
		return compactLayout() ? COMPACT_PANEL_GAP : PANEL_GAP;
	}

	private void returnToParent() {
		this.minecraft.setScreen(this.parent == this ? null : this.parent);
	}

	private static List<GroupData> loadGroups() {
		List<GroupData> loadedGroups = new ArrayList<>();
		List<? extends String> rawGroups = ExtraSlotsConfig.MUTUAL_EXCLUSION_GROUPS.get();
		for (int i = 0; i < rawGroups.size(); i++) {
			String rawGroup = rawGroups.get(i);
			String name = defaultGroupName(i);
			String skillsText = rawGroup;
			int separator = rawGroup.indexOf('|');
			if (separator >= 0) {
				name = sanitizeGroupName(rawGroup.substring(0, separator));
				skillsText = rawGroup.substring(separator + 1);
			}

			List<ResourceLocation> groupSkills = new ArrayList<>();
			for (String rawId : skillsText.split(",")) {
				ResourceLocation id = ResourceLocation.tryParse(rawId.trim());
				if (id != null && !groupSkills.contains(id)) {
					groupSkills.add(id);
				}
			}

			if (!name.isBlank() || !groupSkills.isEmpty()) {
				loadedGroups.add(new GroupData(name.isBlank() ? defaultGroupName(i) : name, groupSkills));
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

	private static String defaultGroupName(int index) {
		return "Group " + (index + 1);
	}

	private static String sanitizeGroupName(String name) {
		return name.replace('|', ' ').replace(',', ' ').trim();
	}

	private record Layout(int leftX, int middleX, int rightX, int groupWidth, int middleWidth, int selectedWidth, int panelBottom) {
	}

	private static final class GroupData {
		private static final GroupData EMPTY = new GroupData("", List.of());

		private String name;
		private final List<ResourceLocation> skills;

		private GroupData(String name, List<ResourceLocation> skills) {
			this.name = name;
			this.skills = skills;
		}

		private String name() {
			return this.name;
		}

		private void setName(String name) {
			this.name = name;
		}

		private List<ResourceLocation> skills() {
			return this.skills;
		}

		private String displayName(int index) {
			return this.name.isBlank() ? defaultGroupName(index) : this.name;
		}
	}

	private record SkillOption(ResourceLocation id, Component displayName, ResourceLocation icon) {
		private String searchText() {
			return (this.displayName.getString() + " " + this.id).toLowerCase(Locale.ROOT);
		}
	}
}
