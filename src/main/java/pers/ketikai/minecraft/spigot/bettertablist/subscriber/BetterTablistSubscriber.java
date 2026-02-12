/*
 *    better-tablist
 *    Copyright (C) 2025  ketikai
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pers.ketikai.minecraft.spigot.bettertablist.subscriber;

import me.clip.placeholderapi.PlaceholderAPI;
import net.minecraft.server.v1_12_R1.ChatComponentText;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.IChatBaseComponent;
import net.minecraft.server.v1_12_R1.MinecraftServer;
import net.minecraft.server.v1_12_R1.NetworkManager;
import net.minecraft.server.v1_12_R1.Packet;
import net.minecraft.server.v1_12_R1.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_12_R1.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.next.context.ContextHolder;
import team.idealstate.sugar.next.context.annotation.component.Subscriber;
import team.idealstate.sugar.next.context.aware.ContextHolderAware;
import team.idealstate.sugar.validate.annotation.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Subscriber
public class BetterTablistSubscriber implements Listener, ContextHolderAware {

    private volatile Plugin plugin;

    @Override
    public void setContextHolder(@NotNull ContextHolder contextHolder) {
        this.plugin = (Plugin) contextHolder;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void on(PlayerJoinEvent event) {
        EntityPlayer handle = ((CraftPlayer) event.getPlayer()).getHandle();
        PlayerConnection connection = handle.playerConnection;
        handle.playerConnection = new BetterTablistPlayerConnection(
                connection.player.server,
                connection.networkManager,
                connection.player
        );
//        UUID uniqueId = event.getPlayer().getUniqueId();
//        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
//            CraftPlayer player = (CraftPlayer) Bukkit.getPlayer(uniqueId);
//            if (player == null) {
//                return;
//            }
//            player.setPlayerListName("lv.%player_level% " + player.getName());
//        }, 1L, 20L);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void on(EntityDamageEvent event) {
        if (DamageCause.HOT_FLOOR.equals(event.getCause())) {
            event.setCancelled(true);
        }
    }

    public static class BetterTablistPlayerConnection extends PlayerConnection {

        private static final Field PACKET_PLAY_OUT_PLAYER_INFO$B;
        private static final Field PACKET_PLAY_OUT_PLAYER_INFO$PLAYER_INFO_DATA$E;
        private static final Field CHAT_COMPONENT_TEXT$B;

        static  {
            Field field = null;
            try {
                field = PacketPlayOutPlayerInfo.class.getDeclaredField("b");
                field.setAccessible(true);
            } catch (Throwable e) {
                Log.error(e);
            }
            PACKET_PLAY_OUT_PLAYER_INFO$B = field;
            field = null;
            try {
                field = PacketPlayOutPlayerInfo.class.getDeclaredClasses()[0].getDeclaredField("e");
                field.setAccessible(true);
            } catch (Throwable e) {
                Log.error(e);
            }
            PACKET_PLAY_OUT_PLAYER_INFO$PLAYER_INFO_DATA$E = field;
            field = null;
            Field modifiersField = null;
            try {
                modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
            } catch (Throwable e) {
                Log.error(e);
            }
            try {
                if (modifiersField != null) {
                    field = ChatComponentText.class.getDeclaredField("b");
                    field.setAccessible(true);
                    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                }
            } catch (Throwable e) {
                Log.error(e);
                field = null;
            }
            CHAT_COMPONENT_TEXT$B = field;
        }

        private static List<?> getPlayerInfoData(PacketPlayOutPlayerInfo packet) {
            if (PACKET_PLAY_OUT_PLAYER_INFO$B == null) {
                return null;
            }
            try {
                return (List<?>) PACKET_PLAY_OUT_PLAYER_INFO$B.get(packet);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private static IChatBaseComponent getPlayerListName(Object playerInfoData) {
            if (PACKET_PLAY_OUT_PLAYER_INFO$PLAYER_INFO_DATA$E == null) {
                return null;
            }
            try {
                return (IChatBaseComponent) PACKET_PLAY_OUT_PLAYER_INFO$PLAYER_INFO_DATA$E.get(playerInfoData);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private static IChatBaseComponent parsePlaceholders(Player player, IChatBaseComponent root) {
            boolean changed = false;
            if (root instanceof ChatComponentText) {
                ChatComponentText that = (ChatComponentText) root;
                String text = that.getText();
                String parsedText = PlaceholderAPI.setPlaceholders(player, text);
                int found = parsedText.lastIndexOf('%');
                if (found != -1) {
                    parsedText = parsedText.length() == found + 1 ? "" : parsedText.substring(found);
                }
                if (!Objects.equals(text, parsedText)) {
                    try {
                        CHAT_COMPONENT_TEXT$B.set(that, parsedText);
                        changed = true;
                    } catch (IllegalAccessException e) {
                        Log.error(e);
                    }
                }

            }
            List<IChatBaseComponent> components = root.a();
            for (IChatBaseComponent component : components) {
                if (parsePlaceholders(player, component) != null) {
                    changed = true;
                }
            }
            return changed ? root : null;
        }

        public BetterTablistPlayerConnection(MinecraftServer minecraftserver, NetworkManager networkmanager, EntityPlayer entityplayer) {
            super(minecraftserver, networkmanager, entityplayer);
        }

        @Override
        public void sendPacket(final Packet<?> packet) {
            if (packet instanceof PacketPlayOutPlayerInfo) {
                PacketPlayOutPlayerInfo that = (PacketPlayOutPlayerInfo) packet;
                List<?> playerInfoData = getPlayerInfoData(that);
                if (playerInfoData != null && !playerInfoData.isEmpty()) {
                    CraftPlayer player = getPlayer();
                    for (Object data : playerInfoData) {
                        IChatBaseComponent listName = getPlayerListName(data);
                        if (listName == null) {
                            continue;
                        }
                        parsePlaceholders(player, listName);
                    }
                }
            }
            super.sendPacket(packet);
        }
    }
}
