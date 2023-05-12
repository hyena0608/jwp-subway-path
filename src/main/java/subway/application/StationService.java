package subway.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import subway.dao.LineDao;
import subway.dao.SectionDao;
import subway.dao.StationDao;
import subway.dao.StationLineDao;
import subway.dao.dto.SectionEntity;
import subway.domain.Distance;
import subway.domain.Line;
import subway.domain.Station;
import subway.domain.StationLine;
import subway.dto.DeleteStationRequest;
import subway.dto.StationRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
public class StationService {

    private final StationDao stationDao;
    private final LineDao lineDao;
    private final SectionDao sectionDao;
    private final StationLineDao stationLineDao;

    public StationService(
            final StationDao stationDao,
            final LineDao lineDao,
            final SectionDao sectionDao,
            final StationLineDao stationLineDao
    ) {
        this.stationDao = stationDao;
        this.lineDao = lineDao;
        this.sectionDao = sectionDao;
        this.stationLineDao = stationLineDao;
    }

    @Transactional
    public Long saveStation(StationRequest stationRequest) {
        final Distance distance = new Distance(stationRequest.getDistance());

        final List<StationLine> upStationLines = stationLineDao.findByStationName(stationRequest.getUpStationName());
        final List<StationLine> downStationLines = stationLineDao.findByStationName(stationRequest.getDownStationName());

        final List<Line> linesDownStation = downStationLines.stream()
                .map(StationLine::getLine)
                .collect(Collectors.toList());

        final Optional<Line> maybeSameLine = upStationLines.stream()
                .map(StationLine::getLine)
                .filter(linesDownStation::contains)
                .findFirst();

        if (maybeSameLine.isPresent() && maybeSameLine.get().getName().equals(stationRequest.getLineName())) {
            throw new IllegalArgumentException("이미 존재하는 구간입니다.");
        }

        final Line line = getLine(stationRequest);
        final Station upStation = getStation(stationRequest.getUpStationName(), line.getId());
        final Station downStation = getStation(stationRequest.getDownStationName(), line.getId());

        final List<SectionEntity> sectionDaoByLineId = sectionDao.findByLineId(line.getId());

        final boolean isStart = sectionDaoByLineId.stream()
                .filter(SectionEntity::getStart)
                .anyMatch(sectionEntity -> sectionEntity.getDownStationId().equals(downStation.getId()));

        Optional<SectionEntity> maybeDownSection = sectionDao.findDownSectionByStationIdAndLineId(upStation.getId(), line.getId());
        Optional<SectionEntity> maybeUpSection = sectionDao.findUpSectionByStationIdAndLineId(downStation.getId(), line.getId());

        if (maybeDownSection.isPresent()) {
            final SectionEntity downSection = maybeDownSection.get();
            if (distance.isSameOrOver(new Distance(downSection.getDistance()))) {
                throw new IllegalArgumentException("새로운 구간의 거리가 기존 두 역의 거리보다 작아야합니다.");
            }

            sectionDao.delete(downSection.getId());
            sectionDao.save(downStation.getId(), downSection.getDownStationId(), line.getId(), false, distance);
            return sectionDao.save(upStation.getId(), downStation.getId(), line.getId(), isStart, distance);
        } else if (maybeUpSection.isPresent()) {
            final SectionEntity upSection = maybeUpSection.get();
            if (distance.isSameOrOver(new Distance(upSection.getDistance()))) {
                throw new IllegalArgumentException("새로운 구간의 거리가 기존 두 역의 거리보다 작아야합니다.");
            }

            sectionDao.delete(upSection.getId());
            sectionDao.save(upSection.getUpStationId(), upStation.getId(), line.getId(), isStart, distance);
            return sectionDao.save(upStation.getId(), downStation.getId(), line.getId(), false, distance);
        } else {
            return sectionDao.save(upStation.getId(), downStation.getId(), line.getId(), isStart, distance);
        }
    }

    private Line getLine(final StationRequest stationRequest) {
        final Optional<Line> maybeLine = lineDao.findByName(stationRequest.getLineName());
        if (maybeLine.isPresent()) {
            return maybeLine.get();
        }
        final Long lineId = lineDao.save(stationRequest.getLineName(), stationRequest.getLineColor());
        return lineDao.findById(lineId).orElseThrow(() -> new IllegalStateException("노선 저장 시 오류가 발생했습니다."));
    }

    private Station getStation(final String stationName, final Long lineId) {
        final Optional<Station> maybeUpStation = stationDao.findByName(stationName);
        if (maybeUpStation.isPresent()) {
            return maybeUpStation.get();
        }
        final Long stationId = stationDao.save(stationName);
        stationLineDao.save(stationId, lineId);
        return stationDao.findById(stationId).orElseThrow(() -> new IllegalStateException("역 저장 시 오류가 발생하였습니다."));
    }

    @Transactional
    public void deleteStationByStationNameAndLineName(final DeleteStationRequest deleteStationRequest) {
        final Station station = stationDao.findByName(deleteStationRequest.getStationName())
                .orElseThrow(() -> new IllegalArgumentException("역이 존재하지 않습니다."));
        final Line line = lineDao.findByName(deleteStationRequest.getLineName())
                .orElseThrow(() -> new IllegalArgumentException("노선이 존재하지 않습니다."));

        final Optional<SectionEntity> maybeUpSection = sectionDao.findUpSectionByStationIdAndLineId(station.getId(), line.getId());
        final Optional<SectionEntity> maybeDownSection = sectionDao.findDownSectionByStationIdAndLineId(station.getId(), line.getId());

        if (maybeUpSection.isPresent() && maybeDownSection.isPresent()) {
            final SectionEntity upSectionEntity = maybeUpSection.get();
            final SectionEntity downSectionEntity = maybeDownSection.get();

            sectionDao.delete(upSectionEntity.getId());
            sectionDao.delete(downSectionEntity.getId());

            sectionDao.save(
                    upSectionEntity.getUpStationId(),
                    downSectionEntity.getDownStationId(),
                    line.getId(),
                    upSectionEntity.getStart(),
                    new Distance(upSectionEntity.getDistance() + downSectionEntity.getDistance())
            );
        }

        if((maybeUpSection.isPresent() && maybeDownSection.isEmpty())) { // 타겟 역이 하행 종점
            final SectionEntity upSectionEntity = maybeUpSection.get();
            sectionDao.delete(upSectionEntity.getId());
        }

        if((maybeDownSection.isPresent() && maybeUpSection.isEmpty())) { // 타겟 역이 상행 종점
            final SectionEntity downSectionEntity = maybeDownSection.get();
            sectionDao.delete(downSectionEntity.getId());
        }

        //타겟 역을 지우기
        deleteStationWhenAnotherLineNotExist(station.getId(), line.getId());

        List<StationLine> stationLines = stationLineDao.findByLineId(line.getId());
        if (stationLines.size() <= 1) {
            final StationLine stationLine = stationLines.get(0);
            deleteStationWhenAnotherLineNotExist(stationLine.getStation().getId(), line.getId());
            lineDao.deleteById(line.getId());
        }
    }

    private void deleteStationWhenAnotherLineNotExist(final Long stationId, final Long lineId) { //
        stationLineDao.deleteByStationIdAndLineId(stationId, lineId);
        final List<StationLine> stationLines = stationLineDao.findByStationId(stationId);
        if (stationLines.isEmpty()) {
            stationDao.deleteById(stationId);
        }
    }
}
