package com.poopycobblemon.cobblemonext.species;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ExtSpeciesBuilder 产出的 JSON 结构契约（纯 JVM，无 Minecraft 类路径） */
class ExtSpeciesBuilderTest {

    private static JsonObject parse(SpeciesDefinition definition) {
        return JsonParser.parseString(definition.json()).getAsJsonObject();
    }

    @Test
    void buildsCompleteSpeciesJsonWithDefaults() {
        SpeciesDefinition def = ExtSpeciesBuilder.create("poopy_cobblemon", "dungemon")
                .dexNumber(1026)
                .primaryType("poison").secondaryType("dark")
                .baseStats(60, 80, 50, 90, 60, 100)
                .abilities("stench", "innerfocus")
                .hiddenAbility("sticky_hold")
                .move("1:tackle", "9:poison_gas", "egg:scary_face", "tm:bodyslam")
                .dexMetrics(7, 80)
                .evYield("special_attack", 2)
                .pokedex("cobblemon.species.dungemon.desc")
                .build();

        assertEquals("poopy_cobblemon", def.namespace());
        assertEquals("dungemon.json", def.fileName());
        assertEquals("data/poopy_cobblemon/species/dungemon.json", def.resourcePath());

        JsonObject root = parse(def);
        assertTrue(root.get("implemented").getAsBoolean());
        assertEquals(1026, root.get("nationalPokedexNumber").getAsInt());
        assertEquals("Dungemon", root.get("name").getAsString());
        assertEquals("poison", root.get("primaryType").getAsString());
        assertEquals("dark", root.get("secondaryType").getAsString());
        assertEquals(0.5, root.get("maleRatio").getAsDouble(), 1e-9);
        assertEquals(7, root.get("height").getAsInt());
        assertEquals(80, root.get("weight").getAsInt());

        JsonArray abilities = root.getAsJsonArray("abilities");
        assertEquals(List.of("stench", "innerfocus", "h:sticky_hold"),
                List.of(abilities.get(0).getAsString(), abilities.get(1).getAsString(),
                        abilities.get(2).getAsString()));

        assertEquals(List.of("undiscovered"), jsonList(root.getAsJsonArray("eggGroups")));
        assertEquals(List.of("custom"), jsonList(root.getAsJsonArray("labels")));

        JsonObject stats = root.getAsJsonObject("baseStats");
        assertEquals(60, stats.get("hp").getAsInt());
        assertEquals(80, stats.get("attack").getAsInt());
        assertEquals(50, stats.get("defence").getAsInt());
        assertEquals(90, stats.get("special_attack").getAsInt());
        assertEquals(60, stats.get("special_defence").getAsInt());
        assertEquals(100, stats.get("speed").getAsInt());

        assertEquals(2, root.getAsJsonObject("evYield").get("special_attack").getAsInt());
        assertEquals("medium_fast", root.get("experienceGroup").getAsString());
        assertEquals(45, root.get("catchRate").getAsInt());
        assertEquals(50, root.get("baseFriendship").getAsInt());
        assertEquals(1.0, root.get("baseScale").getAsDouble(), 1e-9);

        JsonObject hitbox = root.getAsJsonObject("hitbox");
        assertEquals(0.8, hitbox.get("width").getAsDouble(), 1e-9);
        assertEquals(0.8, hitbox.get("height").getAsDouble(), 1e-9);
        assertFalse(hitbox.get("fixed").getAsBoolean());

        assertEquals(List.of("1:tackle", "9:poison_gas", "egg:scary_face", "tm:bodyslam"),
                jsonList(root.getAsJsonArray("moves")));
    }

    @Test
    void secondaryTypeOmittedWhenAbsent() {
        SpeciesDefinition def = minimal("plainmon").build();
        JsonObject root = parse(def);
        assertFalse(root.has("secondaryType"));
    }

    @Test
    void customFieldsPassThroughToTopLevel() {
        SpeciesDefinition def = minimal("weirdmon")
                .custom("poopsky:poop_type", "toxic")
                .custom("poopsky:yields", 3)
                .custom("poopsky:nested", java.util.Map.of("a", java.util.List.of(1, 2)))
                .build();
        JsonObject root = parse(def);
        assertEquals("toxic", root.get("poopsky:poop_type").getAsString());
        assertEquals(3, root.get("poopsky:yields").getAsInt());
        JsonObject nested = root.getAsJsonObject("poopsky:nested");
        assertEquals(List.of("1", "2"), jsonList(nested.getAsJsonArray("a")));
    }

    @Test
    void stringsAreEscapedAndRoundTrip() {
        SpeciesDefinition def = minimal("escapemon")
                .name("Quote \" Back\\slash\nNew")
                .build();
        JsonObject root = parse(def); // 能被 JSON 解析器还原即合法
        assertEquals("Quote \" Back\\slash\nNew", root.get("name").getAsString());
    }

    @Test
    void explicitEggGroupsReplaceDefault() {
        SpeciesDefinition def = minimal("eggy").eggGroups("amorphous", "mineral").build();
        assertEquals(List.of("amorphous", "mineral"), jsonList(parse(def).getAsJsonArray("eggGroups")));
    }

    @Test
    void missingRequiredFieldsReject() {
        assertThrows(IllegalStateException.class,
                () -> ExtSpeciesBuilder.create("ns", "noprimary").baseStats(1, 1, 1, 1, 1, 1).build());
        assertThrows(IllegalStateException.class,
                () -> ExtSpeciesBuilder.create("ns", "nostats").primaryType("fire").build());
        assertThrows(IllegalStateException.class,
                () -> ExtSpeciesBuilder.create("ns", "noabilities").primaryType("fire")
                        .baseStats(1, 1, 1, 1, 1, 1).build());
        assertThrows(IllegalStateException.class, () -> ExtSpeciesBuilder.create(" ", "x").build());
    }

    private static ExtSpeciesBuilder minimal(String id) {
        return ExtSpeciesBuilder.create("testns", id)
                .primaryType("normal")
                .baseStats(50, 50, 50, 50, 50, 50)
                .abilities("run_away");
    }

    private static List<String> jsonList(JsonArray array) {
        return array.asList().stream().map(com.google.gson.JsonElement::getAsString).toList();
    }
}
