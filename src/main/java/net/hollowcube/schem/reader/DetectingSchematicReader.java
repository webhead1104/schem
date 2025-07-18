package net.hollowcube.schem.reader;

import net.hollowcube.schem.AxiomBlueprint;
import net.hollowcube.schem.Schematic;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class DetectingSchematicReader implements SchematicReader {

    @Override
    public @NotNull Schematic read(byte @NotNull [] data) throws IOException, UnknownSchematicTypeException {
        // Axiom Blueprint is the simplest because it always has a known magic number at the start.
        var dis = new DataInputStream(new ByteArrayInputStream(data));
        if (dis.readInt() == AxiomBlueprint.MAGIC_NUMBER) {
            return new AxiomBlueprintReader().read(data);
        }

        // All other options are an NBT compound at the root.
        final Map.Entry<String, CompoundBinaryTag> rootPair = BinaryTagIO.reader().readNamed(
                new ByteArrayInputStream(data),
                BinaryTagIO.Compression.GZIP
        );

        final Set<String> keys = rootPair.getValue().keySet();
        return switch (rootPair.getKey()) {
            case "" -> {
                // An empty key at the root can either be Litematica, or Sponge V3 schematic
                if (keys.contains("MinecraftDataVersion") || keys.contains("Regions")) {
                    // Definitely a Litematic schematic.
                    yield new LitematicaSchematicReader().read(rootPair);
                }

                // Otherwise, its probably a sponge schematic
                yield new SpongeSchematicReader().read(rootPair);
            }
            case "Schematic" -> new SpongeSchematicReader().read(rootPair);
            default -> throw new UnknownSchematicTypeException();
        };
    }
}
