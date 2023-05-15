package subway.domain;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;

@DisplayNameGeneration(ReplaceUnderscores.class)
class NameTest {

    @ParameterizedTest
    @NullAndEmptySource
    void 이름이_공백이거나_null일_경우_예외가_발생한다(final String nameValue) {
        assertThatThrownBy(() -> new Name(nameValue))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "01234567890"})
    void 이름의_길이가_1미만_10초과일_경우_예외가_발생한다(final String nameValue) {
        assertThatThrownBy(() -> new Name(nameValue))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
