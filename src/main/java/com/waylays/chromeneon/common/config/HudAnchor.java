package com.waylays.chromeneon.common.config;

/** Where a HUD group is attached on screen; its offset is measured inward from that point. */
public enum HudAnchor {
    TOP_LEFT(0, 0), TOP_CENTER(1, 0), TOP_RIGHT(2, 0),
    MIDDLE_LEFT(0, 1), MIDDLE_RIGHT(2, 1),
    BOTTOM_LEFT(0, 2), BOTTOM_CENTER(1, 2), BOTTOM_RIGHT(2, 2);

    private final int column;
    private final int row;

    HudAnchor(int column, int row) {
        this.column = column;
        this.row = row;
    }

    /** Left edge of a {@code width}-wide box anchored here on a {@code screen}-wide screen, {@code offset} inward. */
    public float x(float screen, float width, float offset) {
        return switch (column) {
            case 0 -> offset;
            case 1 -> (screen - width) / 2F + offset;
            default -> screen - width - offset;
        };
    }

    public float y(float screen, float height, float offset) {
        return switch (row) {
            case 0 -> offset;
            case 1 -> (screen - height) / 2F + offset;
            default -> screen - height - offset;
        };
    }

    public boolean isRight() {
        return column == 2;
    }

    public boolean isBottom() {
        return row == 2;
    }
}
