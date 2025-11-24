package hu.uni_obuda.thesis.railways.data.delaydatacollector.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void isText_null_returnsFalse() {
        assertThat(StringUtils.isText(null)).isFalse();
    }

    @Test
    void isText_emptyString_returnsFalse() {
        assertThat(StringUtils.isText("")).isFalse();
    }

    @Test
    void isText_blankString_returnsFalse() {
        assertThat(StringUtils.isText("   ")).isFalse();
    }

    @Test
    void isText_nonBlankString_returnsTrue() {
        assertThat(StringUtils.isText("hello")).isTrue();
        assertThat(StringUtils.isText("  hello  ")).isTrue();
    }

    @Test
    void isEachText_nullArray_returnsFalse() {
        assertThat(StringUtils.isEachText((String[]) null)).isFalse();
    }

    @Test
    void isEachText_emptyArray_returnsTrue() {
        assertThat(StringUtils.isEachText()).isTrue();
    }

    @Test
    void isEachText_allValidStrings_returnsTrue() {
        assertThat(StringUtils.isEachText("a", "b", "c")).isTrue();
    }

    @Test
    void isEachText_containsNull_returnsFalse() {
        assertThat(StringUtils.isEachText("a", null, "c")).isFalse();
    }

    @Test
    void isEachText_containsBlank_returnsFalse() {
        assertThat(StringUtils.isEachText("a", "   ", "c")).isFalse();
    }

    @Test
    void isEachText_containsEmpty_returnsFalse() {
        assertThat(StringUtils.isEachText("a", "", "c")).isFalse();
    }

    @Test
    void isAnyText_nullArray_returnsFalse() {
        assertThat(StringUtils.isAnyText((String[]) null)).isFalse();
    }

    @Test
    void isAnyText_emptyArray_returnsFalse() {
        assertThat(StringUtils.isAnyText()).isFalse();
    }

    @Test
    void isAnyText_allInvalid_returnsFalse() {
        assertThat(StringUtils.isAnyText(null, "", "   ")).isFalse();
    }

    @Test
    void isAnyText_atLeastOneValid_returnsTrue() {
        assertThat(StringUtils.isAnyText(null, "", "  ", "hello")).isTrue();
        assertThat(StringUtils.isAnyText("hello", null, "", "   ")).isTrue();
    }
}
