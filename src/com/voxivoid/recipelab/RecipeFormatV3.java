package com.voxivoid.recipelab;

import java.util.HashMap;
import java.util.Map;

/** PP files use format 3 so old readers reject the whole look instead of silently dropping PP. */
final class RecipeFormatV3 implements RecipeFormat {
    static final String FAMILY = "sony-a6300-picture-profile";
    private static final RecipeFormatV2 V2 = new RecipeFormatV2();

    public int version() { return 3; }

    public String write(Recipes.Recipe r, String madeOn) {
        String text = V2.write(r, madeOn).replace("\nformat: 2\n", "\nformat: 3\n");
        if (r.ppProfile == null) return text;
        String problem = CustomRecipes.problem(r);
        if (problem != null) throw new IllegalArgumentException(problem);
        text = text.replace("family: " + RecipeFormatV2.FAMILY, "family: " + FAMILY);
        StringBuilder out = new StringBuilder(text);
        int[] v = PictureProfiles.values(r.ppProfile);
        for (int i = 0; i < v.length; i++) out.append(PictureProfiles.KEYS[i]).append(": ").append(v[i]).append('\n');
        return out.toString();
    }

    public CustomRecipes.Parsed read(Map<String, String> keys, String fileName) {
        if (!FAMILY.equals(keys.get("family"))) {
            for (String k : PictureProfiles.KEYS)
                if (keys.containsKey(k)) return CustomRecipes.Parsed.fail(CustomRecipes.bad("family", keys.get("family")));
            return V2.read(keys, fileName);
        }
        Map<String, String> base = new HashMap<String, String>(keys);
        base.put("family", RecipeFormatV2.FAMILY);
        CustomRecipes.Parsed parsed = V2.read(base, fileName);
        if (parsed.recipe == null) return parsed;
        int[] v = new int[PictureProfiles.KEYS.length];
        for (int i = 0; i < v.length; i++) {
            String key = PictureProfiles.KEYS[i], raw = keys.get(key);
            Integer n = raw == null ? null : CustomRecipes.integer(raw);
            if (n == null || n < PictureProfiles.MIN[i] || n > PictureProfiles.MAX[i])
                return CustomRecipes.Parsed.fail(CustomRecipes.bad(key, raw));
            v[i] = n;
        }
        Recipes.Recipe r = PictureProfiles.attach(parsed.recipe, PictureProfiles.from(v));
        String problem = CustomRecipes.problem(r);
        return problem == null ? CustomRecipes.Parsed.ok(r) : CustomRecipes.Parsed.fail(problem);
    }
}
