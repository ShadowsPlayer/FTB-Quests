package com.feed_the_beast.ftbquests.quest.reward;

import com.feed_the_beast.ftblib.lib.config.ConfigGroup;
import com.feed_the_beast.ftblib.lib.gui.GuiIcons;
import com.feed_the_beast.ftblib.lib.gui.IOpenableGui;
import com.feed_the_beast.ftblib.lib.gui.misc.GuiEditConfig;
import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftbquests.FTBQuests;
import com.feed_the_beast.ftbquests.gui.GuiSelectQuestObject;
import com.feed_the_beast.ftbquests.quest.Quest;
import com.feed_the_beast.ftbquests.quest.QuestObjectBase;
import com.feed_the_beast.ftbquests.quest.QuestObjectType;
import com.feed_the_beast.ftbquests.quest.loot.RewardTable;
import com.feed_the_beast.ftbquests.util.ConfigQuestObject;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public final class QuestRewardType extends IForgeRegistryEntry.Impl<QuestRewardType>
{
	private static ForgeRegistry<QuestRewardType> REGISTRY;

	public static void createRegistry()
	{
		if (REGISTRY == null)
		{
			ResourceLocation registryName = new ResourceLocation(FTBQuests.MOD_ID, "rewards");
			REGISTRY = (ForgeRegistry<QuestRewardType>) new RegistryBuilder<QuestRewardType>().setType(QuestRewardType.class).setName(registryName).create();
			MinecraftForge.EVENT_BUS.post(new RegistryEvent.Register<>(registryName, REGISTRY));
		}
	}

	public static ForgeRegistry<QuestRewardType> getRegistry()
	{
		return REGISTRY;
	}

	@Nullable
	public static QuestReward createReward(Quest quest, String id)
	{
		if (id.isEmpty())
		{
			id = FTBQuests.MOD_ID + ":item";
		}
		else if (id.indexOf(':') == -1)
		{
			id = FTBQuests.MOD_ID + ':' + id;
		}

		QuestRewardType type = REGISTRY.getValue(new ResourceLocation(id));

		if (type == null)
		{
			if (id.equals("ftbquests:ftb_money"))
			{
				return createReward(quest, "ftbmoney:money");
			}

			return null;
		}

		return type.provider.create(quest);
	}

	@FunctionalInterface
	public interface Provider
	{
		QuestReward create(QuestObjectBase parent);
	}

	public interface GuiProvider
	{
		@SideOnly(Side.CLIENT)
		void openCreationGui(IOpenableGui gui, QuestObjectBase parent, Consumer<QuestReward> callback);
	}

	public final Provider provider;
	private ITextComponent displayName;
	private Icon icon;
	private GuiProvider guiProvider;
	private boolean excludeFromListRewards;

	public QuestRewardType(Provider p)
	{
		provider = p;
		displayName = null;
		icon = GuiIcons.MONEY_BAG;
		guiProvider = new GuiProvider()
		{
			@Override
			@SideOnly(Side.CLIENT)
			public void openCreationGui(IOpenableGui gui, QuestObjectBase parent, Consumer<QuestReward> callback)
			{
				QuestReward reward = provider.create(parent);

				if (reward instanceof RandomReward)
				{
					ConfigQuestObject config = new ConfigQuestObject(parent.getQuestFile(), null, QuestObjectType.REWARD_TABLE);
					new GuiSelectQuestObject(config, gui, () -> {
						((RandomReward) reward).table = (RewardTable) config.getObject();
						callback.accept(reward);
					}).openGui();
					return;
				}

				ConfigGroup group = ConfigGroup.newGroup(FTBQuests.MOD_ID);
				reward.getConfig(reward.createSubGroup(group));
				new GuiEditConfig(group, (g1, sender) -> callback.accept(reward)).openGui();
			}
		};
	}

	public String getTypeForNBT()
	{
		return getRegistryName().getNamespace().equals(FTBQuests.MOD_ID) ? getRegistryName().getPath() : getRegistryName().toString();
	}

	public QuestRewardType setDisplayName(ITextComponent name)
	{
		displayName = name;
		return this;
	}

	public ITextComponent getDisplayName()
	{
		if (displayName != null)
		{
			return displayName.createCopy();
		}

		ResourceLocation id = getRegistryName();
		return new TextComponentTranslation("ftbquests.reward." + id.getNamespace() + '.' + id.getPath());
	}

	public QuestRewardType setIcon(Icon i)
	{
		icon = i;
		return this;
	}

	public Icon getIcon()
	{
		return icon;
	}

	public QuestRewardType setGuiProvider(GuiProvider p)
	{
		guiProvider = p;
		return this;
	}

	public GuiProvider getGuiProvider()
	{
		return guiProvider;
	}

	public QuestRewardType setExcludeFromListRewards(boolean v)
	{
		excludeFromListRewards = v;
		return this;
	}

	public boolean getExcludeFromListRewards()
	{
		return excludeFromListRewards;
	}
}