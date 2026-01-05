package me.moontimer.smpcore.auction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public final class ItemSerializer {
    private ItemSerializer() {
    }

    public static String toBase64(ItemStack item) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(output)) {
            dataOutput.writeObject(item);
            dataOutput.flush();
            return Base64.getEncoder().encodeToString(output.toByteArray());
        }
    }

    public static ItemStack fromBase64(String data) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(data);
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(input)) {
            Object object = dataInput.readObject();
            return (ItemStack) object;
        }
    }
}
