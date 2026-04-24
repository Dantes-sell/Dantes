package ru.cloud.utility.game.player;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import ru.cloud.client.modules.impl.combat.Aura;
import ru.cloud.base.events.impl.server.EventChatReceive;
import ru.cloud.base.events.impl.server.EventPacket;
import ru.cloud.utility.interfaces.IMinecraft;
import ru.cloud.utility.render.display.base.color.ColorUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks real HP of entities by intercepting multiple sources:
 * - EntityTrackerUpdateS2CPacket (entity metadata, index 9 = health)
 * - Scoreboard (BELOW_NAME, SIDEBAR, LIST slots)
 * - Action bar messages (parsed via regex)
 *
 * Anticheats spoof entity metadata health to 1.0 or maxHealth.
 * We detect spoofed values and fall back to last known good value.
 */
public class HealthTracker implements IMinecraft {

    // entity metadata index for health in LivingEntity is 9 (float)
    private static final int HEALTH_DATA_INDEX = 9;

    // Cache: entityId -> last known real HP
    private final Map<Integer, Float> healthCache = new ConcurrentHashMap<>();
    // Cache: entityId -> last known max HP (from entity itself)
    private final Map<Integer, Float> maxHealthCache = new ConcurrentHashMap<>();
    // UUID -> entityId mapping for scoreboard lookups
    private final Map<UUID, Integer> uuidToEntityId = new ConcurrentHashMap<>();

    // Action bar HP patterns: "20❤", "20 ❤", "20hp", "20 hp", "❤ 20", "HP: 20", "♥20" etc.
    private static final Pattern ACTION_BAR_HP_PATTERN = Pattern.compile(
        "(?:(?:hp|health|❤|♥|❥|\\u2764|\\u2665)\\s*[:\\-]?\\s*(\\d+(?:[.,]\\d+)?)" +
        "|" +
        "(\\d+(?:[.,]\\d+)?)\\s*(?:/\\s*\\d+(?:[.,]\\d+)?)?\\s*(?:hp|health|❤|♥|❥|\\u2764|\\u2665))",
        Pattern.CASE_INSENSITIVE
    );

    // Scoreboard HP patterns for sidebar text lines like "вќ¤ 18.5" or "HP: 20"
    private static final Pattern SCOREBOARD_HP_PATTERN = Pattern.compile(
        "(\\d+(?:[.,]\\d+)?)\\s*(?:/\\s*\\d+(?:[.,]\\d+)?)?\\s*(?:❤|♥|hp|health)",
        Pattern.CASE_INSENSITIVE
    );

    public HealthTracker() {
        EventManager.register(this);
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (!event.isReceive()) return;
        if (!(event.getPacket() instanceof EntityTrackerUpdateS2CPacket packet)) return;

        List<DataTracker.SerializedEntry<?>> entries = packet.trackedValues();
        if (entries == null) return;

        for (DataTracker.SerializedEntry<?> entry : entries) {
            if (entry.id() != HEALTH_DATA_INDEX) continue;
            if (!(entry.handler() == TrackedDataHandlerRegistry.FLOAT)) continue;

            float receivedHp = (float) entry.value();
            int entityId = packet.id();

            // Get entity to validate
            if (mc.world == null) continue;
            var entity = mc.world.getEntityById(entityId);
            if (!(entity instanceof LivingEntity living)) continue;

            float maxHp = living.getMaxHealth();
            maxHealthCache.put(entityId, maxHp);
            uuidToEntityId.put(living.getUuid(), entityId);

            float lastKnown = healthCache.getOrDefault(entityId, -1f);

            // Anticheat detection: spoofed values are typically exactly 1.0 or exactly maxHealth
            // when the entity clearly took damage recently (last known was significantly different)
            boolean likelySpoofed = false;
            if (lastKnown > 0) {
                float delta = Math.abs(receivedHp - lastKnown);
                // If HP jumped from e.g. 18 -> 1.0 (exactly) or 18 -> maxHealth (exactly), it's spoofed
                boolean isExactOne = receivedHp == 1.0f;
                boolean isExactMax = receivedHp == maxHp;
                boolean bigJump = delta > maxHp * 0.5f; // more than 50% HP change in one packet
                likelySpoofed = (isExactOne || isExactMax) && bigJump;
            }

            if (!likelySpoofed && receivedHp >= 0) {
                healthCache.put(entityId, receivedHp);
            }
        }
    }

    @EventTarget
    public void onActionBar(EventChatReceive event) {
        // Action bar messages come through chat receive in some implementations
        tryParseHpFromText(event.getMessage());
    }

    /**
     * Try to extract HP from action bar / chat text and match to nearby players.
     * This is a best-effort approach for servers that display HP in action bar.
     */
    private void tryParseHpFromText(Text text) {
        if (text == null || mc.world == null || mc.player == null) return;
        String raw = ColorUtil.removeFormatting(text.getString());
        if (raw == null) return;

        Matcher m = ACTION_BAR_HP_PATTERN.matcher(raw);
        if (!m.find()) return;

        String hpStr = m.group(1) != null ? m.group(1) : m.group(2);
        if (hpStr == null) return;

        try {
            float hp = Float.parseFloat(hpStr.replace(",", "."));
            // Action bar HP usually refers to the target we're looking at or the closest enemy
            // Try to match to current aura target
            var aura = Aura.INSTANCE;
            var target = aura.getTarget();
            if (target != null) {
                int id = target.getId();
                float maxHp = target.getMaxHealth();
                if (hp >= 0 && hp <= maxHp + target.getAbsorptionAmount() + 20) {
                    healthCache.put(id, hp);
                }
            }
        } catch (NumberFormatException ignored) {}
    }

    /**
     * Get the best available HP for an entity, checking all sources.
     */
    public float getHealth(LivingEntity entity) {
        if (entity == null) return 0;

        int entityId = entity.getId();
        uuidToEntityId.put(entity.getUuid(), entityId);

        // 1. Try scoreboard BELOW_NAME (most reliable on servers that use it)
        if (entity instanceof net.minecraft.entity.player.PlayerEntity player) {
            float scoreHp = getScoreboardHealth(player);
            if (scoreHp > 0) return scoreHp;
        }

        // 2. Try cached value from entity metadata packet interception
        Float cached = healthCache.get(entityId);
        if (cached != null && cached > 0) {
            // Add absorption on top if entity still has it
            float absorption = entity.getAbsorptionAmount();
            return Math.min(cached + absorption, entity.getMaxHealth() + absorption);
        }

        // 3. Fallback: direct entity health + absorption
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    /**
     * Get max health for display purposes (base max + current absorption).
     */
    public float getMaxHealth(LivingEntity entity) {
        return entity.getMaxHealth() + entity.getAbsorptionAmount();
    }

    /**
     * Read HP from all available scoreboard slots.
     */
    private float getScoreboardHealth(net.minecraft.entity.player.PlayerEntity player) {
        if (mc.world == null) return -1;
        Scoreboard scoreboard = player.getScoreboard();

        // Check all display slots that might contain health
        ScoreboardDisplaySlot[] slots = {
            ScoreboardDisplaySlot.BELOW_NAME,
            ScoreboardDisplaySlot.LIST,
            ScoreboardDisplaySlot.SIDEBAR
        };

        for (ScoreboardDisplaySlot slot : slots) {
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(slot);
            if (objective == null) continue;

            // Check if objective name/display name suggests it's health-related
            String objName = objective.getName().toLowerCase();
            String objDisplay = ColorUtil.removeFormatting(objective.getDisplayName().getString());
            if (objDisplay == null) objDisplay = "";
            objDisplay = objDisplay.toLowerCase();

            boolean isHealthObjective = objName.contains("health") || objName.contains("hp") ||
                objName.contains("life") || objName.contains("hearts") ||
                objDisplay.contains("health") || objDisplay.contains("hp") ||
                objDisplay.contains("❤") || objDisplay.contains("♥") ||
                slot == ScoreboardDisplaySlot.BELOW_NAME; // BELOW_NAME is almost always HP

            if (!isHealthObjective) continue;

            try {
                MutableText scoreText = ReadableScoreboardScore.getFormattedScore(
                    scoreboard.getScore(player, objective),
                    objective.getNumberFormatOr(StyledNumberFormat.EMPTY)
                );
                String raw = ColorUtil.removeFormatting(scoreText.getString());
                if (raw == null) continue;

                // Try direct parse first
                try {
                    float hp = Float.parseFloat(raw.trim());
                    if (hp > 0) return hp;
                } catch (NumberFormatException ignored) {}

                // Try regex parse for formatted strings like "18вќ¤" or "HP: 20"
                Matcher m = SCOREBOARD_HP_PATTERN.matcher(raw);
                if (m.find()) {
                    float hp = Float.parseFloat(m.group(1).replace(",", "."));
                    if (hp > 0) return hp;
                }
            } catch (Exception ignored) {}
        }

        return -1;
    }

    /**
     * Clear cache for a specific entity (e.g. when they leave).
     */
    public void clearEntity(int entityId) {
        healthCache.remove(entityId);
        maxHealthCache.remove(entityId);
    }

    public void clearAll() {
        healthCache.clear();
        maxHealthCache.clear();
        uuidToEntityId.clear();
    }
}
