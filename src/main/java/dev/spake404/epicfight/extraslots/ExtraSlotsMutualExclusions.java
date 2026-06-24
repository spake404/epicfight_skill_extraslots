package dev.spake404.epicfight.extraslots;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

public final class ExtraSlotsMutualExclusions {
	private static volatile CompiledGroups cachedGroups = new CompiledGroups(List.of(), Map.of());

	private ExtraSlotsMutualExclusions() {
	}

	public static Optional<Conflict> findConflict(CapabilitySkill skills, Skill incoming, SkillContainer ignoredContainer) {
		ResourceLocation incomingId = idOf(incoming);
		if (skills == null || incomingId == null) {
			return Optional.empty();
		}

		Set<ResourceLocation> group = groupsBySkill().get(incomingId);
		if (group == null) {
			return Optional.empty();
		}

		try (Stream<SkillContainer> containers = skills.listSkillContainers()) {
			Iterator<SkillContainer> iterator = containers.iterator();
			while (iterator.hasNext()) {
				SkillContainer container = iterator.next();
				if (container == null || container.isEmpty()) {
					continue;
				}

				Skill skill = container.getSkill();
				ResourceLocation skillId = idOf(skill);
				if (skillId != null && !incomingId.equals(skillId) && group.contains(skillId)) {
					return Optional.of(new Conflict(container, skill));
				}
			}
		}

		return Optional.empty();
	}

	public static boolean conflicts(Skill first, Skill second) {
		return conflicts(idOf(first), idOf(second));
	}

	private static boolean conflicts(ResourceLocation first, ResourceLocation second) {
		if (first == null || second == null || first.equals(second)) {
			return false;
		}

		Set<ResourceLocation> group = groupsBySkill().get(first);
		return group != null && group.contains(second);
	}

	private static Map<ResourceLocation, Set<ResourceLocation>> groupsBySkill() {
		List<String> rawGroups = ExtraSlotsConfig.MUTUAL_EXCLUSION_GROUPS.get().stream()
			.map(String::trim)
			.toList();
		CompiledGroups current = cachedGroups;
		if (rawGroups.equals(current.rawGroups())) {
			return current.groupsBySkill();
		}

		synchronized (ExtraSlotsMutualExclusions.class) {
			current = cachedGroups;
			if (rawGroups.equals(current.rawGroups())) {
				return current.groupsBySkill();
			}

			Map<ResourceLocation, Set<ResourceLocation>> mutableGroupsBySkill = new HashMap<>();
			for (String rawGroup : rawGroups) {
				Set<ResourceLocation> group = parseGroup(rawGroup);
				if (group.size() <= 1) {
					continue;
				}

				for (ResourceLocation skill : group) {
					mutableGroupsBySkill.computeIfAbsent(skill, ignored -> new HashSet<>()).addAll(group);
				}
			}

			Map<ResourceLocation, Set<ResourceLocation>> immutableGroupsBySkill = new HashMap<>();
			for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : mutableGroupsBySkill.entrySet()) {
				immutableGroupsBySkill.put(entry.getKey(), Set.copyOf(entry.getValue()));
			}

			cachedGroups = new CompiledGroups(List.copyOf(rawGroups), Map.copyOf(immutableGroupsBySkill));
			return cachedGroups.groupsBySkill();
		}
	}

	private static Set<ResourceLocation> parseGroup(String rawGroup) {
		Set<ResourceLocation> group = new HashSet<>();

		for (String rawId : rawGroup.split(",")) {
			ResourceLocation id = ResourceLocation.tryParse(rawId.trim());
			if (id != null) {
				group.add(id);
			}
		}

		return group;
	}

	private static ResourceLocation idOf(Skill skill) {
		return skill == null ? null : skill.getRegistryName();
	}

	private record CompiledGroups(List<String> rawGroups, Map<ResourceLocation, Set<ResourceLocation>> groupsBySkill) {
	}

	public record Conflict(SkillContainer container, Skill skill) {
	}
}
