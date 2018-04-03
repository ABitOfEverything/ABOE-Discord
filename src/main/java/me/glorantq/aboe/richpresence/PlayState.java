package me.glorantq.aboe.richpresence;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PlayState {
    MENU("aboe_logo", ""),
    SP("aboe_logo", "vn_small"),
    UNOFFICIAL_MP("aboe_logo", "vn_small"),
    OFFICIAL_MP("vn_logo", "aboe_small"),
    DEVELOPER("aboe_logo", "");

    private final String largeKey;
    private final String smallKey;
    private final String details = "<resources.playstate_" + name().toLowerCase() + "_details>";
    private final String state = "<resources.playstate_" + name().toLowerCase() + "_state>";
    private final String largeTooltip = "<resources.playstate_" + name().toLowerCase() + "_largetooltip>";
    private final String smallTooltip = "<resources.playstate_" + name().toLowerCase() + "_smalltooltip>";
}
