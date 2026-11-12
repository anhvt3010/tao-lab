package com.voxivoid.recipelab;

/**
 * Film-look approximations built ONLY from settings the A6000 can store persistently. Names follow the popular
 * film-simulation lists; values are original. No gamma / per-hue colour depth / split-toning exists on this body,
 * so log profiles (S-Log, V-Log, Blackmagic Film) and tinted monochromes (selenium, cyanotype) are not included.
 *
 * style   : Creative Style (stored enum verified on camera: 1 standard, 2 vivid, 3 neutral, 6 mono, 14 sepia; 13 is some style we have not identified, 4..12 still guessed from the runtime list order)
 * sat/con/sharp : Creative Style adjustments (-3..+3: the live preview takes more, but the store keeps only the menu range)
 * wbMode  : 0 = leave WB as is · 1 = auto · 14 = colour temperature (kelvin)
 * ab / gm : WB fine tune  amber(+)/blue(-)  green(+)/magenta(-)  (-7..+7)
 * pe      : Picture Effect (persistent; when on, Creative Style is ignored by the camera and RAW is disabled)
 * ev      : exposure bias in 1/3 EV steps (persistent)
 * dro     : DRO 0 off, 1..5, 6 auto (persistent)
 * sub     : effect sub-parameter — Soft High-key tint 0 blue / 1 pink / 2 green, Toy tone 0..4, Partial hue 0..3, Posterization 0 colour / 1 b&w
 */
public class Recipes {
    /**
     * Sony A6300 Deep-Color Picture Profile hardware configuration (written to 0x010701d0 + 10 * k + pp).
     * Controls Black Level, Gamma/Color Mode, PP Saturation, Color Phase (skintone axis), 6-axis Color Depth
     * (B, C, G, M, R, Y), and Detail Level (organic film edge softness).
     */
    public static final class PpProfile {
        public final int pp, mode, black, sat, phase, r, g, b, c, m, y, detail;
        private final String valuesKey;
        public PpProfile(int pp, int mode, int black, int sat, int phase,
                         int r, int g, int b, int c, int m, int y, int detail) {
            this.pp = pp; this.mode = mode; this.black = black; this.sat = sat; this.phase = phase;
            this.r = r; this.g = g; this.b = b; this.c = c; this.m = m; this.y = y; this.detail = detail;
            valuesKey = mode + "," + black + "," + sat + "," + phase + "," + r + "," + g + "," + b
                    + "," + c + "," + m + "," + y + "," + detail;
        }
    }

    public static final int PP_MODE_MOVIE = 1, PP_MODE_STILL = 2, PP_MODE_CINE = 3, PP_MODE_PRO = 3, PP_MODE_ITU709 = 4;

    private static PpProfile pp(int mode, int black, int sat, int phase, int r, int g, int b, int c, int m, int y, int detail) {
        return new PpProfile(Params.PP_DEFAULT_SLOT, mode, black, sat, phase, r, g, b, c, m, y, detail);
    }

    public static class Recipe {
        public final int group; public final String name; public final int style, sat, con, sharp, wbMode, kelvin, ab, gm, pe, ev, dro, sub;
        public final PpProfile ppProfile;
        private final String[] ppKeys;
        Recipe(int group, String name, int style, int sat, int con, int sharp, int wbMode, int kelvin, int ab, int gm) {
            this(group, name, style, sat, con, sharp, wbMode, kelvin, ab, gm, 0, 0, DRO_AUTO, 0, null);
        }
        Recipe(int group, String name, int style, int sat, int con, int sharp, int wbMode, int kelvin, int ab, int gm, PpProfile ppProfile) {
            this(group, name, style, sat, con, sharp, wbMode, kelvin, ab, gm, 0, 0, DRO_AUTO, 0, ppProfile);
        }
        Recipe(int group, String name, int style, int sat, int con, int sharp, int wbMode, int kelvin, int ab, int gm, int pe, int ev, int dro) {
            this(group, name, style, sat, con, sharp, wbMode, kelvin, ab, gm, pe, ev, dro, 0, null);
        }
        Recipe(int group, String name, int style, int sat, int con, int sharp, int wbMode, int kelvin, int ab, int gm, int pe, int ev, int dro, PpProfile ppProfile) {
            this(group, name, style, sat, con, sharp, wbMode, kelvin, ab, gm, pe, ev, dro, 0, ppProfile);
        }
        Recipe(int group, String name, int style, int sat, int con, int sharp, int wbMode, int kelvin, int ab, int gm, int pe, int ev, int dro, int sub) {
            this(group, name, style, sat, con, sharp, wbMode, kelvin, ab, gm, pe, ev, dro, sub, null);
        }
        Recipe(int group, String name, int style, int sat, int con, int sharp, int wbMode, int kelvin, int ab, int gm, int pe, int ev, int dro, int sub, PpProfile ppProfile) {
            this.sub = sub;
            this.group = group; this.name = name; this.style = style; this.sat = sat; this.con = con; this.sharp = sharp;
            this.wbMode = wbMode; this.kelvin = kelvin; this.ab = ab; this.gm = gm; this.pe = pe; this.ev = ev; this.dro = dro;
            this.ppProfile = ppProfile;
            this.ppKeys = ppProfile == null ? null : new String[10];
        }
        /** At most one string per selected PP slot, instead of formatting write lists on every redraw. */
        String ppKey(int pp) {
            if (ppKeys == null || pe != 0 || pp < 1 || pp > 9) return "";
            if (ppKeys[pp] == null) ppKeys[pp] = name + ":" + pp + ":" + ppProfile.valuesKey;
            return ppKeys[pp];
        }
        public boolean isEffect() { return pe != 0; }
        public boolean hasPictureProfile() { return ppProfile != null; }
        /** a recipe the user made, kept on the memory card ({@link CustomRecipes}), rather than one of the table below */
        public boolean isCustom() { return group == CUSTOM; }
        /** one-line summary for lists, in the display language: "Neutral  -4/-1  A1" */
        public String summary() {
            StringBuilder s = new StringBuilder();
            if (pe != 0) { s.append(peLabel(pe)); String sl = subLabel(pe, sub); if (sl != null) s.append(' ').append(sl); } else s.append(styleLabel(style)).append("  ").append(sat > 0 ? "+" : "").append(sat).append('/').append(con > 0 ? "+" : "").append(con);
            if (ev != 0) s.append("  ").append(evLabel(ev));
            if (dro != DRO_AUTO) s.append("  DRO ").append(droLabel(dro));
            if (wbMode == 14) s.append("  ").append(kelvin).append('K');
            if (ab != 0) s.append("  ").append(ab > 0 ? "A" + ab : "B" + (-ab));
            if (gm != 0) s.append("  ").append(gm > 0 ? "G" + gm : "M" + (-gm));
            return s.toString();
        }
    }

    public static final int STD = 1, VIVID = 2, NEUTRAL = 3, PORTRAIT = 4, LANDSCAPE = 5, MONO = 6, CLEAR = 7, DEEP = 8, LIGHT = 9, SUNSET = 10, NIGHT = 11, AUTUMN = 12, SEPIA = 14;
    /** index = stored enum; value = runtime color-mode name (API); null = an enum value the camera uses for something we have not identified */
    public static final String[] STYLE_NAMES = { null, "standard", "vivid", "neutral", "portrait", "landscape", "mono", "clear", "deep", "light", "sunset", "night", "red-leaves", null, "sepia" };
    public static final String[] STYLE_LABEL = { "?", "Standard", "Vivid", "Neutral", "Portrait", "Landscape", "B&W", "Clear", "Deep", "Light", "Sunset", "Night", "Autumn", "?", "Sepia" };

    private static final int AUTO = 1, K = 14;

    /** Picture Effect: stored byte = index in the runtime list (verified) */
    public static final String[] PE_KEYS = { "off", "toy-camera", "pop-color", "posterization", "retro-photo", "soft-high-key", "part-color", "rough-mono", "soft-focus", "hdr-art", "richtone-mono", "miniature", "illust", "watercolor" };
    public static final String[] PE_LABEL = { "off", "Toy", "Pop", "Poster", "Retro", "High-key", "Part col", "HC mono", "Soft foc", "HDR art", "Rich mono", "Miniature", "Illust", "Watercol" };
    public static final int PE_OFF = 0, PE_TOY = 1, PE_POP = 2, PE_RETRO = 4, PE_HIGHKEY = 5, PE_HCMONO = 7;
    /** a style whose runtime name we know — the others are enum values seen in the store but never identified */
    public static boolean styleKnown(int v) { return v >= 1 && v < STYLE_NAMES.length && STYLE_NAMES[v] != null; }
    /**
     * Chip / HUD labels in the display language, with a "?n" fallback for an unidentified or out-of-table stored value.
     * STYLE_LABEL and PE_LABEL are the English; a translation keys them by the runtime name (see {@link Lang}).
     */
    public static String styleLabel(int v) { return styleKnown(v) ? Lang.label("style_" + Lang.slug(STYLE_NAMES[v]), STYLE_LABEL[v]) : "?" + v; }
    public static String peLabel(int v) { return v >= 0 && v < PE_LABEL.length ? Lang.label("effect_" + Lang.slug(PE_KEYS[v]), PE_LABEL[v]) : "?" + v; }
    /** effect sub-parameter (tint / tone / hue / mode): runtime key, stored slot, value names — index = stored byte */
    public static String subKey(int pe) { switch (pe) { case 5: return "pe-soft-high-key-effect"; case 1: return "pe-toy-camera-effect"; case 6: return "pe-part-color-effect"; case 3: return "pe-posterization-effect"; default: return null; } }
    public static int subId(int pe) { switch (pe) { case 5: return 0x010709d8; case 1: return 0x010706f3; case 6: return 0x010706ee; case 3: return 0x010706ef; default: return 0; } }
    public static String[] subValues(int pe) {
        switch (pe) {
            case 5: return new String[] { "blue", "pink", "green" };
            case 1: return new String[] { "normal", "cool", "warm", "green", "magenta" };
            case 6: return new String[] { "red", "green", "blue", "yellow" };
            case 3: return new String[] { "posterization-color", "posterization-bw" };
            default: return null;
        }
    }
    /**
     * The value the camera's runtime takes for an effect sub-parameter (the {@code pe-…-effect-values} list it reports):
     * the recipe's value, except B&W posterization, which recipe files spell {@code posterization-bw} and the camera
     * {@code posterization-mono}. The camera ignores a value not in its list, so before this B&W posterization shot in
     * colour. Null for an effect without a sub-parameter or an index out of range.
     */
    public static String subRuntime(int pe, int sub) {
        String[] v = subValues(pe);
        if (v == null || sub < 0 || sub >= v.length) return null;
        return v[sub].equals("posterization-bw") ? "posterization-mono" : v[sub];
    }
    public static String subLabel(int pe, int sub) {
        String[] v = subValues(pe);
        if (v == null) return null;
        return sub >= 0 && sub < v.length ? Lang.label("sub_" + Lang.slug(v[sub]), v[sub].replace("posterization-", "")) : "?" + sub;
    }
    /** DRO: 0 off, 1..5 level, 6 auto */
    public static final int DRO_OFF = 0, DRO_AUTO = 6;
    public static String droLabel(int v) { return v == DRO_AUTO ? Lang.t("value_auto") : v == 0 ? Lang.t("value_off") : Lang.t("value_level", v); }
    /** exposure bias in 1/3 EV steps -> "+0.7" */
    public static String evLabel(int ev) {
        if (ev == 0) return "0";
        int a = Math.abs(ev); String frac = a % 3 == 0 ? ".0" : a % 3 == 1 ? ".3" : ".7";
        return (ev > 0 ? "+" : "-") + (a / 3) + frac;
    }

    // ---- groups (brands) — recipes below MUST be listed in group order
    public static final String[] GROUPS = { "Sony", "Fuji", "Kodak", "Cine", "Ricoh GR", "Leica", "Hasselblad", "Canon / Nikon", "Pana / Olympus", "Other Stocks", "Ilford" };
    /** the group of a custom recipe: not a brand, so never an index into GROUPS */
    public static final int CUSTOM = -2;
    private static final int SONY = 0, FUJI = 1, KODAK = 2, CINE = 3, RICOH = 4, LEICA = 5, HASSEL = 6, CANIK = 7, PANOLY = 8, OTHER = 9, ILFORD = 10;

    public static final Recipe[] ALL = {
        // ---- Sony (Creative Looks from newer bodies — same pipeline, best fidelity)
        new Recipe(SONY,  "FACTORY (ST)",                        STD,      0,  0,  0, AUTO, 0,     0,  0),
        new Recipe(SONY,  "Sony NT (neutral)",                   NEUTRAL,  0,  0,  0, AUTO, 0,     0,  0),
        new Recipe(SONY,  "Sony PT (portrait)",                  PORTRAIT, 0,  0,  0, AUTO, 0,     0,  0),
        new Recipe(SONY,  "Sony SH (soft high-key)",             LIGHT,   -2, -2,  0, AUTO, 0,     1,  0,  5,  3, 6),
        new Recipe(SONY,  "Sony FL (film-like)",                 NEUTRAL, -3, -1,  0, AUTO, 0,     1,  0),
        new Recipe(SONY,  "Sony IN (instant)",                   NEUTRAL, -3, -3,  0, AUTO, 0,     0, -1),
        new Recipe(SONY,  "Sony VV (vivid)",                     VIVID,    0,  0,  0, AUTO, 0,     0,  0),
        new Recipe(SONY,  "Sony VV2",                            VIVID,    2,  1,  0, AUTO, 0,     0,  0),
        // ---- Fuji
        // Chân dung (Portrait)
        new Recipe(FUJI,  "Astia",                               PORTRAIT, 0, -1,  0, AUTO, 0,     1,  0,  0,  1, 6, pp(PP_MODE_STILL, +2,  +3, -2, -2, +1, +2, +2, -2, -2, -3)),
        new Recipe(FUJI,  "Pro Neg Std",                         PORTRAIT,-2, -1,  0, AUTO, 0,     0,  0,            pp(PP_MODE_CINE,  +3,  -5, -2, -2,  0, +1, +1, -2, -2, -4)),
        new Recipe(FUJI,  "Pro Neg Hi",                          PORTRAIT,-2,  1,  0, AUTO, 0,     0,  0,            pp(PP_MODE_STILL, -2,  -3, -2, -1, +2, +2, +1, -2, -2, -3)),
        new Recipe(FUJI,  "Fuji Pro 400H",                       LIGHT,   -2, -2,  0, AUTO, 0,    -1,  1,  0,  2, 6, pp(PP_MODE_CINE,  +5,  -3, -1, -2, +2, -1, -3, -2, -3, -4)),
        // Đời thường (Street)
        new Recipe(FUJI,  "Classic Chrome",                      STD,     -1,  2,  0, AUTO, 0,     0,  0,  0,  0, 6, pp(PP_MODE_CINE,  -3,  -8,  0, +2,  0, +4, +2, -2, +1, -3)),
        new Recipe(FUJI,  "Classic Negative",                    STD,     -3,  3,  1, AUTO, 0,    -1,  1,  0,  0, 6, pp(PP_MODE_CINE,  +4,  -4, -2, +4, +5, -2, +3, -3, -2, -4)),
        new Recipe(FUJI,  "Nostalgic Neg",                       STD,      1,  0,  0, AUTO, 0,     3,  0,  0,  0, 6, pp(PP_MODE_CINE,  +3,  +5, -3, +3, +2, -2, -2, +1, +4, -4)),
        new Recipe(FUJI,  "Fuji Superia 400",                    STD,      1,  1,  0, AUTO, 0,     1,  1,  0,  1, 6, pp(PP_MODE_STILL, +2,  +4, -2, +2, +5, +2, +2, +3, -1, -4)),
        new Recipe(FUJI,  "Fuji C200",                           STD,      0,  0,  0, AUTO, 0,    -1,  1,            pp(PP_MODE_STILL, +2,  +2, -1, +2, +3, +1, +2,  0, -1, -3)),
        // Điện ảnh (Cinema)
        new Recipe(FUJI,  "Eterna",                              NEUTRAL, -3, -2, -1, AUTO, 0,     0,  0,  0, -1, 3, pp(PP_MODE_CINE,  +5, -12, +1, +1, +3, +2, +3, -2, -1, -5)),
        new Recipe(FUJI,  "Eterna Bleach Bypass",                NEUTRAL, -3,  3,  0, AUTO, 0,     0,  0,  0, -1, DRO_OFF, pp(PP_MODE_CINE, -6, -22, +2, +3, +4, +3, +2, 0, +2, -2)),
        new Recipe(FUJI,  "Fuji Natura 1600",                    PORTRAIT,-2, -2,  0, AUTO, 0,     1,  0,  0,  1, 6, pp(PP_MODE_CINE,  +6,  -4, -3, +3, +5, +1, +2, +2, -3, -6)),
        // Phong cảnh (Landscape)
        new Recipe(FUJI,  "Provia",                              STD,      1,  0,  0, AUTO, 0,     0,  0,  0,  0, 6, pp(PP_MODE_STILL, -1,  +4, -1, +1, +1, +2, +1,  0, -1, -2)),
        new Recipe(FUJI,  "Velvia",                              VIVID,    3,  2,  0, AUTO, 0,     0,  0,            pp(PP_MODE_STILL, -5, +14, -1, +4, +4, +5, +3, +2, +2, -1)),
        new Recipe(FUJI,  "Reala Ace",                           STD,      0,  1,  0, AUTO, 0,     0,  0,            pp(PP_MODE_STILL, -2,  +2, -1, +1, +2, +2, +1, -1, -1, -2)),
        new Recipe(FUJI,  "Fuji Fortia 50",                      VIVID,    3,  2,  0, AUTO, 0,    -1,  0,  0, -1, 6, pp(PP_MODE_STILL, -4, +16, -2, +5, +3, +5, +2, +4, +3, -1)),
        // Trắng đen (Monochrome)
        new Recipe(FUJI,  "Acros",                               MONO,     0,  1,  1, AUTO, 0,     0,  0),
        new Recipe(FUJI,  "Acros +Ye (yellow filter)",           MONO,     0,  1,  1, K,    4000,  0,  0),
        new Recipe(FUJI,  "Acros +R (red filter)",               MONO,     0,  0,  0, K,    2500,  0,  0,  7,  0, 6),
        new Recipe(FUJI,  "Acros +G (green filter)",             MONO,     0,  1,  1, K,    5600,  0,  4),
        new Recipe(FUJI,  "Sepia",                               SEPIA,    0,  0,  0, AUTO, 0,     0,  0),
        // ---- Kodak
        new Recipe(KODAK, "Kodak Portra 160",                    PORTRAIT,-3, -2,  0, AUTO, 0,     1, -1,  0,  2, 6, pp(PP_MODE_CINE,  +4,  -4, -2, -2, +1, +1, -1, -2, -3, -4)),   // warm pastel with a touch of magenta
        new Recipe(KODAK, "Kodak Portra 400",                    PORTRAIT,-1, -1,  0, AUTO, 0,     3,  1,  0,  2, 6, pp(PP_MODE_CINE,  +3,  +4, +1, +2, +1, +2, -2, +1, -1, -3)),   // golden, soft
        new Recipe(KODAK, "Kodak Portra 800",                    PORTRAIT,-2, -2,  0, AUTO, 0,     1,  1,  0,  1, 6, pp(PP_MODE_CINE,  +6,  +5, -3, -1, +2, +3, -1, -1, -3, -5)),
        new Recipe(KODAK, "Kodak Gold 200",                      STD,      2,  1,  0, AUTO, 0,     3,  1,  0,  1, 6, pp(PP_MODE_STILL, +2,  +8, -3, +2, +2, +2,  0, +2, +3, -2)),
        new Recipe(KODAK, "Kodak Ultra Max 400",                 STD,      1,  1,  0, AUTO, 0,     1,  0,  0,  0, 6, pp(PP_MODE_STILL, -2,  +4, -2, +3, +3, +4, +2, +1, +1, -2)),
        new Recipe(KODAK, "Kodak Color Plus 200",                STD,      0,  0,  0, AUTO, 0,     2,  0,  0,  0, 6, pp(PP_MODE_STILL, +1,  +2, -2, +2, +1, +2,  0,  0, +1, -2)),
        new Recipe(KODAK, "Kodak Ektar 100",                     VIVID,    1,  1,  0, AUTO, 0,     0,  0,            pp(PP_MODE_STILL, -3, +10, -2, +4, +3, +5, +2, +3, +3, -1)),
        new Recipe(KODAK, "Kodak Ektachrome E100",               VIVID,    1,  2,  0, AUTO, 0,    -1, -1,            pp(PP_MODE_STILL, -4,  +8, -1, +1, +3, +4, +2,  0, +1, -1)),
        new Recipe(KODAK, "Kodachrome 64",                       STD,     -1,  2,  0, AUTO, 0,     2, -1,            pp(PP_MODE_STILL, -1,  +2, -3, +3, +2, +2,  0, -1, +2, -2)),
        new Recipe(KODAK, "Kodak Vision3 500T (daylight)",       NEUTRAL, -1,  0,  0, K,    3200,  0,  0,  0,  1, 3),
        new Recipe(KODAK, "Kodak Vision 200T (Asteroid City)",   NEUTRAL, -2, -3,  0, K,    5000,  2,  3,  0,  1, 3, pp(PP_MODE_CINE,  +5,  +2, -3, +3, +2, -1, -2, +1, +3, -2)),   // warm pastel yellow-green wash
        new Recipe(KODAK, "Kodak Tri-X 400",                     MONO,     0,  2,  2, AUTO, 0,     0,  0,  0,  1, 6),
        new Recipe(KODAK, "Kodak T-Max",                         MONO,     0,  2,  3, AUTO, 0,     0,  0,  0,  0, 6),
        new Recipe(KODAK, "Kodak Tri-X 1600 (pushed)",           MONO,     0,  3, -1, AUTO, 0,     0,  0,  0,  2, 6),
        // ---- Cinema / Video
        new Recipe(CINE,  "Cinestill 50D (Blue Velvet)",         STD,     -2, -1,  0, K,    5500, -1, -1),   // faded, near-neutral, a touch cool and magenta
        new Recipe(CINE,  "Cinestill 800T",                      NEUTRAL,  0,  0,  0, K,    3200,  0, -1,  0,  1, 6, pp(PP_MODE_CINE,  +5,  +4, +2, +4, +2, +4, +5, +1, -2, -6)),   // tungsten, halation glow
        new Recipe(CINE,  "Classic Cinema",                      STD,      0, -1,  0, K,    6000,  2,  0,  0,  0, 3, pp(PP_MODE_CINE,  +2,  +3, -2, +3, +3, +4, +4,  0, +2, -4)),
        new Recipe(CINE,  "Rec709 Video (flat-ish)",             NEUTRAL, -2, -2,  0, AUTO, 0,     0,  0,  0,  0, 5, pp(PP_MODE_CINE,  +5, -10,  0,  0,  0,  0,  0,  0,  0, -4)),
        // ---- Ricoh GR
        new Recipe(RICOH, "GR Positive Film",                    STD,      3,  2,  0, AUTO, 0,     2,  0,  0, -1, 6, pp(PP_MODE_STILL, -5, +10, -2, +4, +3, +4, +2, +2, +1, -2)),
        new Recipe(RICOH, "GR Negative Film",                    NEUTRAL, -2,  1,  0, AUTO, 0,    -1,  1,  0,  1, 6, pp(PP_MODE_CINE,  +4,  -4, -1, +1, +2, +1, +1, -2, -2, -3)),
        new Recipe(RICOH, "GR Bleach Bypass",                    NEUTRAL, -3,  3,  1, AUTO, 0,     0,  0,  0,  0, DRO_OFF, pp(PP_MODE_CINE, -6, -20, +1, +3, +3, +3, +2, 0, +1, -1)),   // the store keeps sat -3 at most, so DRO off carries the rest
        new Recipe(RICOH, "GR Retro",                            STD,     -3, -1,  0, AUTO, 0,     3, -1,  4,  0, 6),
        new Recipe(RICOH, "GR Cross Process",                    VIVID,    2,  2,  0, AUTO, 0,    -2,  4,            pp(PP_MODE_STILL, -4, +12, +4, +3, +5, +4, -3, +3, -3, -2)),
        new Recipe(RICOH, "GR Hi-Contrast B&W",                  MONO,     0,  3,  1, AUTO, 0,     0,  0,  7,  0, 6),
        new Recipe(RICOH, "GR Hard Monotone",                    MONO,     0,  2,  2, AUTO, 0,     0,  0),
        new Recipe(RICOH, "GR Soft Monotone",                    MONO,     0, -2, -1, AUTO, 0,     0,  0),
        // ---- Leica
        new Recipe(LEICA, "Leica Contemporary",                  STD,      1,  1,  0, AUTO, 0,     0,  0,            pp(PP_MODE_STILL, -2,  +2, -1, +2, +2, +3, +1,  0, -1, -2)),
        new Recipe(LEICA, "Leica Classic",                       STD,     -1,  2,  0, AUTO, 0,     1,  0,            pp(PP_MODE_CINE,  -5,  -4, -2, +4, +3, +4, +2, +1, +2, -3)),
        new Recipe(LEICA, "Leica Eternal",                       NEUTRAL, -3, -1,  0, AUTO, 0,     1,  0,  0,  0, 3, pp(PP_MODE_CINE,  -4,  +8, -2, +5, +4, +5, +3, +2, +2, -2)),
        new Recipe(LEICA, "Leica Monochrom",                     MONO,     0,  2,  1, AUTO, 0,     0,  0),
        // ---- Hasselblad
        new Recipe(HASSEL,"Hasselblad HNCS Natural",             NEUTRAL, -1, -1,  0, AUTO, 0,     0,  0,            pp(PP_MODE_STILL, +1,   0, -2, -1, +1, +1, +1, -2, -2, -3)),
        // ---- Canon / Nikon
        new Recipe(CANIK, "Canon Portrait",                      PORTRAIT, 0,  0, -1, AUTO, 0,     0, -1,            pp(PP_MODE_STILL, +2,  +3, -1, +2,  0, +1, -1, +2, +1, -3)),
        new Recipe(CANIK, "Canon Faithful",                      NEUTRAL,  0,  0,  0, AUTO, 0,     0,  0,            pp(PP_MODE_STILL,  0,   0, -1,  0,  0, +1,  0, -1, -1, -2)),
        new Recipe(CANIK, "Canon Standard",                      STD,      1,  1,  0, AUTO, 0,     1, -1,            pp(PP_MODE_STILL, -1,  +5, -2, +1,  0, +1,  0, -2, -2, -2)),
        new Recipe(CANIK, "Nikon Flat",                          NEUTRAL, -3, -3, -1, AUTO, 0,     0,  0,  0,  0, 5, pp(PP_MODE_CINE,  +8, -12,  0, -1, -1, -1, -1, -1, -1, -5)),
        new Recipe(CANIK, "Nikon Vivid",                         VIVID,    1,  1,  1, AUTO, 0,     0,  0,            pp(PP_MODE_STILL, -3, +10,  0, +2, +3, +3, +2, +1, +1, -1)),
        // ---- Panasonic / Olympus
        new Recipe(PANOLY,"Pana L.ClassicNeo",                   NEUTRAL, -3, -1,  0, AUTO, 0,     2,  0,  0,  1, 6, pp(PP_MODE_CINE,  +4,  -6, -1, +2, +2, +3, +3, -1, -2, -4)),
        new Recipe(PANOLY,"Olympus Pop Art",                     VIVID,    3,  3,  0, AUTO, 0,     0,  0,            pp(PP_MODE_STILL, -4, +18, -1, +4, +4, +5, +3, +3, +2, -1)),
        new Recipe(PANOLY,"Olympus Pale & Light",                LIGHT,   -3, -2,  0, AUTO, 0,     0,  0,  0,  2, 6, pp(PP_MODE_CINE,  +7, -10, -2, -3, -2, -2, -3, -3, -3, -4)),
        new Recipe(PANOLY,"Pana L.Monochrome D",                 MONO,     0,  3,  1, AUTO, 0,     0,  0),
        // ---- Other stocks
        new Recipe(OTHER, "Agfa Vista 200",                      STD,      2,  1,  0, AUTO, 0,     2, -1,  0,  1, 6, pp(PP_MODE_STILL, +1,  +8, -2, +4, +3, +2, +1,  0, -2, -3)),
        new Recipe(OTHER, "Agfa Ultra 100",                      VIVID,    3,  1,  0, AUTO, 0,     0,  0,            pp(PP_MODE_STILL, -3, +16, -2, +5, +4, +5, +3, +2, +2, -2)),
        new Recipe(OTHER, "Polaroid / Instax",                   STD,     -3, -2,  0, AUTO, 0,     1, -2,  4,  1, 6),
        // ---- Ilford
        new Recipe(ILFORD,"Ilford Pan F 50",                     MONO,     0,  2,  2, AUTO, 0,     0,  0),
        new Recipe(ILFORD,"Ilford FP4",                          MONO,     0,  1,  1, AUTO, 0,     0,  0),
        new Recipe(ILFORD,"Ilford Delta 100",                    MONO,     0,  1,  1, AUTO, 0,     0,  0),
        new Recipe(ILFORD,"Ilford HP5",                          MONO,     0,  1,  0, AUTO, 0,     0,  0,  0,  1, 6),
        new Recipe(ILFORD,"Ilford Delta 3200",                   MONO,     0,  3, -2, AUTO, 0,     0,  0,  0,  2, 6),
    };

    public static final int FACTORY = 0;
    /** how many recipes the wheel and the brand list walk: the whole table */
    public static final int LISTED = ALL.length;

    /** first recipe index of each group, and how many it lists */
    public static final int[] GROUP_START = new int[GROUPS.length];
    public static final int[] GROUP_COUNT = new int[GROUPS.length];
    static {
        for (int g = 0; g < GROUPS.length; g++) GROUP_START[g] = -1;
        for (int i = 0; i < ALL.length; i++) {
            int g = ALL[i].group;
            if (GROUP_START[g] < 0) GROUP_START[g] = i;
            GROUP_COUNT[g]++;
        }
    }

    // ---- navigation: every step wraps over the list, dir is +1 / -1
    /** the recipe after / before i over the whole list */
    public static int next(int i, int dir) { return (i + LISTED + dir) % LISTED; }
    /** the first recipe of the brand after / before i's */
    public static int nextGroupStart(int i, int dir) { return GROUP_START[(ALL[i].group + GROUPS.length + dir) % GROUPS.length]; }
    /** the recipe after / before i within its brand */
    public static int nextInGroup(int i, int dir) {
        int g = ALL[i].group, start = GROUP_START[g], n = GROUP_COUNT[g];
        return start + ((i - start + n + dir) % n);
    }

    /** the first recipe of the next / previous category within the brand group of {@code i} */
    public static int nextCategoryInGroup(int i, int dir) {
        if (i < 0 || i >= LISTED) return i;
        int g = ALL[i].group, start = GROUP_START[g], n = GROUP_COUNT[g];
        if (n <= 1) return i;
        int curCat = categoryCode(ALL[i]);
        if (dir > 0) {
            for (int step = 1; step < n; step++) {
                int target = start + ((i - start + step) % n);
                if (categoryCode(ALL[target]) != curCat) return target;
            }
        } else {
            int curStart = i;
            while (curStart > start && categoryCode(ALL[curStart - 1]) == curCat) {
                curStart--;
            }
            if (i > curStart) return curStart;
            for (int step = 1; step < n; step++) {
                int target = start + ((i - start - step + n) % n);
                if (categoryCode(ALL[target]) != curCat) {
                    int prevCat = categoryCode(ALL[target]);
                    while (target > start && categoryCode(ALL[target - 1]) == prevCat) {
                        target--;
                    }
                    return target;
                }
            }
        }
        return i;
    }

    /** the first recipe of the next / previous category across all built-in recipes */
    public static int nextCategoryInAll(int i, int dir) {
        if (i < 0 || i >= LISTED) return i;
        int curCat = categoryCode(ALL[i]);
        if (dir > 0) {
            for (int step = 1; step < LISTED; step++) {
                int target = (i + step) % LISTED;
                if (categoryCode(ALL[target]) != curCat) return target;
            }
        } else {
            int curStart = i;
            while (curStart > 0 && categoryCode(ALL[curStart - 1]) == curCat) {
                curStart--;
            }
            if (i > curStart) return curStart;
            for (int step = 1; step < LISTED; step++) {
                int target = (i - step + LISTED) % LISTED;
                if (categoryCode(ALL[target]) != curCat) {
                    int prevCat = categoryCode(ALL[target]);
                    while (target > 0 && categoryCode(ALL[target - 1]) == prevCat) {
                        target--;
                    }
                    return target;
                }
            }
        }
        return i;
    }

    /** where a recipe sits in the list, as the panel shows it: "12 / 77" */
    public static String position(int i) { return (i + 1) + " / " + LISTED; }

    // ---- names on screen: the display language's, where it has one. The canonical English above stays the identifier
    // that favorites, the sample manifest and the tests use, so a change of language never loses a mark.
    /** a recipe's name as the panel and the list show it; a custom recipe's is the user's own, never translated */
    public static String displayName(Recipe r) { return r.isCustom() ? r.name : Lang.label("recipe_" + Lang.slug(r.name), r.name); }
    /** the canonical name, shown small under a translated one; null when the display name is the canonical one */
    public static String originalName(Recipe r) { String d = displayName(r); return d.equals(r.name) ? null : r.name; }
    /** a brand as the list shows it, or "Custom" for a custom recipe */
    public static String groupLabel(int g) { return g == CUSTOM ? Lang.t("group_custom") : Lang.label("group_" + Lang.slug(GROUPS[g]), GROUPS[g]); }

    // ---- categories (shooting purpose)
    public static final int
        CAT_PORTRAIT = 1,
        CAT_STREET = 2,
        CAT_CINE = 3,
        CAT_LANDSCAPE = 4,
        CAT_MONO = 5,
        CAT_STANDARD = 6,
        CAT_VINTAGE = 7,
        CAT_ART = 8,
        CAT_NATURAL = 9;

    public static int categoryCode(Recipe r) {
        if (r == null || r.isCustom()) return 0;
        return categoryCode(r.name);
    }

    public static int categoryCode(String name) {
        if (name == null) return 0;
        if (name.equals("Sony PT (portrait)") || name.equals("Sony SH (soft high-key)") ||
            name.equals("Astia") || name.equals("Pro Neg Std") || name.equals("Pro Neg Hi") || name.equals("Fuji Pro 400H") ||
            name.equals("Kodak Portra 160") || name.equals("Kodak Portra 400") || name.equals("Kodak Portra 800") ||
            name.equals("Canon Portrait") || name.equals("Canon Faithful")) {
            return CAT_PORTRAIT;
        }
        if (name.equals("Sony FL (film-like)") || name.equals("Sony IN (instant)") ||
            name.equals("Classic Chrome") || name.equals("Classic Negative") || name.equals("Nostalgic Neg") ||
            name.equals("Fuji Superia 400") || name.equals("Fuji C200") ||
            name.equals("Kodak Gold 200") || name.equals("Kodak Ultra Max 400") || name.equals("Kodak Color Plus 200") ||
            name.equals("Canon Standard") || name.equals("Nikon Flat") ||
            name.equals("Pana L.ClassicNeo") ||
            (name.startsWith("GR ") && !name.contains("B&W") && !name.contains("Monotone")) ||
            name.startsWith("Leica C") || name.equals("Leica Eternal")) {
            return CAT_STREET;
        }
        if (name.equals("Eterna") || name.equals("Eterna Bleach Bypass") || name.equals("Fuji Natura 1600") ||
            name.startsWith("Kodak Vision") ||
            name.startsWith("Cinestill") || name.equals("Classic Cinema") || name.equals("Rec709 Video (flat-ish)")) {
            return CAT_CINE;
        }
        if (name.equals("Sony VV (vivid)") || name.equals("Sony VV2") ||
            name.equals("Provia") || name.equals("Velvia") || name.equals("Reala Ace") || name.equals("Fuji Fortia 50") ||
            name.equals("Kodak Ektar 100") || name.equals("Kodak Ektachrome E100") || name.equals("Kodachrome 64") ||
            name.equals("Nikon Vivid")) {
            return CAT_LANDSCAPE;
        }
        if (name.startsWith("Acros") || name.equals("Sepia") ||
            name.startsWith("Kodak Tri-X") || name.equals("Kodak T-Max") ||
            name.contains("B&W") || name.contains("Monotone") || name.equals("Leica Monochrom") ||
            name.equals("Pana L.Monochrome D") ||
            name.startsWith("Ilford")) {
            return CAT_MONO;
        }
        if (name.startsWith("Olympus")) {
            return CAT_ART;
        }
        if (name.startsWith("Agfa") || name.equals("Polaroid / Instax")) {
            return CAT_VINTAGE;
        }
        if (name.startsWith("Hasselblad")) {
            return CAT_NATURAL;
        }
        return CAT_STANDARD;
    }

    public static String category(Recipe r) {
        int code = categoryCode(r);
        switch (code) {
            case CAT_PORTRAIT: return Lang.t("cat_portrait");
            case CAT_STREET: return Lang.t("cat_street");
            case CAT_CINE: return Lang.t("cat_cine");
            case CAT_LANDSCAPE: return Lang.t("cat_landscape");
            case CAT_MONO: return Lang.t("cat_mono");
            case CAT_ART: return Lang.t("cat_art");
            case CAT_VINTAGE: return Lang.t("cat_vintage");
            case CAT_NATURAL: return Lang.t("cat_natural");
            default: return Lang.t("cat_standard");
        }
    }
}
