package com.poopycobblemon.cobblemonext.species;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 数据包文件布局契约：pack.mcmeta + data/<ns>/species/<种族>.json */
class SpeciesPackFilesTest {

    @Test
    void laysOutMcmetaAndSpeciesFiles() {
        SpeciesDefinition def = ExtSpeciesBuilder.create("poopy_cobblemon", "dungemon")
                .primaryType("poison").baseStats(1, 2, 3, 4, 5, 6).abilities("stench")
                .build();
        Map<String, byte[]> files = SpeciesPackFiles.create(List.of(def), 48);

        assertEquals(2, files.size());
        JsonObject meta = JsonParser.parseString(new String(files.get("pack.mcmeta"), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("pack");
        assertEquals(48, meta.get("pack_format").getAsInt());
        String json = new String(files.get("data/poopy_cobblemon/species/dungemon.json"), StandardCharsets.UTF_8);
        assertEquals(def.json(), json);
    }

    @Test
    void emptyOrNullDefinitionsReturnNull() {
        assertNull(SpeciesPackFiles.create(List.of(), 48));
        assertNull(SpeciesPackFiles.create(null, 48));
    }
}
