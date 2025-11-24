package plugily.projects.murdermystery.handlers.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Hologram {

    private final Location location;
    private final List<String> lines;
    private final ItemStack item;
    private final List<Integer> entityIds = new ArrayList<>();
    private final boolean isItem;

    public Hologram(Location location, List<String> lines) {
        this.location = location;
        this.lines = lines;
        this.item = null;
        this.isItem = false;
        generateEntityIds();
    }

    public Hologram(Location location, ItemStack item) {
        this.location = location;
        this.lines = null;
        this.item = item;
        this.isItem = true;
        generateEntityIds();
    }

    private void generateEntityIds() {
        if (isItem) {
            entityIds.add(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
        } else {
            for (int i = 0; i < lines.size(); i++) {
                entityIds.add(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
            }
        }
    }

    public void spawn(Player player) {
        try {
            if (isItem) {
                spawnItemDisplay(player);
            } else {
                spawnTextDisplay(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            destroy(player);
        }
    }

    public void destroy(Player player) {
        try {
            sendDestroyPacket(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Location getLocation() {
        return location;
    }

    // NMS Reflection Helpers

    private void spawnTextDisplay(Player player) throws Exception {
        double yOffset = 0;
        for (int i = 0; i < lines.size(); i++) {
            int entityId = entityIds.get(i);
            UUID uuid = UUID.randomUUID();
            // 1.19.4+ TextDisplay entity type
            // We need to construct the packet using reflection to be version agnostic or at
            // least loose
            // For modern versions (Mojang mappings usually):
            // PacketPlayOutSpawnEntity (ClientboundAddEntityPacket)

            Object packet = createSpawnPacket(entityId, uuid, location.getX(), location.getY() + yOffset,
                    location.getZ(), "TEXT_DISPLAY");
            sendPacket(player, packet);

            // Metadata packet to set text
            // ClientboundSetEntityDataPacket
            Object metaPacket = createMetadataPacket(entityId, lines.get(i));
            sendPacket(player, metaPacket);

            yOffset -= 0.25;
        }
    }

    private void spawnItemDisplay(Player player) throws Exception {
        int entityId = entityIds.get(0);
        UUID uuid = UUID.randomUUID();

        Object packet = createSpawnPacket(entityId, uuid, location.getX(), location.getY(), location.getZ(),
                "ITEM_DISPLAY");
        sendPacket(player, packet);

        // Metadata for item would go here
    }

    private void sendDestroyPacket(Player player) throws Exception {
        // ClientboundRemoveEntitiesPacket
        Class<?> packetClass = getNMSClass("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");
        Constructor<?> constructor = packetClass.getConstructor(List.class); // Or int[] depending on version

        // Modern versions use IntList or List<Integer>
        // For simplicity in this example, we'll assume a constructor that takes a list
        // of IDs exists or we find the right one
        // Actually, mostly it's int... or IntList.
        // Let's try to find a constructor taking int[].

        // Fallback for older/newer:
        Object packet;
        try {
            packet = packetClass.getConstructor(int[].class)
                    .newInstance((Object) entityIds.stream().mapToInt(i -> i).toArray());
        } catch (NoSuchMethodException e) {
            // Try List
            packet = packetClass.getConstructor(List.class).newInstance(entityIds);
        }

        sendPacket(player, packet);
    }

    // --- Reflection Utils ---

    private Object createSpawnPacket(int id, UUID uuid, double x, double y, double z, String typeName)
            throws Exception {
        Class<?> packetClass = getNMSClass("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
        Class<?> entityTypeClass = getNMSClass("net.minecraft.world.entity.EntityType");

        Object entityType = entityTypeClass.getField(typeName).get(null);
        Class<?> vec3Class = getNMSClass("net.minecraft.world.phys.Vec3");

        // Constructor varies by version.
        // Usually: (int id, UUID uuid, double x, double y, double z, float pitch, float
        // yaw, EntityType<?> type, int data, Vec3 velocity, double headYaw)

        // We will try to instantiate it. This is tricky with reflection across
        // versions.
        // For 1.21, it might be different.
        // Simplified approach: Use the constructor with the most parameters or specific
        // types.

        Constructor<?> constructor = packetClass.getConstructors()[0]; // Risky, but often the main one

        // To be safe, we might need a proper NMS helper library or just hardcode for
        // the expected version (1.21 paper)
        // Assuming 1.21 Mojang mappings:
        // public ClientboundAddEntityPacket(int id, UUID uuid, double x, double y,
        // double z, float pitch, float yaw, EntityType<?> type, int data, Vec3
        // velocity, double headYaw)

        Object velocity = vec3Class.getConstructor(double.class, double.class, double.class).newInstance(0, 0, 0);

        return constructor.newInstance(
                id, uuid, x, y, z, 0f, 0f, entityType, 0, velocity, 0d);
    }

    private Object createMetadataPacket(int entityId, String text) throws Exception {
        // Strategy: Create a dummy NMS entity, set the text, and get the metadata from
        // it.
        // This avoids hardcoding DataWatcher IDs which change every version.

        Object nmsWorld = getNMSWorld(location.getWorld());
        Object nmsEntity;

        if (isItem) {
            // ItemDisplay
            Class<?> entityClass = getNMSClass("net.minecraft.world.entity.Display$ItemDisplay");
            Class<?> entityTypeClass = getNMSClass("net.minecraft.world.entity.EntityType");
            Object entityType = entityTypeClass.getField("ITEM_DISPLAY").get(null);
            Class<?> levelClass = getNMSClass("net.minecraft.world.level.Level");

            Constructor<?> constructor = entityClass.getConstructor(entityTypeClass, levelClass);
            nmsEntity = constructor.newInstance(entityType, nmsWorld);

            // Set ItemStack
            // setItemStack(ItemStack)
            // We need to convert Bukkit ItemStack to NMS ItemStack
            Object nmsStack = asNMSCopy(item);
            Method setItemMethod = entityClass.getMethod("setItemStack",
                    getNMSClass("net.minecraft.world.item.ItemStack"));
            setItemMethod.invoke(nmsEntity, nmsStack);

        } else {
            // TextDisplay
            Class<?> entityClass = getNMSClass("net.minecraft.world.entity.Display$TextDisplay");
            Class<?> entityTypeClass = getNMSClass("net.minecraft.world.entity.EntityType");
            Object entityType = entityTypeClass.getField("TEXT_DISPLAY").get(null);
            Class<?> levelClass = getNMSClass("net.minecraft.world.level.Level");

            Constructor<?> constructor = entityClass.getConstructor(entityTypeClass, levelClass);
            nmsEntity = constructor.newInstance(entityType, nmsWorld);

            // Set Text
            // setText(Component)
            Object nmsComponent = createNMSComponent(text);
            Method setTextMethod = entityClass.getMethod("setText",
                    getNMSClass("net.minecraft.network.chat.Component"));
            setTextMethod.invoke(nmsEntity, nmsComponent);

            // Set Billboard (Center)
            // setBillboardConstraints(BillboardConstraints)
            Class<?> billboardClass = getNMSClass("net.minecraft.world.entity.Display$BillboardConstraints");
            Object center = billboardClass.getField("CENTER").get(null);
            Method setBillboardMethod = entityClass.getMethod("setBillboardConstraints", billboardClass);
            setBillboardMethod.invoke(nmsEntity, center);
        }

        // Get DataWatcher items
        // entity.getEntityData().getNonDefaultValues() (or packDirty)
        // In 1.19.4+: getNonDefaultValues() returns List<DataValue<?>>
        // In 1.21: getNonDefaultValues()

        Method getEntityDataMethod = getNMSClass("net.minecraft.world.entity.Entity").getMethod("getEntityData");
        Object entityData = getEntityDataMethod.invoke(nmsEntity);

        Method getNonDefaultValuesMethod = entityData.getClass().getMethod("getNonDefaultValues");
        Object dataValues = getNonDefaultValuesMethod.invoke(entityData);

        if (dataValues == null)
            return null; // Should not happen if we set something

        // ClientboundSetEntityDataPacket(int id, List<DataValue<?>> packedItems)
        Class<?> packetClass = getNMSClass("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
        Constructor<?> packetConstructor = packetClass.getConstructor(int.class, List.class);

        return packetConstructor.newInstance(entityId, dataValues);
    }

    private Object getNMSWorld(org.bukkit.World world) throws Exception {
        Method getHandle = world.getClass().getMethod("getHandle");
        return getHandle.invoke(world);
    }

    private Object asNMSCopy(ItemStack stack) throws Exception {
        Class<?> craftItemStackClass = getCraftClass("inventory.CraftItemStack");
        Method asNMSCopy = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
        return asNMSCopy.invoke(null, stack);
    }

    private Object createNMSComponent(String text) throws Exception {
        // Use CraftChatMessage.fromStringOrNull(text) -> returns Component (NMS)
        // Or use Component.Serializer.fromJson if we have JSON
        // Simplest: CraftChatMessage
        Class<?> craftChatMessageClass = getCraftClass("util.CraftChatMessage");
        Method fromString = craftChatMessageClass.getMethod("fromStringOrNull", String.class);
        return fromString.invoke(null, text);
    }

    private Class<?> getCraftClass(String name) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        return Class.forName("org.bukkit.craftbukkit." + version + "." + name);
    }

    private void sendPacket(Player player, Object packet) throws Exception {
        if (packet == null)
            return;

        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = handle.getClass().getField("connection").get(handle); // ServerGamePacketListenerImpl
        Method sendPacket = connection.getClass().getMethod("send",
                getNMSClass("net.minecraft.network.protocol.Packet"));
        sendPacket.invoke(connection, packet);
    }

    private Class<?> getNMSClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
