package com.voxivoid.recipelab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Two-column recipe browser: groups left, recipes of the highlighted group right. Canvas-drawn.
 * The first group is All (every recipe), the second Favorites (the marked recipes, in marking order), the third Custom
 * (the custom recipes from the memory card, A to Z); the rest are the brands.
 */
public class PickerView extends View {
    private static final int ACCENT = 0xFFFF9F1C, INK = 0xFF0E1015;

    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG), edge = new Paint(Paint.ANTI_ALIAS_FLAG), sel = new Paint(Paint.ANTI_ALIAS_FLAG),
            head = new Paint(Paint.ANTI_ALIAS_FLAG), item = new Paint(Paint.ANTI_ALIAS_FLAG), small = new Paint(Paint.ANTI_ALIAS_FLAG), rule = new Paint(),
            track = new Paint(Paint.ANTI_ALIAS_FLAG), thumb = new Paint(Paint.ANTI_ALIAS_FLAG), tagBg = new Paint(Paint.ANTI_ALIAS_FLAG),
            star = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float d;
    private final Legend legend;
    private Keys.Caps caps = Keys.Caps.UNKNOWN;
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int selected = 0, column = 1, group = 0;              // column: 0 groups, 1 recipes · group: Favorites.ALL, .GROUP, .CUSTOM or a brand
    private List<Integer> favs = new ArrayList<Integer>();
    private Library lib = new Library();
    private boolean card = true;                                  // a memory card is in, so custom recipes can be kept

    public PickerView(Context c, AttributeSet a) {
        super(c, a);
        d = c.getResources().getDisplayMetrics().density;
        legend = new Legend(d);
        bg.setColor(0xF40D1017);
        edge.setColor(0x66FF9F1C); edge.setStyle(Paint.Style.STROKE); edge.setStrokeWidth(d);
        sel.setColor(ACCENT);
        outline.setColor(ACCENT); outline.setStyle(Paint.Style.STROKE); outline.setStrokeWidth(1.5f * d);
        head.setColor(0x99FFFFFF); head.setTextSize(9 * d); head.setFakeBoldText(true);
        item.setColor(0xFFFFFFFF); item.setTextSize(13 * d);
        small.setColor(0x99FFFFFF); small.setTextSize(10 * d);
        rule.setColor(0x33FFFFFF);
        track.setColor(0x26FFFFFF); thumb.setColor(0xCCFF9F1C);
    }

    /** what the key probe found; until it is set only the universal keys are named */
    public void setCaps(Keys.Caps k) { caps = k; invalidate(); }

    /** the display language's typeface ({@link UiFont}) */
    public void setTypeface(Typeface tf) { head.setTypeface(tf); item.setTypeface(tf); small.setTypeface(tf); legend.setTypeface(tf); invalidate(); }

    /**
     * The highlighted recipe, the active column, the group the left column is on, the favorites in
     * marking order, the recipes with the custom ones, and whether a memory card is in.
     */
    public void set(int recipe, int col, int grp, List<Integer> favorites, Library library, boolean cardIn) {
        selected = recipe; column = col; group = grp; favs = favorites; lib = library; card = cardIn; invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), pad = 12 * d;
        c.drawRect(0, 0, w, h, bg);

        int g = group;
        boolean favGroup = g == Favorites.GROUP, mixed = favGroup || g == Favorites.ALL;   // mixed: rows of several brands
        int count = Favorites.groupCount(g, favs, lib);
        float colX = w * 0.35f;                                 // divider
        int mode = column == 0 ? Keys.H_BRANDS : lib.isCustom(selected) ? Keys.H_RECIPES_CUSTOM : Keys.H_RECIPES;
        Keys.Hints hints = Keys.hints(mode, caps);
        float legTop = h - pad - legend.height(legend.lines(w - 2 * pad, hints)) + 2 * d;   // the legend, on as many lines as it needs
        float top = pad + 12 * d, bottom = legTop - 6 * d;      // header / footer reserved
        float sbW = 4 * d;                                      // scrollbar width
        head.setColor(column == 0 ? ACCENT : 0x99FFFFFF);
        c.drawText(Lang.t("picker_brand"), pad, pad + 7 * d, head);
        head.setColor(column == 1 ? ACCENT : 0x99FFFFFF);
        c.drawText(Favorites.groupName(g).toUpperCase() + "  ·  " + Favorites.groupCount(g, favs, lib), colX + pad, pad + 7 * d, head);
        head.setColor(0x99FFFFFF);
        c.drawLine(colX, pad, colX, h - pad, rule);
        c.drawLine(pad, top + 3 * d, w - pad, top + 3 * d, rule);

        // ---- left: All, Favorites, Custom, then the brands
        int ng = Favorites.groupRows(), gRow = Favorites.groupRow(g);
        float listTop = top + 6 * d, listH = bottom - listTop;
        float rowH = 24 * d;
        int gVisible = Math.max(1, (int) (listH / rowH));
        int gFirst = ng > gVisible ? Math.max(0, Math.min(gRow - gVisible / 2, ng - gVisible)) : 0;
        float gRight = colX - 8 * d - (ng > gVisible ? sbW + 4 * d : 0);
        float y = listTop;
        for (int i = gFirst; i < Math.min(ng, gFirst + gVisible); i++, y += rowH) {
            int gi = Favorites.groupAt(i);
            boolean on = i == gRow, active = on && column == 0;
            if (on) { r.set(pad - 4 * d, y, gRight, y + rowH); c.drawRoundRect(r, 4 * d, 4 * d, active ? sel : outline); }
            item.setColor(active ? INK : on || gi < 0 ? ACCENT : 0xCCFFFFFF); item.setFakeBoldText(on);   // All, Favorites and Custom in the accent
            small.setColor(active ? 0xAA0E1015 : 0x66FFFFFF);
            String n = String.valueOf(Favorites.groupCount(gi, favs, lib));
            float nX = gRight - 5 * d - small.measureText(n);
            c.drawText(fit(Favorites.groupName(gi), item, nX - pad - 4 * d), pad, y + rowH / 2 - (item.ascent() + item.descent()) / 2f, item);
            c.drawText(n, nX, y + rowH / 2 - (small.ascent() + small.descent()) / 2f, small);
            if (i == Favorites.SPECIAL_ROWS - 1) c.drawLine(pad, y + rowH - d, gRight, y + rowH - d, rule);   // set apart from the brands
        }
        item.setFakeBoldText(false);
        if (ng > gVisible) scrollbar(c, colX - 6 * d - sbW, listTop, listH, sbW, gFirst, gVisible, ng);

        // ---- right: the group's recipes, windowed around the highlight
        float x = colX + pad;
        if (count == 0) {                                       // an empty Favorites or Custom group says so, and how to fill it
            item.setColor(0xCCFFFFFF);
            c.drawText(Favorites.emptyTitle(g, card), x, listTop + 20 * d, item);
            small.setColor(0x99FFFFFF);
            c.drawText(Favorites.emptyHint(g), x, listTop + 36 * d, small);
        } else {
            float rh = 27 * d;
            int visible = Math.max(1, (int) (listH / rh));
            int selPos = Math.max(0, Favorites.positionIn(g, selected, favs, lib));
            int first = 0;
            boolean scroll = count > visible;
            if (scroll) { first = Math.max(0, Math.min(selPos - visible / 2, count - visible)); }
            float xr = w - pad - (scroll ? sbW + 6 * d : 0);
            y = listTop;
            for (int k = first; k < Math.min(count, first + visible); k++, y += rh) {
                int idx = Favorites.recipeAt(g, k, favs, lib);
                boolean on = idx == selected, active = on && column == 1;
                if (on) { r.set(x - 4 * d, y, xr, y + rh); c.drawRoundRect(r, 4 * d, 4 * d, active ? sel : outline); }
                Recipes.Recipe rc = lib.get(idx);
                item.setColor(active ? INK : on ? ACCENT : 0xFFFFFFFF); item.setFakeBoldText(on);
                // a Picture Effect recipe works only as JPEG: say so; every other recipe needs no tag
                float tx = rc.isEffect() ? tag(c, Lang.t("tag_jpeg_only"), xr - 4 * d, y, active, active ? 0x330E1015 : 0x55FF9F1C, active ? INK : 0xCCFFFFFF) : xr;
                if (!favGroup && favs.contains(idx)) { star.setColor(active ? INK : ACCENT); Legend.star(c, tx - 4 * d - 6 * d, y + rh / 2, 6 * d, star); tx -= 16 * d; }
                float maxTextW = tx - x - 6 * d;
                String original = Lang.current() == Lang.VI ? null : Recipes.originalName(rc), brand = mixed ? Recipes.groupLabel(rc.group) : null;
                String detail = brand != null && original != null ? brand + "  ·  " + original : brand != null ? brand : original;
                c.drawText(fit(Recipes.displayName(rc), item, maxTextW), x, detail == null ? y + rh / 2 - (item.ascent() + item.descent()) / 2f : y + 11.5f * d, item);
                small.setColor(active ? 0xAA0E1015 : 0x80FFFFFF);
                if (detail != null) c.drawText(fit(detail, small, maxTextW), x, y + 22.5f * d, small);
            }
            item.setFakeBoldText(false);
            if (scroll) scrollbar(c, w - pad - sbW, listTop, listH, sbW, first, visible, count);
        }

        // ---- footer: icon legend
        c.drawLine(pad, legTop - 2 * d, w - pad, legTop - 2 * d, rule);
        legend.drawWrapped(c, pad, legTop, w - 2 * pad, hints);
    }

    private static String fit(String s, final Paint p, float max) {
        return TextFit.ellipsize(s, new TextFit.Width() {
            public float of(String str, int start, int end) { return p.measureText(str, start, end); }
        }, Math.max(0, max));
    }

    /** a small pill ending at {@code right} on the row at {@code y}; returns its left edge */
    private float tag(Canvas c, String text, float right, float y, boolean active, int bgColor, int textColor) {
        float tw = head.measureText(text) + 8 * d, tx = right - tw;
        r.set(tx, y + 5 * d, tx + tw, y + 17 * d);
        tagBg.setColor(bgColor);
        c.drawRoundRect(r, 2 * d, 2 * d, tagBg);
        head.setColor(textColor);
        c.drawText(text, tx + 4 * d, y + 14 * d, head);
        head.setColor(0x99FFFFFF);
        return tx;
    }

    /** vertical scrollbar: track + thumb proportional to the visible window */
    private void scrollbar(Canvas c, float x, float top, float height, float width, int first, int visible, int total) {
        r.set(x, top, x + width, top + height); c.drawRoundRect(r, width / 2, width / 2, track);
        float thumbH = Math.max(12 * d, height * visible / total);
        float thumbY = top + (height - thumbH) * first / Math.max(1, total - visible);
        r.set(x, thumbY, x + width, thumbY + thumbH); c.drawRoundRect(r, width / 2, width / 2, thumb);
    }
}
