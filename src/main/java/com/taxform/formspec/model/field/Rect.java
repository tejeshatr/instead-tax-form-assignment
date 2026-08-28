package com.taxform.formspec.model.field;

import com.taxform.formspec.model.page.PageSize;

/**
 * A box on the page. {@code x}/{@code y} are the top-left corner in PDF
 * points (1/72 inch), measured from the page's top-left corner; x grows
 * right, y grows down. {@code width}/{@code height} are positive.
 */
public record Rect(double x, double y, double width, double height) {

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }

    /** True if the whole rect lies within the given page size. */
    public boolean within(PageSize pageSize) {
        return x >= 0 && y >= 0
                && right() <= pageSize.width()
                && bottom() <= pageSize.height();
    }

    @Override
    public String toString() {
        return "(" + fmt(x) + ", " + fmt(y) + ", " + fmt(width) + " x " + fmt(height) + ")";
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? Long.toString(Math.round(v)) : Double.toString(v);
    }
}
