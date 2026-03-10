package com.mystichorizons.mysticnametags.tags;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.List;

public class TagDefinition {

    // JSON fields
    String id;
    String display;
    String description;
    double price;
    boolean purchasable;
    String permission;
    String category;

    Integer requiredPlaytimeMinutes;
    List<String> requiredOwnedTags;

    // Legacy stat format
    String requiredStatKey;
    Integer requiredStatValue;

    // New stat format
    List<StatRequirement> requiredStats = List.of();

    List<ItemRequirement> requiredItems;
    List<String> onUnlockCommands;
    @SerializedName(value = "placeholderRequirements", alternate = {"requiredPlaceholders"})
    List<PlaceholderRequirement> placeholderRequirements = List.of();

    public static class PlaceholderRequirement {
        String placeholder;
        String operator;
        String value;

        public String getPlaceholder() { return placeholder; }
        public String getOperator() { return operator; }
        public String getValue() { return value; }
    }

    public static class StatRequirement {
        String key;
        Integer min;

        public String getKey() { return key; }
        public Integer getMin() { return min; }

        public boolean isValid() {
            return key != null && !key.isBlank() && min != null && min > 0;
        }
    }

    public static class ItemRequirement {
        String itemId;
        int amount;

        public String getItemId() { return itemId; }
        public int getAmount() { return amount; }
    }

    public String getId() { return id; }
    public String getDisplay() { return display; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public boolean isPurchasable() { return purchasable; }
    public String getPermission() { return permission; }

    public String getCategory() {
        if (category == null) return "General";
        String trimmed = category.trim();
        return trimmed.isEmpty() ? "General" : trimmed;
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }

    @Nullable
    public Integer getRequiredPlaytimeMinutes() {
        return requiredPlaytimeMinutes;
    }

    public boolean hasPlaytimeRequirement() {
        return requiredPlaytimeMinutes != null && requiredPlaytimeMinutes > 0;
    }

    public List<String> getRequiredOwnedTags() {
        return requiredOwnedTags == null ? List.of() : requiredOwnedTags;
    }

    public boolean hasRequiredOwnedTags() {
        return requiredOwnedTags != null && !requiredOwnedTags.isEmpty();
    }

    public List<ItemRequirement> getRequiredItems() {
        return requiredItems == null ? List.of() : requiredItems;
    }

    public boolean hasItemRequirements() {
        return requiredItems != null && !requiredItems.isEmpty();
    }

    public List<String> getOnUnlockCommands() {
        return onUnlockCommands == null ? List.of() : onUnlockCommands;
    }

    public boolean hasOnUnlockCommands() {
        return onUnlockCommands != null && !onUnlockCommands.isEmpty();
    }

    public List<PlaceholderRequirement> getPlaceholderRequirements() {
        return placeholderRequirements != null ? placeholderRequirements : List.of();
    }

    public void setPlaceholderRequirements(List<PlaceholderRequirement> placeholderRequirements) {
        this.placeholderRequirements = (placeholderRequirements != null)
                ? placeholderRequirements
                : List.of();
    }

    public List<StatRequirement> getRequiredStats() {
        if (requiredStats != null && !requiredStats.isEmpty()) {
            return requiredStats;
        }

        if (requiredStatKey != null && !requiredStatKey.isBlank()
                && requiredStatValue != null && requiredStatValue > 0) {
            StatRequirement legacy = new StatRequirement();
            legacy.key = requiredStatKey;
            legacy.min = requiredStatValue;
            return List.of(legacy);
        }

        return List.of();
    }

    public void setRequiredStats(List<StatRequirement> requiredStats) {
        this.requiredStats = (requiredStats != null) ? requiredStats : List.of();
    }

    public boolean hasStatRequirement() {
        return !getRequiredStats().isEmpty();
    }

    @Nullable
    public String getRequiredStatKey() {
        return requiredStatKey;
    }

    @Nullable
    public Integer getRequiredStatValue() {
        return requiredStatValue;
    }

    public boolean usesLegacyStatRequirement() {
        return requiredStatKey != null && !requiredStatKey.isBlank()
                && requiredStatValue != null && requiredStatValue > 0;
    }

    public void clearLegacyStatRequirement() {
        this.requiredStatKey = null;
        this.requiredStatValue = null;
    }
}