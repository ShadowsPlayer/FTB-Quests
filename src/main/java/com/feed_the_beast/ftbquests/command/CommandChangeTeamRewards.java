package com.feed_the_beast.ftbquests.command;

import com.feed_the_beast.ftblib.FTBLib;
import com.feed_the_beast.ftbquests.net.edit.MessageEditObjectResponse;
import com.feed_the_beast.ftbquests.quest.Quest;
import com.feed_the_beast.ftbquests.quest.QuestChapter;
import com.feed_the_beast.ftbquests.quest.ServerQuestFile;
import com.feed_the_beast.ftbquests.quest.reward.QuestReward;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author LatvianModder
 */
public class CommandChangeTeamRewards extends CommandFTBQuestsBase
{
	@Override
	public String getName()
	{
		return "change_team_rewards";
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
	{
		if (args.length == 2)
		{
			return getListOfStringsMatchingLastWord(args, "true", "false");
		}
		else if (args.length == 1)
		{
			List<String> list = new ArrayList<>(ServerQuestFile.INSTANCE.chapters.size() + 1);
			list.add("*");

			for (QuestChapter chapter : ServerQuestFile.INSTANCE.chapters)
			{
				list.add(chapter.toString());
			}

			return getListOfStringsMatchingLastWord(args, list);
		}

		return super.getTabCompletions(server, sender, args, pos);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		if (args.length < 2)
		{
			throw new WrongUsageException(getUsage(sender));
		}

		Collection<QuestChapter> chapters;

		if (args[0].equals("*"))
		{
			chapters = ServerQuestFile.INSTANCE.chapters;
		}
		else
		{
			QuestChapter chapter = ServerQuestFile.INSTANCE.getChapter(ServerQuestFile.INSTANCE.getID(args[0]));

			if (chapter == null)
			{
				throw FTBLib.error(sender, "commands.ftbquests.change_team_rewards.invalid_id", args[0]);
			}

			chapters = Collections.singleton(chapter);
		}

		boolean value = parseBoolean(args[1]);

		ServerQuestFile.INSTANCE.clearCachedData();
		int r = 0;

		for (QuestChapter chapter : chapters)
		{
			for (Quest quest : chapter.quests)
			{
				for (QuestReward reward : quest.rewards)
				{
					if (reward.team != value)
					{
						reward.team = value;
						r++;
						new MessageEditObjectResponse(reward).sendToAll();
					}
				}
			}
		}

		ServerQuestFile.INSTANCE.save();

		sender.sendMessage(new TextComponentTranslation("commands.ftbquests.change_team_rewards.text", r, Boolean.toString(value)));
	}
}