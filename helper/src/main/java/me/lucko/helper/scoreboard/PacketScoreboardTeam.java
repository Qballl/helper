/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.helper.scoreboard;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import me.lucko.helper.utils.Color;
import me.lucko.helper.utils.annotation.NonnullByDefault;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements {@link ScoreboardTeam} using ProtocolLib.
 */
@NonnullByDefault
public class PacketScoreboardTeam implements ScoreboardTeam {

    // the display name value in teams if limited to 32 chars
    private static final int MAX_NAME_LENGTH = 32;
    private static String trimName(String name) {
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }

    // the prefix/suffix value in the Teams packet is limited to 16 chars
    private static final int MAX_PREFIX_SUFFIX_LENGTH = 16;
    private static String trimPrefixSuffix(String name) {
        return name.length() > MAX_PREFIX_SUFFIX_LENGTH ? name.substring(0, MAX_PREFIX_SUFFIX_LENGTH) : name;
    }

    // the display name value in teams if limited to 40 chars
    private static final int MAX_TEAM_MEMBER_LENGTH = 40;
    private static String trimMember(String name) {
        return name.length() > MAX_TEAM_MEMBER_LENGTH ? name.substring(0, MAX_TEAM_MEMBER_LENGTH) : name;
    }

    // the parent scoreboard
    private final PacketScoreboard scoreboard;
    // the id of this team
    private final String id;
    // if players should be automatically subscribed
    private boolean autoSubscribe;

    // the members of this team
    private final Set<String> players = Collections.synchronizedSet(new HashSet<>());
    // a set of the players subscribed to & receiving updates for this team
    private final Set<Player> subscribed = Collections.synchronizedSet(new HashSet<>());

    // the current display name
    private String displayName;
    // the current prefix
    private String prefix = "";
    // the current suffix
    private String suffix = "";
    // if friendly fire is allowed
    private boolean allowFriendlyFire = true;
    // if members of this team can see invisible members on the same team
    private boolean canSeeFriendlyInvisibles = true;
    // the current nametag visibility
    private NameTagVisibility nameTagVisibility = NameTagVisibility.ALWAYS;
    // the current collision rule
    private CollisionRule collisionRule = CollisionRule.ALWAYS;
    // color
    private ChatColor color = ChatColor.RESET;

    /**
     * Creates a new scoreboard team
     *
     * @param scoreboard the parent scoreboard
     * @param id the id of this team
     * @param displayName the initial display name
     * @param autoSubscribe if players should be automatically subscribed
     */
    public PacketScoreboardTeam(PacketScoreboard scoreboard, String id, String displayName, boolean autoSubscribe) {
        Preconditions.checkNotNull(id, "id");
        Preconditions.checkArgument(id.length() <= 16, "id cannot be longer than 16 characters");

        this.scoreboard = Preconditions.checkNotNull(scoreboard, "scoreboard");
        this.id = id;
        this.displayName = trimName(Color.colorize(Preconditions.checkNotNull(displayName, "displayName")));
        this.autoSubscribe = autoSubscribe;
    }

    /**
     * Creates a new scoreboard team
     *
     * @param scoreboard the parent scoreboard
     * @param id the id of this team
     * @param displayName the initial display name
     */
    public PacketScoreboardTeam(PacketScoreboard scoreboard, String id, String displayName) {
        this(scoreboard, id, displayName, true);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean shouldAutoSubscribe() {
        return autoSubscribe;
    }

    @Override
    public void setAutoSubscribe(boolean autoSubscribe) {
        this.autoSubscribe = autoSubscribe;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        Preconditions.checkNotNull(displayName, "displayName");
        displayName = trimName(Color.colorize(displayName));
        if (this.displayName.equals(displayName)) {
            return;
        }

        this.displayName = displayName;
        scoreboard.broadcastPacket(subscribed, newUpdatePacket());
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void setPrefix(String prefix) {
        Preconditions.checkNotNull(prefix, "prefix");
        prefix = trimPrefixSuffix(Color.colorize(prefix));
        if (this.prefix.equals(prefix)) {
            return;
        }

        this.prefix = prefix;
        scoreboard.broadcastPacket(subscribed, newUpdatePacket());
    }

    @Override
    public String getSuffix() {
        return suffix;
    }

    @Override
    public void setSuffix(String suffix) {
        Preconditions.checkNotNull(suffix, "suffix");
        suffix = trimPrefixSuffix(Color.colorize(suffix));
        if (this.suffix.equals(suffix)) {
            return;
        }

        this.suffix = suffix;
        scoreboard.broadcastPacket(subscribed, newUpdatePacket());
    }

    @Override
    public boolean isAllowFriendlyFire() {
        return allowFriendlyFire;
    }

    @Override
    public void setAllowFriendlyFire(boolean allowFriendlyFire) {
        if (this.allowFriendlyFire == allowFriendlyFire) {
            return;
        }

        this.allowFriendlyFire = allowFriendlyFire;
        scoreboard.broadcastPacket(subscribed, newUpdatePacket());
    }

    @Override
    public boolean isCanSeeFriendlyInvisibles() {
        return canSeeFriendlyInvisibles;
    }

    @Override
    public void setCanSeeFriendlyInvisibles(boolean canSeeFriendlyInvisibles) {
        if (this.canSeeFriendlyInvisibles == canSeeFriendlyInvisibles) {
            return;
        }

        this.canSeeFriendlyInvisibles = canSeeFriendlyInvisibles;
        scoreboard.broadcastPacket(subscribed, newUpdatePacket());
    }

    @Override
    public NameTagVisibility getNameTagVisibility() {
        return nameTagVisibility;
    }

    @Override
    public void setNameTagVisibility(NameTagVisibility nameTagVisibility) {
        Preconditions.checkNotNull(nameTagVisibility, "nameTagVisibility");
        if (this.nameTagVisibility == nameTagVisibility) {
            return;
        }

        this.nameTagVisibility = nameTagVisibility;
        scoreboard.broadcastPacket(subscribed, newUpdatePacket());
    }

    @Override
    public CollisionRule getCollisionRule() {
        return collisionRule;
    }

    @Override
    public void setCollisionRule(CollisionRule collisionRule) {
        Preconditions.checkNotNull(collisionRule, "collisionRule");
        if (this.collisionRule == collisionRule) {
            return;
        }

        this.collisionRule = collisionRule;
        scoreboard.broadcastPacket(subscribed, newUpdatePacket());
    }

    @Override
    public ChatColor getColor() {
        return color;
    }

    @Override
    public void setColor(ChatColor color) {
        Preconditions.checkNotNull(color, "color");
        if (this.color == color) {
            return;
        }

        this.color = color;
        scoreboard.broadcastPacket(subscribed, newUpdatePacket());
    }

    @Override
    public boolean addPlayer(String player) {
        Preconditions.checkNotNull(player, "player");
        player = trimMember(player);
        if (!players.add(player)) {
            return false;
        }

        scoreboard.broadcastPacket(subscribed, newTeamMemberUpdatePacket(player, MemberAction.ADD));
        return true;
    }

    @Override
    public boolean removePlayer(String player) {
        Preconditions.checkNotNull(player, "player");
        player = trimMember(player);
        if (!players.remove(player)) {
            return false;
        }

        scoreboard.broadcastPacket(subscribed, newTeamMemberUpdatePacket(player, MemberAction.REMOVE));
        return true;
    }

    @Override
    public boolean hasPlayer(String player) {
        Preconditions.checkNotNull(player, "player");
        return players.contains(trimMember(player));
    }

    @Override
    public Set<String> getPlayers() {
        return ImmutableSet.copyOf(players);
    }

    @Override
    public void subscribe(Player player) {
        scoreboard.sendPacket(newCreatePacket(), player);
        subscribed.add(player);
    }

    @Override
    public void unsubscribe(Player player) {
        unsubscribe(player, false);
    }

    @Override
    public void unsubscribe(Player player, boolean fast) {
        if (!subscribed.remove(player) || fast) {
            return;
        }

        scoreboard.sendPacket(newRemovePacket(), player);
    }

    @Override
    public void unsubscribeAll() {
        scoreboard.broadcastPacket(subscribed, newRemovePacket());
        subscribed.clear();
    }

    private PacketContainer newCreatePacket() {
        // create an update packet (as that contains a number of values required by the create packet)
        PacketContainer packet = newUpdatePacket();

        // set mode - byte
        packet.getIntegers().write(1, UpdateType.CREATE.getCode());

        // add player info - array of String(40)
        List<String> players = new ArrayList<>(getPlayers());

        // set players - ProtocolLib handles setting 'Entity Count'
        packet.getSpecificModifier(Collection.class).write(0, players);
        
        return packet;
    }

    private PacketContainer newRemovePacket() {
        // http://wiki.vg/Protocol#Teams
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);

        // remove packet only has the default fields

        // set team name - 	String (16)
        packet.getStrings().write(0, getId());

        // set mode - byte
        packet.getIntegers().write(1, UpdateType.REMOVE.getCode());

        return packet;
    }

    private PacketContainer newUpdatePacket() {
        // http://wiki.vg/Protocol#Teams
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);

        // set team name - 	String (16)
        packet.getStrings().write(0, getId());

        // set mode - byte
        packet.getIntegers().write(1, UpdateType.UPDATE.getCode());

        // set display name - String(32)
        packet.getStrings().write(1, getDisplayName());

        // set prefix - String(16)
        packet.getStrings().write(2, getPrefix());

        // set suffix - String(16)
        packet.getStrings().write(3, getSuffix());

        // set friendly flags - byte - Bit mask. 0x01: Allow friendly fire, 0x02: can see invisible entities on same team
        int data = 0;
        if (isAllowFriendlyFire()) {
            data |= 1;
        }
        if (isCanSeeFriendlyInvisibles()) {
            data |= 2;
        }

        packet.getIntegers().write(2, data);

        // set nametag visibility - String Enum (32)
        packet.getStrings().write(4, getNameTagVisibility().getProtocolName());

        // set collision rule - String Enum (32)
        packet.getStrings().write(5, getCollisionRule().getProtocolName());

        // set color - byte - For colors, the same Chat colors (0-15). -1 indicates RESET/no color.
        packet.getIntegers().write(0, COLOR_CODES.getOrDefault(getColor(), -1));

        return packet;
    }

    private PacketContainer newTeamMemberUpdatePacket(String player, MemberAction action) {
        // http://wiki.vg/Protocol#Teams
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);

        // set team name - 	String (16)
        packet.getStrings().write(0, getId());

        // set mode
        switch (action) {
            case ADD:
                packet.getIntegers().write(1, UpdateType.ADD_PLAYERS.getCode());
                break;
            case REMOVE:
                packet.getIntegers().write(1, UpdateType.REMOVE_PLAYERS.getCode());
                break;
            default:
                throw new RuntimeException();
        }

        // set players - Array of String (40)
        packet.getSpecificModifier(Collection.class).write(0, Collections.singletonList(player));
        
        return packet;
    }

    private enum MemberAction {
        ADD, REMOVE
    }

    private enum UpdateType {
        CREATE(0),
        REMOVE(1),
        UPDATE(2),
        ADD_PLAYERS(3),
        REMOVE_PLAYERS(4);

        private final int code;

        UpdateType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    // a map of colors --> their mojang code int
    private static final Map<ChatColor, Integer> COLOR_CODES;

    static {
        Map<ChatColor, Integer> codes = new EnumMap<>(ChatColor.class);
        try {
            Field codeField = ChatColor.class.getDeclaredField("intCode");
            codeField.setAccessible(true);
            for (ChatColor color : ChatColor.values()) {
                if (color.isColor()) {
                    codes.put(color, codeField.getInt(color));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        COLOR_CODES = codes;
    }

}
