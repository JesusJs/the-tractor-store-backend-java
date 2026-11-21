package com.tractorstore.catalog.domain.model;

import java.util.Collection;
import java.util.regex.Pattern;

/** RGB color; channels are {@code double} because an average is rarely an integer. */
public record Color(double red, double green, double blue) {

  private static final Pattern HEX = Pattern.compile("#[0-9A-Fa-f]{6}");

  public Color {
    requireChannel(red, "red");
    requireChannel(green, "green");
    requireChannel(blue, "blue");
  }

  /** Parses {@code #RRGGBB}, ignoring case. */
  public static Color fromHex(String hex) {
    if (hex == null || !HEX.matcher(hex).matches()) {
      throw new IllegalArgumentException("Color must be #RRGGBB: " + hex);
    }
    return new Color(
        Integer.parseInt(hex.substring(1, 3), 16),
        Integer.parseInt(hex.substring(3, 5), 16),
        Integer.parseInt(hex.substring(5, 7), 16));
  }

  /** Mean of each channel. */
  public static Color average(Collection<Color> colors) {
    if (colors == null || colors.isEmpty()) {
      throw new IllegalArgumentException("Cannot average no colors");
    }
    double red = 0;
    double green = 0;
    double blue = 0;
    for (Color color : colors) {
      red += color.red;
      green += color.green;
      blue += color.blue;
    }
    int count = colors.size();
    return new Color(red / count, green / count, blue / count);
  }

  /** Euclidean distance in RGB space. */
  public double distanceTo(Color other) {
    double dr = red - other.red;
    double dg = green - other.green;
    double db = blue - other.blue;
    return Math.sqrt(dr * dr + dg * dg + db * db);
  }

  private static void requireChannel(double value, String name) {
    if (!(value >= 0 && value <= 255)) {
      throw new IllegalArgumentException(name + " must be between 0 and 255: " + value);
    }
  }
}
