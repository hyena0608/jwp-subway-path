package subway.domain;

import subway.domain.vo.Distance;

import java.util.List;
import java.util.Objects;

public class Section {

    private final Long id;
    private final Distance distance;
    private final boolean isStart;
    private final Station upStation;
    private final Station downStation;

    public Section(
            final Distance distance,
            final boolean isStart,
            final Station upStation,
            final Station downStation
    ) {
        this(null, distance, isStart, upStation, downStation);
    }

    public Section(
            final Long id,
            final Distance distance,
            final boolean isStart,
            final Station upStation,
            final Station downStation
    ) {
        validate(upStation, downStation, distance);
        this.id = id;
        this.distance = distance;
        this.isStart = isStart;
        this.upStation = upStation;
        this.downStation = downStation;
    }

    private void validate(final Station upStation, final Station downStation, final Distance distance) {
        if (Objects.isNull(upStation)) {
            throw new IllegalArgumentException("상행역은 null 일 수 없습니다.");
        }
        if (Objects.isNull(downStation)) {
            throw new IllegalArgumentException("하행역은 null 일 수 없습니다.");
        }
        if (Objects.isNull(distance)) {
            throw new IllegalArgumentException("거리는 null 일 수 없습니다.");
        }
    }

    public List<Section> separateBy(final Section newSection) {
        if (isSameUpStationBy(newSection.upStation)) {
            return createInnerSectionBaseIsUpStation(newSection);
        }
        if (isSameDownStationBy(newSection.downStation)) {
            return createInnerSectionBaseIsDownStation(newSection);
        }
        if (isSameUpStationBy(newSection.downStation)) {
            return createOuterSectionBaseIsUpStation(newSection);
        }
        return createOuterSectionBaseIsDownStation(newSection);
    }

    private List<Section> createInnerSectionBaseIsUpStation(final Section newSection) {
        final Distance oldUpToNewDownDistance = newSection.distance;
        final Distance oldDownToNewUpDistance = this.distance.minus(oldUpToNewDownDistance);

        final Section oldUpToNewDownSection
                = new Section(oldUpToNewDownDistance, this.isStart, this.upStation, newSection.downStation);
        final Section oldDownToNewUpSection
                = new Section(oldDownToNewUpDistance, false, newSection.downStation, this.downStation);

        return List.of(oldUpToNewDownSection, oldDownToNewUpSection);
    }

    private List<Section> createInnerSectionBaseIsDownStation(final Section newSection) {
        final Distance oldDownToNewUpDistance = newSection.distance;
        final Distance oldUpToNewDownDistance = this.distance.minus(oldDownToNewUpDistance);

        final Section oldUpToNewDownSection
                = new Section(oldUpToNewDownDistance, this.isStart, this.upStation, newSection.upStation);
        final Section oldDownToNewUpSection
                = new Section(oldDownToNewUpDistance, false, newSection.upStation, this.downStation);

        return List.of(oldUpToNewDownSection, oldDownToNewUpSection);
    }

    private List<Section> createOuterSectionBaseIsUpStation(final Section newSection) {
        if (this.isStart) {
            return List.of(newSection.toStart(), this.toNotStart());
        }
        return List.of(newSection.toNotStart(), this.toNotStart());
    }

    private Section toNotStart() {
        return new Section(this.id, this.distance, false, this.upStation, this.downStation);
    }

    private Section toStart() {
        return new Section(this.id, this.distance, true, this.upStation, this.downStation);
    }

    private List<Section> createOuterSectionBaseIsDownStation(final Section newSection) {
        return List.of(this, newSection.toNotStart());
    }

    public boolean isBaseStationExist(final Section otherSection) {
        return upStation.equals(otherSection.upStation)
                || upStation.equals(otherSection.downStation)
                || downStation.equals(otherSection.upStation)
                || downStation.equals(otherSection.downStation);
    }

    public boolean isSameUpStationBy(final Station otherStation) {
        return Objects.equals(upStation, otherStation);
    }

    public boolean isSameDownStationBy(final Station otherStation) {
        return Objects.equals(downStation, otherStation);
    }

    public Section sortBy(final Station station) {
        if (isSameUpStationBy(station)) {
            return this;
        }
        return new Section(id, distance, isStart, downStation, upStation);
    }

    public Station findSameStationBy(final Section otherSection) {
        if (upStation.equals(otherSection.upStation)) {
            return upStation;
        }
        if (upStation.equals(otherSection.downStation)) {
            return upStation;
        }
        return downStation;
    }

    public boolean isDistanceEqualsOrGreaterThan(final Section otherSection) {
        return distance.isEqualsOrGreaterThan(otherSection.distance);
    }

    public Long getId() {
        return id;
    }

    public Station getUpStation() {
        return upStation;
    }

    public Station getDownStation() {
        return downStation;
    }

    public Distance getDistance() {
        return distance;
    }

    public boolean getStart() {
        return isStart;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Section that = (Section) o;
        return Objects.equals(distance, that.distance)
                && Objects.equals(upStation, that.upStation)
                && Objects.equals(downStation, that.downStation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distance, upStation, downStation);
    }

    @Override
    public String toString() {
        return "Section{" +
                "id=" + id +
                ", distance=" + distance +
                ", isStart=" + isStart +
                ", upStation=" + upStation +
                ", downStation=" + downStation +
                '}';
    }
}
