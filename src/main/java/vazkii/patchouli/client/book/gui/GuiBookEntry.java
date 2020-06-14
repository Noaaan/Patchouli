package vazkii.patchouli.client.book.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.client.base.PersistentData;
import vazkii.patchouli.client.base.PersistentData.DataHolder.BookData;
import vazkii.patchouli.client.base.PersistentData.DataHolder.BookData.Bookmark;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.common.book.Book;

import javax.annotation.Nonnull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class GuiBookEntry extends GuiBook implements IComponentRenderContext {

	final BookEntry entry;
	BookPage leftPage, rightPage;

	final Map<ButtonWidget, Runnable> customButtons = new HashMap<>();

	public GuiBookEntry(Book book, BookEntry entry) {
		this(book, entry, 0);
	}

	public GuiBookEntry(Book book, BookEntry entry, int spread) {
		super(book, new LiteralText(entry.getName()));
		this.entry = entry;
		this.spread = spread;
	}

	@Override
	public void init() {
		super.init();

		maxSpreads = (int) Math.ceil((float) entry.getPages().size() / 2);
		setupPages();
	}

	@Override
	public void onFirstOpened() {
		super.onFirstOpened();

		boolean dirty = false;
		String key = entry.getId().toString();

		BookData data = PersistentData.data.getBookData(book);

		if (!data.viewedEntries.contains(key)) {
			data.viewedEntries.add(key);
			dirty = true;
			entry.markReadStateDirty();
		}

		int index = data.history.indexOf(key);
		if (index != 0) {
			if (index > 0) {
				data.history.remove(key);
			}

			data.history.add(0, key);
			while (data.history.size() > GuiBookEntryList.ENTRIES_PER_PAGE) {
				data.history.remove(GuiBookEntryList.ENTRIES_PER_PAGE);
			}

			dirty = true;
		}

		if (dirty) {
			PersistentData.save();
		}
	}

	@Override
	void drawForegroundElements(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
		drawPage(ms, leftPage, mouseX, mouseY, partialTicks);
		drawPage(ms, rightPage, mouseX, mouseY, partialTicks);

		if (rightPage == null) {
			drawPageFiller(ms, leftPage.book);
		}
	}

	@Override
	public boolean mouseClickedScaled(double mouseX, double mouseY, int mouseButton) {
		return clickPage(leftPage, mouseX, mouseY, mouseButton)
				|| clickPage(rightPage, mouseX, mouseY, mouseButton)
				|| super.mouseClickedScaled(mouseX, mouseY, mouseButton);
	}

	void drawPage(MatrixStack ms, BookPage page, int mouseX, int mouseY, float pticks) {
		if (page == null) {
			return;
		}

		ms.push();
		ms.translate(page.left, page.top, 0);
		page.render(ms, mouseX - page.left, mouseY - page.top, pticks);
		ms.pop();
	}

	boolean clickPage(BookPage page, double mouseX, double mouseY, int mouseButton) {
		if (page != null) {
			return page.mouseClicked(mouseX - page.left, mouseY - page.top, mouseButton);
		}

		return false;
	}

	@Override
	void onPageChanged() {
		setupPages();
		needsBookmarkUpdate = true;
	}

	void setupPages() {
		customButtons.clear();

		if (leftPage != null) {
			leftPage.onHidden(this);
		}
		if (rightPage != null) {
			rightPage.onHidden(this);
		}

		List<BookPage> pages = entry.getPages();
		int leftNum = spread * 2;
		int rightNum = (spread * 2) + 1;

		leftPage = leftNum < pages.size() ? pages.get(leftNum) : null;
		rightPage = rightNum < pages.size() ? pages.get(rightNum) : null;

		if (leftPage != null) {
			leftPage.onDisplayed(this, LEFT_PAGE_X, TOP_PADDING);
		}
		if (rightPage != null) {
			rightPage.onDisplayed(this, RIGHT_PAGE_X, TOP_PADDING);
		}
	}

	public BookEntry getEntry() {
		return entry;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this || (obj instanceof GuiBookEntry && ((GuiBookEntry) obj).entry == entry && ((GuiBookEntry) obj).spread == spread);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(entry) * 31 + Objects.hashCode(spread);
	}

	@Override
	public boolean canBeOpened() {
		return !entry.isLocked() && !equals(MinecraftClient.getInstance().currentScreen);
	}

	@Override
	protected boolean shouldAddAddBookmarkButton() {
		return !isBookmarkedAlready();
	}

	boolean isBookmarkedAlready() {
		if (entry == null || entry.getId() == null) {
			return false;
		}

		String entryKey = entry.getId().toString();
		BookData data = PersistentData.data.getBookData(book);

		for (Bookmark bookmark : data.bookmarks) {
			if (bookmark.entry.equals(entryKey) && bookmark.page == spread) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void bookmarkThis() {
		String entryKey = entry.getId().toString();
		BookData data = PersistentData.data.getBookData(book);
		data.bookmarks.add(new Bookmark(entryKey, spread));
		PersistentData.save();
		needsBookmarkUpdate = true;
	}

	public static void displayOrBookmark(GuiBook currGui, BookEntry entry) {
		Book book = currGui.book;
		GuiBookEntry gui = new GuiBookEntry(currGui.book, entry);

		if (Screen.hasShiftDown()) {
			BookData data = PersistentData.data.getBookData(book);

			if (gui.isBookmarkedAlready()) {
				String key = entry.getId().toString();
				data.bookmarks.removeIf((bm) -> bm.entry.equals(key) && bm.page == 0);
				PersistentData.save();
				currGui.needsBookmarkUpdate = true;
				return;
			} else if (data.bookmarks.size() < MAX_BOOKMARKS) {
				gui.bookmarkThis();
				currGui.needsBookmarkUpdate = true;
				return;
			}
		}

		book.contents.openLexiconGui(gui, true);
	}

	@Override
	public Screen getGui() {
		return this;
	}

	@Override
	public TextRenderer getFont() {
		return book.getFontStyle();
	}

	@Override
	public void renderItemStack(int x, int y, int mouseX, int mouseY, ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}

		client.getItemRenderer().renderInGuiWithOverrides(stack, x, y);
		client.getItemRenderer().renderGuiItemOverlay(textRenderer, stack, x, y);

		if (isMouseInRelativeRange(mouseX, mouseY, x, y, 16, 16)) {
			setTooltipStack(stack);
		}
	}

	@Override
	public void renderIngredient(int x, int y, int mouseX, int mouseY, Ingredient ingr) {
		ItemStack[] stacks = ingr.getMatchingStacksClient();
		if (stacks.length > 0) {
			renderItemStack(x, y, mouseX, mouseY, stacks[(ticksInBook / 20) % stacks.length]);
		}
	}

	@Override
	public void setHoverTooltip(List<String> tooltip) {
		setTooltip(tooltip.stream().map(LiteralText::new).collect(Collectors.toList()));
	}

	@Override
	public void setHoverTooltipComponents(@Nonnull List<Text> tooltip) {
		setTooltip(tooltip);
	}

	@Override
	public boolean isAreaHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
		return isMouseInRelativeRange(mouseX, mouseY, x, y, w, h);
	}

	@Override
	public void registerButton(ButtonWidget button, int pageNum, Runnable onClick) {
		button.x += bookLeft + ((pageNum % 2) == 0 ? LEFT_PAGE_X : RIGHT_PAGE_X);
		button.y += bookTop;

		customButtons.put(button, onClick);
		addButton(button);
	}

	@Override
	public Identifier getBookTexture() {
		return book.bookTexture;
	}

	@Override
	public Identifier getCraftingTexture() {
		return book.craftingTexture;
	}

	@Override
	public int getTextColor() {
		return book.textColor;
	}

	@Override
	public int getHeaderColor() {
		return book.headerColor;
	}

	@Override
	public int getTicksInBook() {
		return ticksInBook;
	}
}
