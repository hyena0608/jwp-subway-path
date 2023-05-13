package subway.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import subway.config.RepositoryTestConfig;
import subway.dao.entity.SectionEntity;
import subway.dao.entity.StationEntity;
import subway.domain.Distance;
import subway.domain.SectionDomain;
import subway.domain.StationDomain;

import static org.assertj.core.api.Assertions.assertThat;

class SectionRepositoryTest extends RepositoryTestConfig {

    SectionRepository sectionRepository;

    @BeforeEach
    void setUp() {
        sectionRepository = new SectionRepository(sectionDao, stationDao);
    }

    @Test
    void 구간_식별자값으로_구간을_검색하여_도메인_객체를_반환한다() {
        // given
        final Long upStationId = stationDao.insert(new StationEntity("루카"));
        final Long downStationId = stationDao.insert(new StationEntity("헤나"));
        final SectionEntity sectionEntity = new SectionEntity(0L, 10, true, upStationId, downStationId);

        final Long saveSectionId = sectionDao.insert(sectionEntity);

        // when
        final SectionDomain section = sectionRepository.findBySectionId(saveSectionId);

        // then
        assertThat(section)
                .usingRecursiveComparison()
                .isEqualTo(SectionDomain.from(
                        saveSectionId,
                        new Distance(10),
                        true,
                        new StationDomain(upStationId, "루카"),
                        new StationDomain(downStationId, "헤나")
                ));
    }
}
