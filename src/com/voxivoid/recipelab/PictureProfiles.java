package com.voxivoid.recipelab;

/** The twelve PP values shared by the file format, QR format and camera read-back. */
final class PictureProfiles {
    private PictureProfiles() {}

    static final String[] KEYS = { "pp-slot", "pp-mode", "pp-black", "pp-saturation", "pp-phase",
            "pp-depth-r", "pp-depth-g", "pp-depth-b", "pp-depth-c", "pp-depth-m", "pp-depth-y", "pp-detail" };
    static final int[] MIN = { 1, 0, -15, -32, -7, -7, -7, -7, -7, -7, -7, -7 };
    static final int[] MAX = { 9, 15, 15, 32, 7, 7, 7, 7, 7, 7, 7, 7 };

    static int[] values(Recipes.PpProfile p) {
        return new int[] { p.pp, p.mode, p.black, p.sat, p.phase, p.r, p.g, p.b, p.c, p.m, p.y, p.detail };
    }

    static Recipes.PpProfile from(int[] v) {
        if (v == null || v.length != KEYS.length) throw new IllegalArgumentException("incomplete Picture Profile");
        for (int i = 0; i < v.length; i++)
            if (v[i] < MIN[i] || v[i] > MAX[i]) throw new IllegalArgumentException(KEYS[i] + ": " + v[i]);
        return new Recipes.PpProfile(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11]);
    }

    static Recipes.PpProfile atSlot(Recipes.PpProfile p, int pp) {
        int[] v = values(p); v[0] = pp;
        return from(v);
    }

    static Recipes.Recipe attach(Recipes.Recipe r, Recipes.PpProfile p) {
        return new Recipes.Recipe(r.group, r.name, r.style, r.sat, r.con, r.sharp,
                r.wbMode, r.kelvin, r.ab, r.gm, r.pe, r.ev, r.dro, r.sub, p);
    }
}
