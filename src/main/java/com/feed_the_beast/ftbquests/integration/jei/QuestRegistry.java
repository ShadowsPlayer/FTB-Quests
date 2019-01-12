package com.feed_the_beast.ftbquests.integration.jei;

import com.feed_the_beast.ftbquests.client.ClientQuestFile;
import com.feed_the_beast.ftbquests.quest.Quest;
import com.feed_the_beast.ftbquests.quest.QuestChapter;

import java.util.ArrayList;

/**
 * @author LatvianModder
 */
public enum QuestRegistry
{
	INSTANCE;

	public final ArrayList<QuestEntry> list = new ArrayList<>();

	@SuppressWarnings("deprecation")
	public void refresh()
	{
		if (FTBQuestsJEIIntegration.RUNTIME != null && !list.isEmpty())
		{
			for (QuestEntry entry : list)
			{
				FTBQuestsJEIIntegration.RUNTIME.getRecipeRegistry().removeRecipe(entry, QuestCategory.UID);
			}
		}

		list.clear();

		if (ClientQuestFile.exists())
		{
			for (QuestChapter chapter : ClientQuestFile.INSTANCE.chapters)
			{
				for (Quest quest : chapter.quests)
				{
					QuestEntry entry = new QuestEntry(quest);
					list.add(entry);
					FTBQuestsJEIIntegration.RUNTIME.getRecipeRegistry().addRecipe(entry, QuestCategory.UID);
				}
			}
		}
	}
}