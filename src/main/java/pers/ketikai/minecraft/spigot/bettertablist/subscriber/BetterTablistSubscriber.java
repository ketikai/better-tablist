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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import me.clip.placeholderapi.PlaceholderAPI;
import net.minecraft.server.v1_12_R1.ChatComponentText;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.IChatBaseComponent;
import net.minecraft.server.v1_12_R1.NetworkManager;
import net.minecraft.server.v1_12_R1.Packet;
import net.minecraft.server.v1_12_R1.PacketPlayInAbilities;
import net.minecraft.server.v1_12_R1.PacketPlayInAdvancements;
import net.minecraft.server.v1_12_R1.PacketPlayInArmAnimation;
import net.minecraft.server.v1_12_R1.PacketPlayInAutoRecipe;
import net.minecraft.server.v1_12_R1.PacketPlayInBlockDig;
import net.minecraft.server.v1_12_R1.PacketPlayInBlockPlace;
import net.minecraft.server.v1_12_R1.PacketPlayInBoatMove;
import net.minecraft.server.v1_12_R1.PacketPlayInChat;
import net.minecraft.server.v1_12_R1.PacketPlayInClientCommand;
import net.minecraft.server.v1_12_R1.PacketPlayInCloseWindow;
import net.minecraft.server.v1_12_R1.PacketPlayInCustomPayload;
import net.minecraft.server.v1_12_R1.PacketPlayInEnchantItem;
import net.minecraft.server.v1_12_R1.PacketPlayInEntityAction;
import net.minecraft.server.v1_12_R1.PacketPlayInFlying;
import net.minecraft.server.v1_12_R1.PacketPlayInHeldItemSlot;
import net.minecraft.server.v1_12_R1.PacketPlayInKeepAlive;
import net.minecraft.server.v1_12_R1.PacketPlayInRecipeDisplayed;
import net.minecraft.server.v1_12_R1.PacketPlayInResourcePackStatus;
import net.minecraft.server.v1_12_R1.PacketPlayInSetCreativeSlot;
import net.minecraft.server.v1_12_R1.PacketPlayInSettings;
import net.minecraft.server.v1_12_R1.PacketPlayInSpectate;
import net.minecraft.server.v1_12_R1.PacketPlayInSteerVehicle;
import net.minecraft.server.v1_12_R1.PacketPlayInTabComplete;
import net.minecraft.server.v1_12_R1.PacketPlayInTeleportAccept;
import net.minecraft.server.v1_12_R1.PacketPlayInTransaction;
import net.minecraft.server.v1_12_R1.PacketPlayInUpdateSign;
import net.minecraft.server.v1_12_R1.PacketPlayInUseEntity;
import net.minecraft.server.v1_12_R1.PacketPlayInUseItem;
import net.minecraft.server.v1_12_R1.PacketPlayInVehicleMove;
import net.minecraft.server.v1_12_R1.PacketPlayInWindowClick;
import net.minecraft.server.v1_12_R1.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_12_R1.PacketPlayOutPosition;
import net.minecraft.server.v1_12_R1.PlayerConnection;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.next.context.ContextHolder;
import team.idealstate.sugar.next.context.annotation.component.Subscriber;
import team.idealstate.sugar.next.context.aware.ContextHolderAware;
import team.idealstate.sugar.validate.annotation.NotNull;

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
        handle.playerConnection = new BetterTablistPlayerConnection(connection);
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

    @SuppressWarnings("deprecation")
    public static final class BetterTablistPlayerConnection extends PlayerConnection {

        private static final Field PACKET_PLAY_OUT_PLAYER_INFO$B;
        private static final Field PACKET_PLAY_OUT_PLAYER_INFO$PLAYER_INFO_DATA$E;
        private static final Field CHAT_COMPONENT_TEXT$B;

        static {
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

        private final PlayerConnection delegate;

        public BetterTablistPlayerConnection(PlayerConnection delegate) {
            super(delegate.player.server, delegate.networkManager, delegate.player);
            this.delegate = delegate;
        }

        @Override
        public CraftPlayer getPlayer() {
            return delegate.getPlayer();
        }

        @Override
        public void e() {
            delegate.e();
        }

        @Override
        public void syncPosition() {
            delegate.syncPosition();
        }

        @Override
        public NetworkManager a() {
            return delegate.a();
        }

        @Deprecated
        @Override
        public void disconnect(IChatBaseComponent ichatbasecomponent) {
            delegate.disconnect(ichatbasecomponent);
        }

        @Override
        public void disconnect(String s) {
            delegate.disconnect(s);
        }

        @Override
        public void a(PacketPlayInSteerVehicle packetplayinsteervehicle) {
            delegate.a(packetplayinsteervehicle);
        }

        @Override
        public void a(PacketPlayInVehicleMove packetplayinvehiclemove) {
            delegate.a(packetplayinvehiclemove);
        }

        @Override
        public void a(PacketPlayInTeleportAccept packetplayinteleportaccept) {
            delegate.a(packetplayinteleportaccept);
        }

        @Override
        public void a(PacketPlayInRecipeDisplayed packetplayinrecipedisplayed) {
            delegate.a(packetplayinrecipedisplayed);
        }

        @Override
        public void a(PacketPlayInAdvancements packetplayinadvancements) {
            delegate.a(packetplayinadvancements);
        }

        @Override
        public void a(PacketPlayInFlying packetplayinflying) {
            delegate.a(packetplayinflying);
        }

        @Override
        public void a(double d0, double d1, double d2, float f, float f1) {
            delegate.a(d0, d1, d2, f, f1);
        }

        @Override
        public void a(double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
            delegate.a(d0, d1, d2, f, f1, cause);
        }

        @Override
        public void a(
                double d0,
                double d1,
                double d2,
                float f,
                float f1,
                Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> set) {
            delegate.a(d0, d1, d2, f, f1, set);
        }

        @Override
        public void a(
                double d0,
                double d1,
                double d2,
                float f,
                float f1,
                Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> set,
                PlayerTeleportEvent.TeleportCause cause) {
            delegate.a(d0, d1, d2, f, f1, set, cause);
        }

        @Override
        public void teleport(Location dest) {
            delegate.teleport(dest);
        }

        @Override
        public void a(PacketPlayInBlockDig packetplayinblockdig) {
            delegate.a(packetplayinblockdig);
        }

        @Override
        public void a(PacketPlayInUseItem packetplayinuseitem) {
            delegate.a(packetplayinuseitem);
        }

        @Override
        public void a(PacketPlayInBlockPlace packetplayinblockplace) {
            delegate.a(packetplayinblockplace);
        }

        @Override
        public void a(PacketPlayInSpectate packetplayinspectate) {
            delegate.a(packetplayinspectate);
        }

        @Override
        public void a(PacketPlayInResourcePackStatus packetplayinresourcepackstatus) {
            delegate.a(packetplayinresourcepackstatus);
        }

        @Override
        public void a(PacketPlayInBoatMove packetplayinboatmove) {
            delegate.a(packetplayinboatmove);
        }

        @Override
        public void a(IChatBaseComponent ichatbasecomponent) {
            delegate.a(ichatbasecomponent);
        }

        @Override
        public void a(PacketPlayInHeldItemSlot packetplayinhelditemslot) {
            delegate.a(packetplayinhelditemslot);
        }

        @Override
        public void a(PacketPlayInChat packetplayinchat) {
            delegate.a(packetplayinchat);
        }

        @Override
        public void chat(String s, boolean async) {
            delegate.chat(s, async);
        }

        @Override
        public void a(PacketPlayInArmAnimation packetplayinarmanimation) {
            delegate.a(packetplayinarmanimation);
        }

        @Override
        public void a(PacketPlayInEntityAction packetplayinentityaction) {
            delegate.a(packetplayinentityaction);
        }

        @Override
        public void a(PacketPlayInUseEntity packetplayinuseentity) {
            delegate.a(packetplayinuseentity);
        }

        @Override
        public void a(PacketPlayInClientCommand packetplayinclientcommand) {
            delegate.a(packetplayinclientcommand);
        }

        @Override
        public void a(PacketPlayInCloseWindow packetplayinclosewindow) {
            delegate.a(packetplayinclosewindow);
        }

        @Override
        public void a(PacketPlayInWindowClick packetplayinwindowclick) {
            delegate.a(packetplayinwindowclick);
        }

        @Override
        public void a(PacketPlayInAutoRecipe packetplayinautorecipe) {
            delegate.a(packetplayinautorecipe);
        }

        @Override
        public void a(PacketPlayInEnchantItem packetplayinenchantitem) {
            delegate.a(packetplayinenchantitem);
        }

        @Override
        public void a(PacketPlayInSetCreativeSlot packetplayinsetcreativeslot) {
            delegate.a(packetplayinsetcreativeslot);
        }

        @Override
        public void a(PacketPlayInTransaction packetplayintransaction) {
            delegate.a(packetplayintransaction);
        }

        @Override
        public void a(PacketPlayInUpdateSign packetplayinupdatesign) {
            delegate.a(packetplayinupdatesign);
        }

        @Override
        public void a(PacketPlayInKeepAlive packetplayinkeepalive) {
            delegate.a(packetplayinkeepalive);
        }

        @Override
        public void a(PacketPlayInAbilities packetplayinabilities) {
            delegate.a(packetplayinabilities);
        }

        @Override
        public void a(PacketPlayInTabComplete packet) {
            delegate.a(packet);
        }

        @Override
        public void a(PacketPlayInSettings packetplayinsettings) {
            delegate.a(packetplayinsettings);
        }

        @Override
        public void a(PacketPlayInCustomPayload packetplayincustompayload) {
            delegate.a(packetplayincustompayload);
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
            delegate.sendPacket(packet);
        }
    }
}
