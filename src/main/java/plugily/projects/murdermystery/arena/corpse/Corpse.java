package plugily.projects.murdermystery.arena.corpse;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import plugily.projects.murdermystery.handlers.hologram.Hologram;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Corpse {

  private final int entityId;
  private final Hologram hologram;
  private final Location location;
  private final String name;
  private final UUID uuid;
  private final Object gameProfile;

  public Corpse(Hologram hologram, Player player, Location location) {
    this.hologram = hologram;
    this.location = location;
    this.name = player.getName();
    this.uuid = UUID.randomUUID(); // New UUID for corpse to avoid conflict
    this.entityId = (int) (Math.random() * Integer.MAX_VALUE);
    this.gameProfile = createGameProfile(this.uuid, this.name, player);
  }

  public int getId() {
    return entityId;
  }

  public Hologram getHologram() {
    return hologram;
  }

  private org.bukkit.entity.Interaction interaction;

  public void spawn(Player viewer) {
    try {
      Object nmsWorld = getNMSWorld(location.getWorld());
      Object nmsServer = getNMSServer();

      Class<?> serverPlayerClass = getNMSClass("net.minecraft.server.level.ServerPlayer");
      Class<?> serverClass = getNMSClass("net.minecraft.server.MinecraftServer");
      Class<?> levelClass = getNMSClass("net.minecraft.server.level.ServerLevel");
      Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");

      Constructor<?> constructor = serverPlayerClass.getConstructor(serverClass, levelClass, gameProfileClass);
      Object serverPlayer = constructor.newInstance(nmsServer, nmsWorld, gameProfile);

      // Set Location
      Method setPos = serverPlayerClass.getMethod("setPos", double.class, double.class, double.class);
      setPos.invoke(serverPlayer, location.getX(), location.getY(), location.getZ());

      // Set ID (Important for packets)
      Method setId = getNMSClass("net.minecraft.world.entity.Entity").getMethod("setId", int.class);
      setId.invoke(serverPlayer, entityId);

      Class<?> packetInfoClass = getNMSClass("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
      Class<?> actionClass = getNMSClass(
          "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
      Object addPlayerAction = Enum.valueOf((Class<Enum>) actionClass, "ADD_PLAYER");

      // EnumSet.of(ADD_PLAYER)
      Object enumSet = java.util.EnumSet.of((Enum) addPlayerAction);

      Constructor<?> infoConstructor = packetInfoClass.getConstructor(java.util.EnumSet.class,
          java.util.Collection.class);
      Object infoPacket = infoConstructor.newInstance(enumSet, Collections.singletonList(serverPlayer));

      sendPacket(viewer, infoPacket);

      Class<?> spawnPacketClass = getNMSClass("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
      Constructor<?> spawnConstructor = spawnPacketClass.getConstructors()[0]; // Risky but usually works

      Object spawnPacket;
      try {
        spawnPacket = spawnPacketClass.getConstructor(getNMSClass("net.minecraft.world.entity.Entity"))
            .newInstance(serverPlayer);
      } catch (Exception e) {
        spawnPacket = spawnConstructor.newInstance(serverPlayer);
      }
      sendPacket(viewer, spawnPacket);

      Class<?> poseClass = getNMSClass("net.minecraft.world.entity.Pose");
      Object sleepingPose = Enum.valueOf((Class<Enum>) poseClass, "SLEEPING");
      Method setPose = serverPlayerClass.getMethod("setPose", poseClass);
      setPose.invoke(serverPlayer, sleepingPose);

      Method getEntityData = serverPlayerClass.getMethod("getEntityData");
      Object entityData = getEntityData.invoke(serverPlayer);
      Method getNonDefaultValues = entityData.getClass().getMethod("getNonDefaultValues");
      Object dataValues = getNonDefaultValues.invoke(entityData);

      if (dataValues != null) {
        Class<?> metaPacketClass = getNMSClass("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
        Constructor<?> metaConstructor = metaPacketClass.getConstructor(int.class, List.class);
        Object metaPacket = metaConstructor.newInstance(entityId, dataValues);
        sendPacket(viewer, metaPacket);
      }

      Bukkit.getScheduler().runTaskLaterAsynchronously(
          plugily.projects.murdermystery.Main.getPlugin(plugily.projects.murdermystery.Main.class), () -> {
            try {
              // send ClientboundPlayerInfoRemovePacket
              Class<?> removeInfoClass = getNMSClass(
                  "net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket");
              // Constructor(List<UUID>)
              Constructor<?> removeConstructor = removeInfoClass.getConstructor(List.class);
              Object removePacket = removeConstructor.newInstance(Collections.singletonList(uuid));
              sendPacket(viewer, removePacket);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }, 20L);

      if (interaction == null || !interaction.isValid()) {
        Bukkit.getScheduler()
            .runTask(plugily.projects.murdermystery.Main.getPlugin(plugily.projects.murdermystery.Main.class), () -> {
              if (interaction == null || !interaction.isValid()) {
                interaction = (org.bukkit.entity.Interaction) location.getWorld().spawnEntity(location,
                    org.bukkit.entity.EntityType.INTERACTION);
                interaction.setInteractionWidth(1.0f);
                interaction.setInteractionHeight(0.5f); // Lying down height
                interaction.setResponsive(true);
              }
            });
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void destroy(Player viewer) {
    try {
      // ClientboundRemoveEntitiesPacket
      Class<?> packetClass = getNMSClass("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");
      // Constructor(int...) or (List<Integer>)
      Object packet;
      try {
        packet = packetClass.getConstructor(int[].class).newInstance(new int[] { entityId });
      } catch (NoSuchMethodException e) {
        packet = packetClass.getConstructor(List.class).newInstance(Collections.singletonList(entityId));
      }
      sendPacket(viewer, packet);

      // Remove Interaction Entity
      if (interaction != null && interaction.isValid()) {
        Bukkit.getScheduler()
            .runTask(plugily.projects.murdermystery.Main.getPlugin(plugily.projects.murdermystery.Main.class), () -> {
              if (interaction != null) {
                interaction.remove();
                interaction = null;
              }
            });
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public org.bukkit.entity.Interaction getInteraction() {
    return interaction;
  }

  // --- Helpers ---

  private Object createGameProfile(UUID uuid, String name, Player original) {
    try {
      Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
      Constructor<?> constructor = gameProfileClass.getConstructor(UUID.class, String.class);
      Object profile = constructor.newInstance(uuid, name);

      // Copy properties (Skin)
      Object originalHandle = original.getClass().getMethod("getHandle").invoke(original);
      Object originalProfile = originalHandle.getClass().getMethod("getGameProfile").invoke(originalHandle);
      Object properties = originalProfile.getClass().getMethod("getProperties").invoke(originalProfile);

      Method getProperties = gameProfileClass.getMethod("getProperties");
      Object newProperties = getProperties.invoke(profile);

      // PropertyMap.putAll(PropertyMap)
      newProperties.getClass().getMethod("putAll", Class.forName("com.google.common.collect.Multimap"))
          .invoke(newProperties, properties);

      return profile;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private void sendPacket(Player player, Object packet) throws Exception {
    if (packet == null)
      return;
    Object handle = player.getClass().getMethod("getHandle").invoke(player);
    Object connection = handle.getClass().getField("connection").get(handle);
    Method sendPacket = connection.getClass().getMethod("send", getNMSClass("net.minecraft.network.protocol.Packet"));
    sendPacket.invoke(connection, packet);
  }

  private Class<?> getNMSClass(String name) throws ClassNotFoundException {
    return Class.forName(name);
  }

  private Object getNMSServer() throws Exception {
    return Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
  }

  private Object getNMSWorld(org.bukkit.World world) throws Exception {
    return world.getClass().getMethod("getHandle").invoke(world);
  }
}
