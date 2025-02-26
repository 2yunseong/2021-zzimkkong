package com.woowacourse.zzimkkong.controller;

import com.woowacourse.zzimkkong.domain.*;
import com.woowacourse.zzimkkong.dto.space.*;
import com.woowacourse.zzimkkong.infrastructure.auth.AuthorizationExtractor;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalTime;
import java.util.List;

import static com.woowacourse.zzimkkong.Constants.*;
import static com.woowacourse.zzimkkong.DocumentUtils.*;
import static com.woowacourse.zzimkkong.controller.MapControllerTest.saveMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

class ManagerSpaceControllerTest extends AcceptanceTest {
    private String spaceApi;
    private Long beSpaceId;
    private Space be;
    private Space fe;

    @BeforeEach
    void setUp() {
        String lutherId = saveMap("/api/managers/maps", mapCreateUpdateRequest).header("location").split("/")[4];
        spaceApi = "/api/managers/maps/" + lutherId + "/spaces";
        ExtractableResponse<Response> saveBeSpaceResponse = saveSpace(spaceApi, beSpaceCreateUpdateRequest);
        ExtractableResponse<Response> saveFe1SpaceResponse = saveSpace(spaceApi, feSpaceCreateUpdateRequest);

        beSpaceId = Long.valueOf(saveBeSpaceResponse.header("location").split("/")[6]);
        Long feSpaceId = Long.valueOf(saveFe1SpaceResponse.header("location").split("/")[6]);

        Member pobi = Member.builder()
                .email(EMAIL)
                .userName(POBI)
                .emoji(ProfileEmoji.MAN_DARK_SKIN_TONE_TECHNOLOGIST)
                .password(passwordEncoder.encode(PW))
                .organization(ORGANIZATION)
                .build();
        Map luther = new Map(LUTHER_NAME, MAP_DRAWING_DATA, MAP_SVG, pobi);
        Setting beSetting = Setting.builder()
                .settingTimeSlot(TimeSlot.of(
                        BE_AVAILABLE_START_TIME,
                        BE_AVAILABLE_END_TIME))
                .reservationTimeUnit(BE_RESERVATION_TIME_UNIT)
                .reservationMinimumTimeUnit(BE_RESERVATION_MINIMUM_TIME_UNIT)
                .reservationMaximumTimeUnit(BE_RESERVATION_MAXIMUM_TIME_UNIT)
                .enabledDayOfWeek(BE_ENABLED_DAY_OF_WEEK)
                .priorityOrder(0)
                .build();

        Setting feSetting = Setting.builder()
                .settingTimeSlot(TimeSlot.of(
                        FE_AVAILABLE_START_TIME,
                        FE_AVAILABLE_END_TIME))
                .reservationTimeUnit(FE_RESERVATION_TIME_UNIT)
                .reservationMinimumTimeUnit(FE_RESERVATION_MINIMUM_TIME_UNIT)
                .reservationMaximumTimeUnit(FE_RESERVATION_MAXIMUM_TIME_UNIT)
                .enabledDayOfWeek(FE_ENABLED_DAY_OF_WEEK)
                .priorityOrder(0)
                .build();

        be = Space.builder()
                .id(beSpaceId)
                .name(BE_NAME)
                .color(BE_COLOR)
                .map(luther)
                .area(SPACE_DRAWING)
                .reservationEnable(BE_RESERVATION_ENABLE)
                .spaceSettings(Settings.toPrioritizedSettings(List.of(beSetting)))
                .build();

        fe = Space.builder()
                .id(feSpaceId)
                .name(FE_NAME)
                .color(FE_COLOR)
                .map(luther)
                .area(SPACE_DRAWING)
                .reservationEnable(FE_RESERVATION_ENABLE)
                .spaceSettings(Settings.toPrioritizedSettings(List.of(feSetting)))
                .build();
    }

    @Test
    @DisplayName("space 정보가 들어오면 space를 저장한다")
    void save() {
        // given
        SettingRequest newSettingRequest = new SettingRequest(
                LocalTime.of(10, 0),
                LocalTime.of(20, 0),
                30,
                60,
                120,
                EnabledDayOfWeekDto.from("monday, tuesday, wednesday, thursday, friday, saturday, sunday"),
                1
        );

        SpaceCreateUpdateRequest newSpaceCreateUpdateRequest = new SpaceCreateUpdateRequest(
                "잠실우리집",
                "#CCFFE5",
                SPACE_DRAWING,
                MAP_SVG,
                true,
                List.of(newSettingRequest)
        );

        // when
        ExtractableResponse<Response> response = saveSpace(spaceApi, newSpaceCreateUpdateRequest);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @Test
    @DisplayName("space 정보 중 주어지지 않은 필드를 디폴트 값으로 저장한다")
    void save_default() {
        // given, when
        SettingRequest settingRequest = new SettingRequest(
                LocalTime.of(0, 0),
                LocalTime.of(18, 0),
                null,
                null,
                null,
                null,
                null
        );

        SpaceCreateUpdateRequest defaultSpaceCreateUpdateRequest = new SpaceCreateUpdateRequest(
                "잠실우리집",
                "#CCFFE5",
                SPACE_DRAWING,
                MAP_SVG,
                true,
                List.of(settingRequest)
        );

        Setting defaultSetting = Setting.builder()
                .settingTimeSlot(TimeSlot.of(
                        LocalTime.of(0, 0),
                        LocalTime.of(18, 0)))
                .reservationTimeUnit(TimeUnit.from(10))
                .reservationMinimumTimeUnit(TimeUnit.from(10))
                .reservationMaximumTimeUnit(TimeUnit.from(120))
                .enabledDayOfWeek("monday, tuesday, wednesday, thursday, friday, saturday, sunday")
                .priorityOrder(0)
                .build();

        Space defaultSpace = Space.builder()
                .name(defaultSpaceCreateUpdateRequest.getName())
                .color(defaultSpaceCreateUpdateRequest.getColor())
                .spaceSettings(Settings.toPrioritizedSettings(List.of(defaultSetting)))
                .reservationEnable(true)
                .area(SPACE_DRAWING)
                .build();

        ExtractableResponse<Response> response = saveSpace(spaceApi, defaultSpaceCreateUpdateRequest);

        // then
        String api = response.header("location");

        ExtractableResponse<Response> findResponse = findSpace(api);
        SpaceFindDetailResponse actualSpaceFindDetailResponse = findResponse.as(SpaceFindDetailResponse.class);
        SpaceFindDetailResponse expectedSpaceFindDetailResponse = SpaceFindDetailResponse.from(defaultSpace);

        assertThat(actualSpaceFindDetailResponse)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expectedSpaceFindDetailResponse);
    }

    @Test
    @DisplayName("spaceId를 받아 해당 공간에 대한 정보를 조회한다.")
    void find() {
        // given, when
        String api = spaceApi + "/" + beSpaceId;
        ExtractableResponse<Response> response = findSpace(api);
        SpaceFindDetailResponse actual = response.body().as(SpaceFindDetailResponse.class);
        SpaceFindDetailResponse expected = SpaceFindDetailResponse.from(be);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(actual).usingRecursiveComparison()
                .ignoringActualNullFields()
                .ignoringExpectedNullFields()
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("전체 공간에 대한 정보를 조회한다.")
    void findAll() {
        // given, when
        ExtractableResponse<Response> response = findAllSpace(spaceApi);
        SpaceFindAllResponse actual = response.body().as(SpaceFindAllResponse.class);
        SpaceFindAllResponse expected = SpaceFindAllResponse.from(List.of(be, fe));

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(actual).usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("공간을 수정한다.")
    void update() {
        // given, when
        SettingRequest settingRequest = new SettingRequest(
                LocalTime.of(10, 0),
                LocalTime.of(22, 0),
                30,
                60,
                120,
                EnabledDayOfWeekDto.from("monday, tuesday, wednesday, thursday, friday, saturday, sunday"),
                1
        );

        SpaceCreateUpdateRequest updateSpaceCreateUpdateRequest = new SpaceCreateUpdateRequest(
                "바다",
                "#CCCCFF",
                SPACE_DRAWING,
                MAP_SVG,
                false,
                List.of(settingRequest)
        );

        String api = spaceApi + "/" + beSpaceId;
        ExtractableResponse<Response> response = updateSpace(api, updateSpaceCreateUpdateRequest);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("공간을 삭제한다.")
    void delete() {
        // given, when
        String api = spaceApi + "/" + beSpaceId;
        SpaceDeleteRequest spaceDeleteRequest = new SpaceDeleteRequest(MAP_SVG);

        ExtractableResponse<Response> response = deleteSpace(api, spaceDeleteRequest);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    static ExtractableResponse<Response> saveSpace(final String api, final SpaceCreateUpdateRequest spaceCreateRequest) {
        return RestAssured
                .given(getRequestSpecification()).log().all()
                .accept("application/json")
                .header("Authorization", AuthorizationExtractor.AUTHENTICATION_TYPE + " " + accessToken)
                .filter(document("space/manager/post", getRequestPreprocessor(), getResponsePreprocessor()))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(spaceCreateRequest)
                .when().post(api)
                .then().log().all().extract();
    }

    private ExtractableResponse<Response> findAllSpace(final String api) {
        return RestAssured
                .given(getRequestSpecification()).log().all()
                .accept("application/json")
                .header("Authorization", AuthorizationExtractor.AUTHENTICATION_TYPE + " " + accessToken)
                .filter(document("space/manager/getAll", getRequestPreprocessor(), getResponsePreprocessor()))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when().get(api)
                .then().log().all().extract();
    }

    private ExtractableResponse<Response> findSpace(final String api) {
        return RestAssured
                .given(getRequestSpecification()).log().all()
                .accept("application/json")
                .header("Authorization", AuthorizationExtractor.AUTHENTICATION_TYPE + " " + accessToken)
                .filter(document("space/manager/get", getRequestPreprocessor(), getResponsePreprocessor()))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when().get(api)
                .then().log().all().extract();
    }

    private ExtractableResponse<Response> updateSpace(
            final String api,
            final SpaceCreateUpdateRequest spaceCreateUpdateRequest) {
        return RestAssured
                .given(getRequestSpecification()).log().all()
                .accept("application/json")
                .header("Authorization", AuthorizationExtractor.AUTHENTICATION_TYPE + " " + accessToken)
                .filter(document("space/manager/put", getRequestPreprocessor(), getResponsePreprocessor()))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(spaceCreateUpdateRequest)
                .when().put(api)
                .then().log().all().extract();
    }

    private ExtractableResponse<Response> deleteSpace(final String api, final SpaceDeleteRequest spaceDeleteRequest) {
        return RestAssured
                .given(getRequestSpecification()).log().all()
                .accept("application/json")
                .header("Authorization", AuthorizationExtractor.AUTHENTICATION_TYPE + " " + accessToken)
                .filter(document("space/manager/delete", getRequestPreprocessor(), getResponsePreprocessor()))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(spaceDeleteRequest)
                .when().delete(api)
                .then().log().all().extract();
    }
}
