package com.mystichorizons.mysticnametags.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mystichorizons.mysticnametags.config.LanguageManager;
import com.mystichorizons.mysticnametags.config.Settings;
import com.mystichorizons.mysticnametags.integrations.IntegrationManager;
import com.mystichorizons.mysticnametags.nameplate.NameplateManager;
import com.mystichorizons.mysticnametags.tags.TagDefinition;
import com.mystichorizons.mysticnametags.tags.TagManager;
import com.mystichorizons.mysticnametags.util.ColorFormatter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.logging.Level;

/**
 * Owned-tags selector UI.
 *
 * Layout file: mysticnametags/OwnedTags.ui
 *
 * Lists ONLY tags the player already owns and lets them equip/unequip.
 */
public class MysticNameTagsOwnedTagsUI extends InteractiveCustomUIPage<MysticNameTagsTagsUI.UIEventData> {

    public static final String LAYOUT = "mysticnametags/OwnedTags.ui";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final int MAX_ROWS  = 10;
    private static final int PAGE_SIZE = 10;

    private final PlayerRef playerRef;
    private final UUID uuid;

    private int currentPage;
    private String filterQuery;

    public MysticNameTagsOwnedTagsUI(@Nonnull PlayerRef playerRef, @Nonnull UUID uuid) {
        this(playerRef, uuid, 0, null);
    }

    public MysticNameTagsOwnedTagsUI(@Nonnull PlayerRef playerRef,
                                     @Nonnull UUID uuid,
                                     int page,
                                     String filterQuery) {
        // Reuse the same UIEventData codec as the main Tags UI
        super(playerRef, CustomPageLifetime.CanDismiss, MysticNameTagsTagsUI.UIEventData.CODEC);
        this.playerRef = playerRef;
        this.uuid = uuid;
        this.currentPage = Math.max(page, 0);
        this.filterQuery = normalizeFilter(filterQuery);
    }

    private static String normalizeFilter(String filter) {
        if (filter == null) return null;
        String trimmed = filter.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd, @NotNull UIEventBuilder evt, @NotNull Store<EntityStore> store) {

        // Load layout (supports localized overrides)
        cmd.append(LanguageManager.get().resolveUi(LAYOUT));

        // Static button bindings
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BottomCloseButton",
                com.hypixel.hytale.server.core.ui.builder.EventData.of("Action", "close"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#PrevPageButton",
                com.hypixel.hytale.server.core.ui.builder.EventData.of("Action", "prev_page"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#NextPageButton",
                com.hypixel.hytale.server.core.ui.builder.EventData.of("Action", "next_page"));

        // Filter controls
        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ApplyFilterButton",
                new com.hypixel.hytale.server.core.ui.builder.EventData()
                        .append("Action", "set_filter")
                        .append("Filter", "#TagSearchBox.Text")
        );

        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ClearFilterButton",
                new com.hypixel.hytale.server.core.ui.builder.EventData()
                        .append("Action", "set_filter")
                        .append("Filter", "")
        );

        rebuildPage(ref, store, cmd, evt);
    }

    private List<TagDefinition> createOwnedSnapshot() {
        TagManager tagManager = TagManager.get();
        Collection<TagDefinition> all = tagManager.getAllTags();

        List<TagDefinition> owned = new ArrayList<>();
        String needle = (filterQuery != null) ? filterQuery.toLowerCase(Locale.ROOT) : null;

        for (TagDefinition def : all) {
            if (def == null || def.getId() == null) continue;
            if (!tagManager.ownsTag(uuid, def.getId())) continue; // ONLY owned tags

            if (needle != null) {
                String id       = def.getId() != null ? def.getId() : "";
                String display  = def.getDisplay() != null ? def.getDisplay() : "";
                String descr    = def.getDescription() != null ? def.getDescription() : "";
                String category = def.getCategory() != null ? def.getCategory() : "";

                String plainDisplay = ColorFormatter.stripFormatting(display);

                String haystack = (id + " " + plainDisplay + " " + descr + " " + category)
                        .toLowerCase(Locale.ROOT);

                if (!haystack.contains(needle)) continue;
            }

            owned.add(def);
        }

        return owned;
    }

    private void rebuildPage(@Nonnull Ref<EntityStore> ref,
                             @Nonnull Store<EntityStore> store,
                             @Nonnull com.hypixel.hytale.server.core.ui.builder.UICommandBuilder cmd,
                             @Nonnull com.hypixel.hytale.server.core.ui.builder.UIEventBuilder evt) {

        LanguageManager lang = LanguageManager.get();
        TagManager tagManager = TagManager.get();

        List<TagDefinition> tags = createOwnedSnapshot();

        int totalTags  = tags.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalTags / (double) PAGE_SIZE));
        if (currentPage > totalPages - 1) currentPage = totalPages - 1;

        int startIndex = currentPage * PAGE_SIZE;
        int endIndex   = Math.min(startIndex + PAGE_SIZE, totalTags);

        // Search box placeholder
        if (filterQuery != null) {
            cmd.set("#TagSearchBox.PlaceholderText",
                    lang.tr("ui.tags.search_filter_prefix", Map.of("filter", filterQuery)));
        } else {
            cmd.set("#TagSearchBox.PlaceholderText", lang.tr("ui.tags.search_placeholder"));
        }

        // Equipped tag id
        TagDefinition active = tagManager.getEquipped(uuid);
        if (active == null) {
            active = tagManager.resolveActiveOrDefaultTag(uuid);
        }
        String equippedId = (active != null) ? active.getId() : null;

        // ---- Rows ----
        int row = 0;
        for (int i = startIndex; i < endIndex && row < MAX_ROWS; i++, row++) {
            TagDefinition def = tags.get(i);

            String nameSelector   = "#TagRow" + row + "Name";
            String descSelector   = "#TagRow" + row + "Description";
            String buttonSelector = "#TagRow" + row + "Button";

            String rawDisplay     = def.getDisplay();
            String rawDescription = def.getDescription();

            String nameText = ColorFormatter.stripFormatting(rawDisplay);
            String nameHex  = ColorFormatter.extractUiTextColor(rawDisplay);

            String descText = rawDescription != null ? ColorFormatter.stripFormatting(rawDescription) : "";
            if (descText.length() > 90) descText = descText.substring(0, 87) + "...";

            cmd.set(nameSelector + ".Text", nameText);
            cmd.set(descSelector + ".Text", descText);

            if (nameHex != null) {
                cmd.set(nameSelector + ".Style.TextColor", "#" + nameHex);
            }

            boolean isEquipped = equippedId != null && equippedId.equalsIgnoreCase(def.getId());

            String buttonText = isEquipped
                    ? lang.tr("ui.tags.button_unequip")
                    : lang.tr("ui.tags.button_equip");

            cmd.set(buttonSelector + ".Text", buttonText);

            cmd.set(nameSelector + ".Visible", true);
            cmd.set(descSelector + ".Visible", true);
            cmd.set(buttonSelector + ".Visible", true);

            // Row click event
            com.hypixel.hytale.server.core.ui.builder.EventData rowEvent =
                    new com.hypixel.hytale.server.core.ui.builder.EventData()
                            .append("Action", "tag_click")
                            .append("TagId", def.getId() != null ? def.getId() : "")
                            .append("RowIndex", String.valueOf(row));

            evt.addEventBinding(CustomUIEventBindingType.Activating, buttonSelector, rowEvent, false);
        }

        // Hide unused rows
        for (; row < MAX_ROWS; row++) {
            String nameSelector   = "#TagRow" + row + "Name";
            String descSelector   = "#TagRow" + row + "Description";
            String buttonSelector = "#TagRow" + row + "Button";

            cmd.set(nameSelector + ".Visible", false);
            cmd.set(descSelector + ".Visible", false);
            cmd.set(buttonSelector + ".Visible", false);
        }

        // Page label + nav
        String label;
        if (totalTags == 0) {
            label = (filterQuery != null)
                    ? lang.tr("ui.tags.page_none_for_filter", Map.of("filter", filterQuery))
                    : "You don't own any tags yet.";
        } else {
            label = lang.tr("ui.tags.page_label", Map.of(
                    "page", String.valueOf(currentPage + 1),
                    "pages", String.valueOf(totalPages)
            ));
            if (filterQuery != null) {
                label += "  " + lang.tr("ui.tags.page_filter_suffix", Map.of("filter", filterQuery));
            }
        }

        cmd.set("#PageLabel.Text", label);
        cmd.set("#PrevPageButton.Visible", totalTags > 0 && currentPage > 0);
        cmd.set("#NextPageButton.Visible", totalTags > 0 && currentPage < totalPages - 1);

        // Current nameplate preview
        String previewText;
        String previewHex = null;

        try {
            String baseName = playerRef.getUsername();
            String coloredNameplate = tagManager.buildNameplate(playerRef, baseName, uuid);
            previewText = ColorFormatter.stripFormatting(coloredNameplate);

            if (uuid != null) {
                String rankPrefix = TagManager.get().getIntegrations().getPrimaryPrefix(uuid);
                previewHex = ColorFormatter.extractFirstHexColor(rankPrefix);

                if (previewHex == null && active != null) {
                    previewHex = ColorFormatter.extractFirstHexColor(active.getDisplay());
                }
            }
        } catch (Throwable ignored) {
            previewText = playerRef.getUsername();
        }

        cmd.set("#CurrentNameplateLabel.Text", previewText);
        if (previewHex != null) {
            cmd.set("#CurrentNameplateLabel.Style.TextColor", "#" + previewHex);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull MysticNameTagsTagsUI.UIEventData data) {

        String action = data.action;
        if (action == null) return;

        switch (action) {
            case "close" -> this.close();

            case "prev_page" -> {
                if (currentPage <= 0) return;
                currentPage--;

                var cmd = new com.hypixel.hytale.server.core.ui.builder.UICommandBuilder();
                var evt = new com.hypixel.hytale.server.core.ui.builder.UIEventBuilder();
                rebuildPage(ref, store, cmd, evt);
                sendUpdate(cmd, evt, false);
            }

            case "next_page" -> {
                List<TagDefinition> tags = createOwnedSnapshot();
                int totalPages = Math.max(1, (int) Math.ceil(tags.size() / (double) PAGE_SIZE));
                if (currentPage >= totalPages - 1) return;

                currentPage++;

                var cmd = new com.hypixel.hytale.server.core.ui.builder.UICommandBuilder();
                var evt = new com.hypixel.hytale.server.core.ui.builder.UIEventBuilder();
                rebuildPage(ref, store, cmd, evt);
                sendUpdate(cmd, evt, false);
            }

            case "set_filter" -> {
                long now = System.currentTimeMillis();
                // simple debounce like main UI
                // if you want: keep a lastFilterApplyMs here too
                String requested = data.filter;
                String newFilter = normalizeFilter(requested);

                if (!Objects.equals(this.filterQuery, newFilter)) {
                    this.filterQuery = newFilter;
                    currentPage = 0;
                }

                var cmd = new com.hypixel.hytale.server.core.ui.builder.UICommandBuilder();
                var evt = new com.hypixel.hytale.server.core.ui.builder.UIEventBuilder();
                rebuildPage(ref, store, cmd, evt);
                sendUpdate(cmd, evt, false);
            }

            case "tag_click" -> {
                if (uuid == null) return;

                TagManager manager = TagManager.get();

                TagDefinition def = null;
                String resolvedId = null;

                if (data.tagId != null && !data.tagId.isEmpty()) {
                    def = manager.getTag(data.tagId);
                    if (def != null) {
                        resolvedId = def.getId();
                    }
                }

                if (def == null) {
                    int rowIndex = data.rowIndex;
                    if (rowIndex < 0 || rowIndex >= MAX_ROWS) return;

                    List<TagDefinition> tags = createOwnedSnapshot();
                    int startIndex = currentPage * PAGE_SIZE;
                    int absIndex = startIndex + rowIndex;

                    if (absIndex < 0 || absIndex >= tags.size()) return;

                    def = tags.get(absIndex);
                    if (def == null || def.getId() == null || def.getId().isEmpty()) return;

                    resolvedId = def.getId();
                }

                if (resolvedId == null || resolvedId.isEmpty()) return;

                TagManager.TagPurchaseResult result;

                try {
                    result = manager.toggleTag(playerRef, uuid, resolvedId);

                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player != null) {
                        String baseName = playerRef.getUsername();
                        try {
                            switch (result) {
                                case UNLOCKED_FREE:
                                case UNLOCKED_PAID:
                                case EQUIPPED_ALREADY_OWNED: {
                                    String text = manager.buildPlainNameplate(playerRef, baseName, uuid);
                                    NameplateManager.get().apply(uuid, store, ref, text);
                                    break;
                                }
                                case UNEQUIPPED: {
                                    NameplateManager.get().restore(uuid, store, ref, baseName);
                                    String text = manager.buildPlainNameplate(playerRef, baseName, uuid);
                                    NameplateManager.get().apply(uuid, store, ref, text);
                                    break;
                                }
                                default:
                                    break;
                            }
                        } catch (Throwable ignored) { }
                    }

                    // Reuse existing notification flow
                    handlePurchaseResult(result, def);

                } catch (Throwable t) {
                    LOGGER.at(Level.WARNING).withCause(t)
                            .log("[MysticNameTags] Failed to handle owned-tag click for " + resolvedId);
                }

                // Refresh page (button states)
                var updateCmd = new com.hypixel.hytale.server.core.ui.builder.UICommandBuilder();
                var updateEvt = new com.hypixel.hytale.server.core.ui.builder.UIEventBuilder();
                rebuildPage(ref, store, updateCmd, updateEvt);
                sendUpdate(updateCmd, updateEvt, false);
            }
        }
    }

    private void handlePurchaseResult(TagManager.TagPurchaseResult result, TagDefinition def) {
        // For simplicity we just forward to the same logic used by the main tags UI:
        // reuse MysticNameTagsTagsUI.handlePurchaseResult via a tiny helper,
        // OR if you prefer, you can duplicate the method here.
        // To keep it self-contained, we’ll do a minimal duplicate.

        LanguageManager lang = LanguageManager.get();

        String title = "&b" + lang.tr("plugin.title");
        String tagDisplay = def != null ? def.getDisplay() : "";

        String msgKey;
        Map<String, String> vars;

        switch (result) {
            case NOT_FOUND -> {
                msgKey = "tags.not_found";
                vars = Map.of();
            }
            case NO_PERMISSION -> {
                msgKey = "tags.no_permission";
                vars = Map.of();
            }
            case UNLOCKED_FREE -> {
                msgKey = "tags.unlocked_free";
                vars = Map.of("tag", tagDisplay);
            }
            case UNLOCKED_PAID -> {
                msgKey = "tags.unlocked_paid";
                vars = Map.of("tag", tagDisplay);
            }
            case EQUIPPED_ALREADY_OWNED -> {
                msgKey = "tags.equipped";
                vars = Map.of("tag", tagDisplay);
            }
            case UNEQUIPPED -> {
                msgKey = "tags.unequipped";
                vars = Map.of("tag", tagDisplay);
            }
            case NO_ECONOMY -> {
                msgKey = "tags.no_economy";
                vars = Map.of();
            }
            case NOT_ENOUGH_MONEY -> {
                msgKey = "tags.not_enough_money";
                vars = Map.of();
            }
            case TRANSACTION_FAILED -> {
                msgKey = "tags.transaction_failed";
                vars = Map.of();
            }
            case REQUIREMENTS_NOT_MET -> {
                msgKey = "tags.requirements_not_met";
                vars = Map.of();
            }
            default -> {
                msgKey = "tags.unknown_result";
                vars = Map.of("result", String.valueOf(result));
            }
        }

        String msg = lang.tr(msgKey, vars);

        String parsedTitle = com.mystichorizons.mysticnametags.integrations.WiFlowPlaceholderSupport.apply(playerRef, title);
        String parsedMsg   = com.mystichorizons.mysticnametags.integrations.WiFlowPlaceholderSupport.apply(playerRef, msg);

        parsedTitle = ColorFormatter.colorize(parsedTitle);
        parsedMsg   = ColorFormatter.colorize(parsedMsg);

        com.mystichorizons.mysticnametags.util.MysticNotificationUtil.send(
                playerRef.getPacketHandler(),
                parsedTitle,
                parsedMsg,
                com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Default
        );
    }
}