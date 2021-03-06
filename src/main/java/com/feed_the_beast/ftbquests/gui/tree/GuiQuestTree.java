package com.feed_the_beast.ftbquests.gui.tree;

import com.feed_the_beast.ftblib.lib.config.ConfigBoolean;
import com.feed_the_beast.ftblib.lib.config.ConfigGroup;
import com.feed_the_beast.ftblib.lib.config.ConfigValueInstance;
import com.feed_the_beast.ftblib.lib.config.IIteratingConfig;
import com.feed_the_beast.ftblib.lib.gui.ContextMenuItem;
import com.feed_the_beast.ftblib.lib.gui.GuiBase;
import com.feed_the_beast.ftblib.lib.gui.GuiHelper;
import com.feed_the_beast.ftblib.lib.gui.GuiIcons;
import com.feed_the_beast.ftblib.lib.gui.IOpenableGui;
import com.feed_the_beast.ftblib.lib.gui.Panel;
import com.feed_the_beast.ftblib.lib.gui.Theme;
import com.feed_the_beast.ftblib.lib.gui.Widget;
import com.feed_the_beast.ftblib.lib.gui.WidgetType;
import com.feed_the_beast.ftblib.lib.icon.Color4I;
import com.feed_the_beast.ftblib.lib.math.MathUtils;
import com.feed_the_beast.ftblib.lib.util.misc.MouseButton;
import com.feed_the_beast.ftbquests.FTBQuests;
import com.feed_the_beast.ftbquests.client.ClientQuestFile;
import com.feed_the_beast.ftbquests.gui.FTBQuestsTheme;
import com.feed_the_beast.ftbquests.gui.GuiSelectQuestObject;
import com.feed_the_beast.ftbquests.gui.GuiVariables;
import com.feed_the_beast.ftbquests.net.edit.MessageChangeProgress;
import com.feed_the_beast.ftbquests.net.edit.MessageEditObjectQuick;
import com.feed_the_beast.ftbquests.net.edit.MessageMoveQuest;
import com.feed_the_beast.ftbquests.quest.EnumChangeProgress;
import com.feed_the_beast.ftbquests.quest.Quest;
import com.feed_the_beast.ftbquests.quest.QuestChapter;
import com.feed_the_beast.ftbquests.quest.QuestObject;
import com.feed_the_beast.ftbquests.quest.QuestObjectBase;
import com.feed_the_beast.ftbquests.quest.QuestObjectType;
import com.feed_the_beast.ftbquests.quest.QuestVariable;
import com.feed_the_beast.ftbquests.quest.reward.RandomReward;
import com.feed_the_beast.ftbquests.quest.task.QuestTask;
import com.feed_the_beast.ftbquests.util.ConfigQuestObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.toasts.SystemToast;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class GuiQuestTree extends GuiBase
{
	public final ClientQuestFile file;
	public int scrollWidth, scrollHeight, prevMouseX, prevMouseY, grabbed;
	public QuestChapter selectedChapter;
	public final HashSet<Quest> selectedQuests;
	public final Panel chapterPanel, quests, questLeft, questRight, otherButtons;
	public final ButtonSubscribe subscribe;
	public Color4I borderColor, backgroundColor;
	public boolean movingQuest = false;
	public int zoom = 16;
	public double zoomd = 16D;
	public long lastShiftPress = 0L;

	public GuiQuestTree(ClientQuestFile q)
	{
		file = q;
		selectedQuests = new HashSet<>();

		chapterPanel = new PanelChapters(this);
		chapterPanel.setHeight(20);

		selectedChapter = file.chapters.isEmpty() ? null : file.chapters.get(0);
		borderColor = Color4I.WHITE.withAlpha(88);
		backgroundColor = Color4I.WHITE.withAlpha(33);

		quests = new PanelQuests(this);
		questLeft = new PanelQuestLeft(this);
		questRight = new PanelQuestRight(this);
		otherButtons = new PanelOtherButtons(this);

		subscribe = new ButtonSubscribe(this);

		selectChapter(null);
	}

	@Nullable
	public Quest getSelectedQuest()
	{
		return selectedQuests.size() == 1 ? selectedQuests.iterator().next() : null;
	}

	@Override
	public void addWidgets()
	{
		add(chapterPanel);
		add(quests);
		add(otherButtons);
		add(questLeft);
		add(questRight);
		add(subscribe);
	}

	@Override
	public void alignWidgets()
	{
		otherButtons.alignWidgets();
		chapterPanel.alignWidgets();
		subscribe.setPos(width - 13, height - 13);
	}

	@Override
	public boolean onInit()
	{
		return setFullscreen();
	}

	public void selectChapter(@Nullable QuestChapter chapter)
	{
		if (selectedChapter != chapter)
		{
			movingQuest = false;
			selectQuest(null);
			selectedChapter = chapter;
			quests.setScrollX(0);
			quests.setScrollY(0);
			quests.refreshWidgets();
			resetScroll(true);
		}
	}

	public void selectQuest(@Nullable Quest quest)
	{
		Quest prev = getSelectedQuest();
		selectedQuests.clear();

		if (prev != quest)
		{
			if (quest != null)
			{
				selectedQuests.add(quest);
			}

			quests.refreshWidgets();
			questLeft.refreshWidgets();
			questRight.refreshWidgets();
		}
	}

	public void resetScroll(boolean realign)
	{
		if (realign)
		{
			quests.alignWidgets();
		}

		quests.setScrollX((scrollWidth - quests.width) / 2);
		quests.setScrollY((scrollHeight - quests.height) / 2);
	}

	public static void addObjectMenuItems(List<ContextMenuItem> contextMenu, IOpenableGui gui, QuestObjectBase object)
	{
		ConfigGroup group = ConfigGroup.newGroup(FTBQuests.MOD_ID);
		ConfigGroup g = object.createSubGroup(group);
		object.getConfig(g);

		if (!g.getValues().isEmpty())
		{
			List<ContextMenuItem> list = new ArrayList<>();

			for (ConfigValueInstance inst : g.getValues())
			{
				if (inst.getValue() instanceof IIteratingConfig)
				{
					String name = inst.getDisplayName().getFormattedText();

					if (!inst.getCanEdit())
					{
						name = TextFormatting.GRAY + name;
					}

					list.add(new ContextMenuItem(name, inst.getIcon(), null)
					{
						@Override
						public void addMouseOverText(List<String> list)
						{
							list.add(inst.getValue().getStringForGUI().getFormattedText());
						}

						@Override
						public void onClicked(Panel panel, MouseButton button)
						{
							inst.getValue().onClicked(gui, inst, button, () -> new MessageEditObjectQuick(object.id, inst.getID(), inst.getValue()).sendToServer());
						}

						@Override
						public void drawIcon(Theme theme, int x, int y, int w, int h)
						{
							if (inst.getValue() instanceof ConfigBoolean)
							{
								(inst.getValue().getBoolean() ? GuiIcons.ACCEPT : GuiIcons.ACCEPT_GRAY).draw(x, y, w, h);
							}
							else
							{
								super.drawIcon(theme, x, y, w, h);
							}
						}
					});
				}
			}

			if (!list.isEmpty())
			{
				list.sort(null);
				contextMenu.addAll(list);
				contextMenu.add(ContextMenuItem.SEPARATOR);
			}
		}

		contextMenu.add(new ContextMenuItem(I18n.format("selectServer.edit"), GuiIcons.SETTINGS, object::onEditButtonClicked));

		if (object instanceof RandomReward && ((RandomReward) object).getTable().id != 0)
		{
			contextMenu.add(new ContextMenuItem(I18n.format("ftbquests.reward_table.edit"), GuiIcons.SETTINGS, () -> ((RandomReward) object).getTable().onEditButtonClicked()));
		}

		contextMenu.add(new ContextMenuItem(I18n.format("selectServer.delete"), GuiIcons.REMOVE, () -> ClientQuestFile.INSTANCE.deleteObject(object.id)).setYesNo(I18n.format("delete_item", object.getDisplayName().getFormattedText())));
		contextMenu.add(new ContextMenuItem(I18n.format("ftbquests.gui.reset_progress"), GuiIcons.REFRESH, () -> new MessageChangeProgress(ClientQuestFile.INSTANCE.self.getTeamUID(), object.id, EnumChangeProgress.RESET_DEPS).sendToServer()).setYesNo(I18n.format("ftbquests.gui.reset_progress_q")));

		if (object instanceof QuestObject)
		{
			contextMenu.add(new ContextMenuItem(I18n.format("ftbquests.gui.complete_instantly"), FTBQuestsTheme.COMPLETED, () -> new MessageChangeProgress(ClientQuestFile.INSTANCE.self.getTeamUID(), object.id, EnumChangeProgress.COMPLETE_DEPS).sendToServer()).setYesNo(I18n.format("ftbquests.gui.complete_instantly_q")));
		}

		contextMenu.add(new ContextMenuItem(I18n.format("ftbquests.gui.copy_id"), GuiIcons.INFO, () -> setClipboardString(object.getCodeString()))
		{
			@Override
			public void addMouseOverText(List<String> list)
			{
				list.add(object.getCodeString());
			}
		});
	}

	public static void displayError(ITextComponent error)
	{
		Minecraft.getMinecraft().getToastGui().add(new SystemToast(SystemToast.Type.TUTORIAL_HINT, new TextComponentTranslation("ftbquests.gui.error"), error));
	}

	@Override
	public boolean keyPressed(int key, char keyChar)
	{
		if (super.keyPressed(key, keyChar))
		{
			return true;
		}
		else if (key == Keyboard.KEY_TAB)
		{
			if (selectedChapter != null && file.chapters.size() > 1)
			{
				selectChapter(file.chapters.get(MathUtils.mod(selectedChapter.getIndex() + (isShiftKeyDown() ? -1 : 1), file.chapters.size())));
			}

			return true;
		}
		else if (keyChar >= '1' && keyChar <= '9')
		{
			int i = keyChar - '1';

			if (i < file.chapters.size())
			{
				selectChapter(file.chapters.get(i));
			}

			return true;
		}
		else if (selectedChapter != null && file.canEdit() && isCtrlKeyDown() && !isShiftKeyDown() && !isAltKeyDown())
		{
			switch (key)
			{
				case Keyboard.KEY_A:
					movingQuest = false;
					selectQuest(null);
					selectedQuests.addAll(selectedChapter.quests);
					break;
				case Keyboard.KEY_D:
					movingQuest = false;
					selectQuest(null);
					break;
				case Keyboard.KEY_DOWN:
					movingQuest = true;
					for (Quest quest : selectedQuests)
					{
						new MessageMoveQuest(quest.id, quest.x, (byte) (quest.y + 1)).sendToServer();
					}
					movingQuest = false;
					break;
				case Keyboard.KEY_UP:
					movingQuest = true;
					for (Quest quest : selectedQuests)
					{
						new MessageMoveQuest(quest.id, quest.x, (byte) (quest.y - 1)).sendToServer();
					}
					movingQuest = false;
					break;
				case Keyboard.KEY_LEFT:
					movingQuest = true;
					for (Quest quest : selectedQuests)
					{
						new MessageMoveQuest(quest.id, (byte) (quest.x - 1), quest.y).sendToServer();
					}
					movingQuest = false;
					break;
				case Keyboard.KEY_RIGHT:
					movingQuest = true;
					for (Quest quest : selectedQuests)
					{
						new MessageMoveQuest(quest.id, (byte) (quest.x + 1), quest.y).sendToServer();
					}
					movingQuest = false;
					break;
			}

			return true;
		}
		else if (key == Keyboard.KEY_LSHIFT)
		{
			long now = System.currentTimeMillis();

			if (lastShiftPress == 0L)
			{
				lastShiftPress = now;
			}
			else
			{
				if (now - lastShiftPress <= 400L)
				{
					ConfigQuestObject c = new ConfigQuestObject(file, null, QuestObjectType.CHAPTER, QuestObjectType.QUEST);
					GuiSelectQuestObject gui = new GuiSelectQuestObject(c, this, () -> {
						QuestObjectBase o = c.getObject();

						if (o instanceof QuestChapter)
						{
							selectChapter((QuestChapter) o);
						}
						else if (o instanceof Quest)
						{
							zoom = 20;
							selectChapter(((Quest) o).chapter);
							selectQuest((Quest) o);
						}
					});

					gui.focus();
					gui.setTitle(I18n.format("gui.search_box"));
					gui.openGui();
				}

				lastShiftPress = 0L;
			}
		}

		return false;
	}

	@Override
	public void drawBackground(Theme theme, int x, int y, int w, int h)
	{
		if (zoomd < zoom)
		{
			zoomd += MathHelper.clamp((zoom - zoomd) / 8D, 0.1D, 0.8D);

			if (zoomd > zoom)
			{
				zoomd = zoom;
			}

			grabbed = 0;
			resetScroll(true);
			//quests.alignWidgets();
		}
		else if (zoomd > zoom)
		{
			zoomd -= MathHelper.clamp((zoomd - zoom) / 8D, 0.1D, 0.8D);

			if (zoomd < zoom)
			{
				zoomd = zoom;
			}

			grabbed = 0;
			resetScroll(true);
			//quests.alignWidgets();
		}

		if (selectedChapter != null && selectedChapter.invalid)
		{
			selectChapter(null);
		}

		if (selectedChapter == null && !file.chapters.isEmpty())
		{
			selectChapter(file.chapters.get(0));
		}

		super.drawBackground(theme, x, y, w, h);

		if (grabbed != 0)
		{
			int mx = getMouseX();
			int my = getMouseY();

			if (scrollWidth > quests.width)
			{
				quests.setScrollX(Math.max(Math.min(quests.getScrollX() + (prevMouseX - mx), scrollWidth - quests.width), 0));
			}
			else
			{
				quests.setScrollX((scrollWidth - quests.width) / 2);
			}

			if (scrollHeight > quests.height)
			{
				quests.setScrollY(Math.max(Math.min(quests.getScrollY() + (prevMouseY - my), scrollHeight - quests.height), 0));
			}
			else
			{
				quests.setScrollY((scrollHeight - quests.height) / 2);
			}

			prevMouseX = mx;
			prevMouseY = my;
		}
	}

	@Override
	public void drawForeground(Theme theme, int x, int y, int w, int h)
	{
		GuiHelper.drawHollowRect(x, y, w, h, borderColor, false);

		int start = 1;

		if (!chapterPanel.widgets.isEmpty())
		{
			Widget last = chapterPanel.widgets.get(chapterPanel.widgets.size() - 1);
			start = last.getX() + last.width + 1;
		}

		backgroundColor.draw(start, y + 1, w - start - otherButtons.width - 1, chapterPanel.height - 2);
		borderColor.draw(start, y + chapterPanel.height - 1, w - start - 1, 1);

		Quest selectedQuest = getSelectedQuest();

		if (selectedQuest != null && !movingQuest)
		{
			GlStateManager.pushMatrix();
			GlStateManager.translate(0F, 0F, 500F);
			String txt = selectedQuest.getDisplayName().getFormattedText();
			int txts = theme.getStringWidth(txt);
			GuiHelper.drawHollowRect(2, chapterPanel.height + 1, txts + 6, 14, borderColor, false);
			theme.drawGui(3, chapterPanel.height + 2, txts + 4, 12, WidgetType.DISABLED);
			theme.drawString(txt, 5, chapterPanel.height + 4);
			GlStateManager.popMatrix();
		}

		super.drawForeground(theme, x, y, w, h);
	}

	@Override
	public Theme getTheme()
	{
		return FTBQuestsTheme.INSTANCE;
	}

	@Override
	public boolean drawDefaultBackground()
	{
		return false;
	}

	public void open(@Nullable QuestObject object)
	{
		if (object instanceof QuestVariable)
		{
			new GuiVariables().openGui();
			return;
		}
		else if (object instanceof QuestChapter)
		{
			selectChapter((QuestChapter) object);
		}
		else if (object instanceof Quest)
		{
			selectChapter(((Quest) object).chapter);
			selectQuest((Quest) object);
		}
		else if (object instanceof QuestTask)
		{
			selectChapter(((QuestTask) object).quest.chapter);
			selectQuest(((QuestTask) object).quest);
		}

		openGui();
	}

	@Override
	public boolean handleClick(String scheme, String path)
	{
		if (scheme.isEmpty() && path.startsWith("#"))
		{
			open(file.get(file.getID(path)));
			return true;
		}

		return super.handleClick(scheme, path);
	}
}