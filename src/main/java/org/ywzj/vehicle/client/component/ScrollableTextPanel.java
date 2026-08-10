package org.ywzj.vehicle.client.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.ywzj.vehicle.client.render.util.Color;

import java.util.Objects;

public class ScrollableTextPanel {

    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 4;
    private int left;
    private int top;
    private int right;
    private int bottom;
    private int scrollOffset;
    private int maxScroll;
    private Object contentKey;
    private Component content;

    public void setBounds(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public void setContent(Object contentKey, Component content) {
        if (!Objects.equals(this.contentKey, contentKey)) {
            this.contentKey = contentKey;
            this.scrollOffset = 0;
        }
        this.content = content;
    }

    public void clear() {
        this.contentKey = null;
        this.content = null;
        this.scrollOffset = 0;
        this.maxScroll = 0;
    }

    public void resetScroll() {
        this.scrollOffset = 0;
    }

    public void render(GuiGraphics guiGraphics, Font font, int textColor) {
        int viewportWidth = right - left;
        int viewportHeight = bottom - top;
        if (content == null || viewportWidth <= 0 || viewportHeight <= 0) {
            maxScroll = 0;
            scrollOffset = 0;
            return;
        }

        var lines = font.split(content, viewportWidth);
        int contentHeight = lines.size() * font.lineHeight;
        boolean overflowing = contentHeight > viewportHeight;
        if (overflowing) {
            int textWidth = Math.max(1, viewportWidth - SCROLLBAR_WIDTH - SCROLLBAR_GAP);
            lines = font.split(content, textWidth);
            contentHeight = lines.size() * font.lineHeight;
        }

        maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
        int textRight = overflowing
                ? Math.max(left + 1, right - SCROLLBAR_WIDTH - SCROLLBAR_GAP)
                : right;

        guiGraphics.enableScissor(left, top, textRight, bottom);
        int textY = top - scrollOffset;
        for (int i = 0; i < lines.size(); i++) {
            int lineY = textY + i * font.lineHeight;
            if (lineY + font.lineHeight > top && lineY < bottom) {
                guiGraphics.drawString(font, lines.get(i), left, lineY, textColor);
            }
        }
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            drawScrollBar(guiGraphics, contentHeight);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta, Font font) {
        if (content == null || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (maxScroll > 0) {
            scrollOffset = (int) Mth.clamp(
                    scrollOffset - Math.signum(delta) * font.lineHeight * 2,
                    0,
                    maxScroll
            );
        }
        return true;
    }

    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    private void drawScrollBar(GuiGraphics guiGraphics, int contentHeight) {
        int viewportHeight = bottom - top;
        int x = right - SCROLLBAR_WIDTH;
        guiGraphics.fill(x, top, right, bottom, Color.SCROLLBAR_TRACK);
        int knobHeight = Math.min(viewportHeight, Math.max(12, viewportHeight * viewportHeight / contentHeight));
        int movable = viewportHeight - knobHeight;
        int knobY = top + (int) (movable * (scrollOffset / (float) maxScroll));
        guiGraphics.fill(x, knobY, right, knobY + knobHeight, Color.SCROLLBAR_KNOB);
    }

}
