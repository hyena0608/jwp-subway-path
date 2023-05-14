package subway.integration.builder;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import subway.application.request.CreateLineRequest;
import subway.application.response.LineResponse;
import subway.application.response.StationResponse;
import subway.integration.support.RestAssuredFixture;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static subway.integration.support.RestAssuredFixture.post;

public class LineAssured {

    private LineAssured() {
    }

    public static CreateLineRequest 노선_요청(final String lineName, final String lineColor) {
        return new CreateLineRequest(lineName, lineColor);
    }

    public static LineRequestBuilder request() {
        return new LineRequestBuilder();
    }

    public static class LineRequestBuilder {

        private ExtractableResponse<Response> response;

        public LineRequestBuilder 노선을_등록한다(final CreateLineRequest request) {
            response = post("/lines", request);
            return this;
        }

        public LineRequestBuilder 노선을_조회한다(final Long lineId) {
            response = RestAssuredFixture.get("/lines/" + lineId);
            return this;
        }

        public LineResponseBuilder response() {
            return new LineResponseBuilder(response);
        }
    }

    public static class LineResponseBuilder {
        private ExtractableResponse<Response> response;

        public LineResponseBuilder(final ExtractableResponse<Response> response) {
            this.response = response;
        }

        public <T> T toBody(Class<T> cls) {
            return response.as(cls);
        }

        public void 노선이_조회된다(final Long lineId, final String lineName, final String lineColor, final List<String> stationNames) {
            final LineResponse response = toBody(LineResponse.class);

            final List<String> responseStationNames = response.getStations()
                    .stream()
                    .map(StationResponse::getName)
                    .collect(Collectors.toList());

            assertAll(
                    () -> assertThat(response.getId()).isEqualTo(lineId),
                    () -> assertThat(response.getName()).isEqualTo(lineName),
                    () -> assertThat(response.getColor()).isEqualTo(lineColor),
                    () -> assertThat(responseStationNames).containsExactlyElementsOf(stationNames)
            );
        }
    }
}
