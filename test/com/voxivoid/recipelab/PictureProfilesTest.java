package com.voxivoid.recipelab;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.voxivoid.recipelab.Params.*;

class PictureProfilesTest {
    @Test void filesAndQrRetainEveryProfileValueForEveryBuiltIn() {
        for (Recipes.Recipe builtIn : Recipes.ALL) {
            if (builtIn.ppProfile == null) continue;
            Recipes.Recipe r = CustomRecipes.renamed(builtIn, "Profile");
            String file = RecipeFormats.write(r, "ILCE-6300");
            assertTrue(file.contains("\nformat: 3\n"));
            assertTrue(file.contains(RecipeFormatV3.FAMILY));
            CustomRecipes.Parsed parsed = RecipeFormats.parse(file, "PROBE.YML");
            assertNull(parsed.error, r.name);
            assertArrayEquals(PictureProfiles.values(r.ppProfile), PictureProfiles.values(parsed.recipe.ppProfile), r.name);
            Recipes.Recipe shared = RecipeShare.decode(RecipeShare.encode(r));
            assertNotNull(shared, r.name);
            assertArrayEquals(PictureProfiles.values(r.ppProfile), PictureProfiles.values(shared.ppProfile), r.name);
            assertSame(r.ppProfile, CustomRecipes.renamed(r, "Renamed").ppProfile);
            assertNotNull(RecipeFormats.parse(file, "PROBE.YML", new RecipeFormat[] { new RecipeFormatV1(), new RecipeFormatV2() }).error);
        }
    }

    @Test void allSignedLimitsAndEverySlotSurviveRoundTrips() {
        for (int slot = 1; slot <= 9; slot++) {
            for (int edge = 0; edge < 2; edge++) {
                int[] v = (edge == 0 ? PictureProfiles.MIN : PictureProfiles.MAX).clone(); v[0] = slot;
                Recipes.Recipe r = PictureProfiles.attach(Fixtures.recipe("Canon Portrait"), PictureProfiles.from(v));
                assertArrayEquals(v, PictureProfiles.values(RecipeShare.decode(RecipeShare.encode(r)).ppProfile));
                assertArrayEquals(v, PictureProfiles.values(RecipeFormats.parse(RecipeFormats.write(r, null), "A.YML").recipe.ppProfile));
            }
        }
    }

    @Test void partialOrOutOfRangeProfilesCannotBeApplied() {
        Recipes.Recipe r = Fixtures.recipe("Canon Portrait");
        String file = RecipeFormats.write(r, null);
        int[] valid = PictureProfiles.values(r.ppProfile);
        for (int i = 0; i < valid.length; i++) {
            String key = PictureProfiles.KEYS[i];
            String entry = key + ": " + valid[i] + "\n";
            assertNotNull(RecipeFormats.parse(file.replace(entry, ""), "A.YML").error, key);
            assertNotNull(RecipeFormats.parse(file.replace(entry, key + ": " + (PictureProfiles.MAX[i] + 1) + "\n"), "A.YML").error, key);
            int[] code = RecipeShare.unBase64Url(RecipeShare.encode(r).substring(RecipeShare.PREFIX.length()));
            code[13 + i] = (PictureProfiles.MAX[i] + 1) & 255;
            byte[] bytes = new byte[code.length];
            for (int k = 0; k < bytes.length; k++) bytes[k] = (byte) code[k];
            assertNull(RecipeShare.decodeCode(RecipeShare.base64Url(bytes)), key);
        }
        assertNotNull(RecipeFormats.parse(file.replace(RecipeFormatV3.FAMILY, RecipeFormatV2.FAMILY), "A.YML").error);
        assertNotNull(RecipeFormats.parse(file.replace("effect: off", "effect: retro-photo"), "A.YML").error);
    }

    @Test void captureRenameSaveAndReapplyRetainsTheFullLook() {
        Recipes.Recipe source = Fixtures.recipe("Canon Portrait");
        int[] rows = Fixtures.staged(source, Fixtures.factoryRows(), Q_FINE); rows[R_PP] = 7;
        Recipes.Recipe custom = CustomRecipes.recipe("My Portrait", rows, source.ppProfile);
        Recipes.Recipe renamed = CustomRecipes.renamed(custom, "My Skin");
        Recipes.Recipe read = RecipeFormats.parse(RecipeFormats.write(renamed, "ILCE-6300"), "MYSKIN.YML").recipe;
        assertNotNull(read);
        assertEquals(7, read.ppProfile.pp);
        Map<Integer, Integer> store = Fixtures.factoryStore();
        Fixtures.apply(store, recipeWrites(Fixtures.factoryRows(), rows, 0, read));
        assertEquals(7, Fixtures.byteAt(store, ID_PP_NO));
        for (Write w : ppWrites(7, source.ppProfile)) assertEquals((byte) w.value, Fixtures.byteAt(store, w.id));
        assertThrows(IllegalArgumentException.class, () -> CustomRecipes.recipe("Missing PP", rows, null));
        assertThrows(IllegalArgumentException.class, () -> CustomRecipes.recipe("Missing PP", rows));
        assertSame(ppSignature(source, 7), ppSignature(source, 7), "redraws reuse the cached key");
    }

    @Test void nonPpFilesKeepTheirLegacyFormat() {
        Recipes.Recipe r = Fixtures.recipe("FACTORY (ST)");
        assertTrue(RecipeFormats.write(r, null).contains("\nformat: 2\n"));
        assertNull(RecipeShare.decode(RecipeShare.encode(r)).ppProfile);
    }
}
