package com.tractorstore.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ColorTest {

  @ParameterizedTest
  @CsvSource({"#C0C0C0, 192, 192, 192", "#dd5219, 221, 82, 25", "#DD5219, 221, 82, 25"})
  void parsesHexIgnoringCase(String hex, double red, double green, double blue) {
    assertThat(Color.fromHex(hex)).isEqualTo(new Color(red, green, blue));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"C0C0C0", "#C0C0C", "#GGGGGG", "", "#C0C0C0C"})
  void rejectsInvalidHex(String hex) {
    assertThatThrownBy(() -> Color.fromHex(hex)).isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @CsvSource({"-1, 0, 0", "0, 256, 0", "0, 0, 255.5"})
  void rejectsChannelsOutOfRange(double red, double green, double blue) {
    assertThatThrownBy(() -> new Color(red, green, blue))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsChannelBounds() {
    assertThat(new Color(0, 255, 127.5).blue()).isEqualTo(127.5);
  }

  @Test
  void distanceIsEuclidean() {
    Color black = Color.fromHex("#000000");
    Color white = Color.fromHex("#FFFFFF");

    assertThat(black.distanceTo(white)).isCloseTo(Math.sqrt(3 * 255.0 * 255.0), within(1e-9));
    assertThat(black.distanceTo(white)).isCloseTo(441.6729559300637, within(1e-9));
  }

  @Test
  void distanceIsSymmetricAndZeroToItself() {
    Color silver = Color.fromHex("#C0C0C0");
    Color copper = Color.fromHex("#dd5219");

    assertThat(silver.distanceTo(copper)).isEqualTo(copper.distanceTo(silver));
    assertThat(silver.distanceTo(silver)).isZero();
  }

  @Test
  void averageIsPerChannelMean() {
    Color average = Color.average(List.of(Color.fromHex("#000000"), Color.fromHex("#FFFFFF")));

    assertThat(average).isEqualTo(new Color(127.5, 127.5, 127.5));
  }

  @Test
  void averageOfOneColorIsThatColor() {
    Color copper = Color.fromHex("#dd5219");

    assertThat(Color.average(List.of(copper))).isEqualTo(copper);
  }

  @Test
  void averageOfNothingIsRejected() {
    assertThatThrownBy(() -> Color.average(List.of())).isInstanceOf(IllegalArgumentException.class);
  }
}
