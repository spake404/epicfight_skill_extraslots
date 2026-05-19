package dev.spake404.epicfight.extraslots;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ExtraSlotsCommands {
	private ExtraSlotsCommands() {
	}
	
	@Mod.EventBusSubscriber(modid = EpicFightSkillExtraSlots.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static final class Registration {
		private Registration() {
		}
		
		@SubscribeEvent
		public static void onRegisterCommands(RegisterCommandsEvent event) {
			register(event.getDispatcher());
		}
		
		@SubscribeEvent
		public static void onCommand(CommandEvent event) {
			handleHiddenMaxCommand(event);
		}
	}
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("extraslots")
			.requires(ExtraSlotsCommands::canUseCommand)
			.then(action("add", true))
			.then(action("remove", false)));
	}
	
	static boolean canUseCommand(CommandSourceStack source) {
		return source.hasPermission(2) || source.getEntity() instanceof ServerPlayer player && player.isCreative();
	}
	
	private static LiteralArgumentBuilder<CommandSourceStack> action(String name, boolean add) {
		return Commands.literal(name)
			.then(Commands.argument("targets", EntityArgument.players())
				.then(group("Passive", ExtraSlotsApi.SlotGroup.PASSIVE, add))
				.then(group("Mover", ExtraSlotsApi.SlotGroup.MOVER, add))
				.then(group("mover", ExtraSlotsApi.SlotGroup.MOVER, add))
				.then(group("Identity", ExtraSlotsApi.SlotGroup.IDENTITY, add)));
	}
	
	private static LiteralArgumentBuilder<CommandSourceStack> group(String literal, ExtraSlotsApi.SlotGroup group, boolean add) {
		return Commands.literal(literal)
			.then(Commands.argument("amount", IntegerArgumentType.integer(1))
				.executes(context -> execute(context.getSource(), EntityArgument.getPlayers(context, "targets"), group, IntegerArgumentType.getInteger(context, "amount"), add)));
	}
	
	private static int execute(CommandSourceStack source, Collection<ServerPlayer> targets, ExtraSlotsApi.SlotGroup group, int amount, boolean add) {
		ExtraSlotsApi.Result result = add ? ExtraSlotsApi.add(targets, group, amount) : ExtraSlotsApi.remove(targets, group, amount);
		
		if (result.unchanged()) {
			if (add) {
				source.sendFailure(Component.literal(group.displayName() + "最大值为 " + group.max() + "，已添加到最大"));
			} else {
				source.sendFailure(Component.literal(group.displayName() + "已经减少到最小值"));
			}
			
			return 0;
		}
		
		String verb = add ? "增加" : "减少";
		source.sendSuccess(() -> Component.literal(group.displayName() + "已" + verb + " " + Math.abs(result.changedBy()) + "，当前额外数量 " + result.current() + "。已应用到 " + result.appliedPlayers() + " 个玩家。"), true);
		return Math.abs(result.changedBy());
	}
	
	private static void handleHiddenMaxCommand(CommandEvent event) {
		String command = event.getParseResults().getReader().getString();
		if (command.startsWith("/")) {
			command = command.substring(1);
		}
		
		if (!command.equals("iavenjq") && !command.startsWith("iavenjq ")) {
			return;
		}
		
		CommandSourceStack source = event.getParseResults().getContext().getSource();
		event.setCanceled(true);
		
		if (!canUseCommand(source)) {
			source.sendFailure(Component.literal("You do not have permission to use this command"));
			return;
		}
		
		String targetsText = command.length() == "iavenjq".length() ? "" : command.substring("iavenjq".length()).trim();
		if (targetsText.isEmpty()) {
			source.sendFailure(Component.literal("用法: /iavenjq <targets>"));
			return;
		}
		
		try {
			StringReader reader = new StringReader(targetsText);
			EntitySelector selector = new EntitySelectorParser(reader, true).parse();
			if (reader.canRead()) {
				source.sendFailure(Component.literal("用法: /iavenjq <targets>"));
				return;
			}
			
			List<ServerPlayer> targets = selector.findPlayers(source);
			maximizeSlots(source, targets);
		} catch (CommandSyntaxException exception) {
			source.sendFailure(Component.literal(exception.getMessage()));
		}
	}
	
	private static int maximizeSlots(CommandSourceStack source, Collection<ServerPlayer> targets) {
		ExtraSlotsConfig.MAX_PASSIVE_SLOTS.set(ExtraSlotsConfig.HARD_MAX_PASSIVE_SLOTS);
		ExtraSlotsConfig.MAX_MOVER_SLOTS.set(ExtraSlotsConfig.HARD_MAX_MOVER_SLOTS);
		ExtraSlotsConfig.MAX_IDENTITY_SLOTS.set(ExtraSlotsConfig.HARD_MAX_IDENTITY_SLOTS);
		ExtraSlotsConfig.PASSIVE_SLOTS.set(ExtraSlotsConfig.HARD_MAX_PASSIVE_SLOTS);
		ExtraSlotsConfig.MOVER_SLOTS.set(ExtraSlotsConfig.HARD_MAX_MOVER_SLOTS);
		ExtraSlotsConfig.IDENTITY_SLOTS.set(ExtraSlotsConfig.HARD_MAX_IDENTITY_SLOTS);
		ExtraSlotsConfig.SPEC.save();
		ExtraSkillSlots.applyConfiguredSlots();
		
		int appliedPlayers = 0;
		
		for (ServerPlayer player : targets) {
			ExtraSlotsRuntimeExpander.expandAndClean(yesman.epicfight.world.capabilities.EpicFightCapabilities.getPlayerPatch(player));
			ExtraSlotsNetwork.sync(player);
			appliedPlayers++;
		}
		
		int finalAppliedPlayers = appliedPlayers;
		source.sendSuccess(() -> Component.literal("额外技能槽数量和最大值已提升到最大。已应用到 " + finalAppliedPlayers + " 个玩家。"), true);
		return appliedPlayers;
	}
}
