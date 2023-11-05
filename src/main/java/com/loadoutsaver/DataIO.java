package com.loadoutsaver;

import com.loadoutsaver.implementations.LoadoutImpl;
import com.loadoutsaver.interfaces.ILoadout;
import com.loadoutsaver.interfaces.ISerializable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data serialization helper methods for the loadout saver.
 */
public class DataIO {
    private static final Charset ENCODING = StandardCharsets.UTF_8;

    private DataIO() {}

    /**
     * Default loader that reads one loadout per line.
     * @param inputs The stream containing loadouts, one per line.
     * @throws IOException If reading from the stream fails.
     */
    public static List<ILoadout> Load(InputStream inputs) throws IOException {
        byte[] loaded = inputs.readAllBytes();
        String decoded = new String(loaded, DataIO.ENCODING);
        return Parse(decoded);
    }

    public static List<ILoadout> Parse(String decoded) {
        String[] lines = decoded.split("\n");
        return Arrays.stream(lines).filter(
                l -> !l.isBlank()
        ).map(LoadoutImpl.Deserializer::DeserializeString).collect(Collectors.toList());
    }

    public static String FullSerialize(Collection<ILoadout> loadouts) {
        List<String> encoded = loadouts.stream().map(ISerializable::SerializeString).collect(Collectors.toList());
        return String.join("\n", encoded);
    }

    /**
     * Default saver that saves loadouts on separate lines (when not encoded).
     * @param outputs The stream to which outputs should be written.
     * @param loadouts The loadouts to be saved.
     * @throws IOException If writing to the stream fails.
     */
    public static void Save(OutputStream outputs, Collection<ILoadout> loadouts) throws IOException {
        byte[] fullOutputs = FullSerialize(loadouts).getBytes(DataIO.ENCODING);
        outputs.write(fullOutputs);
    }
}
