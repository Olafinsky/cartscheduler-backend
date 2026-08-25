package org.cartscheduler.controller.rest;

import org.cartscheduler.dto.rest.response.ScheduleDayDto;
import org.cartscheduler.dto.rest.response.ScheduleDto;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.service.ScheduleDayService;
import org.cartscheduler.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cartscheduler.support.TestEntityFactory.user;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SchedulesControllerTest {

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private ScheduleDayService scheduleDayService;

    @InjectMocks
    private SchedulesController schedulesController;

    @Test
    void shouldReturnRequestedScheduleWhenPrincipalHasAccess() {
        RestUserDetails principal = user(1L);
        ScheduleDto expected = new ScheduleDto(5L, "Main schedule");
        given(scheduleService.checkScheduleAccess(principal, 5L)).willReturn(true);
        given(scheduleService.prepareScheduleDtoForParticipant(1L, 5L)).willReturn(expected);

        var result = schedulesController.getSchedule(principal, 5L);

        assertThat(result).isSameAs(expected);
        verify(scheduleService).prepareScheduleDtoForParticipant(1L, 5L);
    }

    @Test
    void shouldForbidScheduleWhenPrincipalHasNoAccess() {
        RestUserDetails principal = user(1L);
        given(scheduleService.checkScheduleAccess(principal, 5L)).willReturn(false);

        assertThatThrownBy(() -> schedulesController.getSchedule(principal, 5L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(scheduleDayService);
    }

    @Test
    void shouldLoadDaysUsingScheduleIdRatherThanParticipantId() {
        RestUserDetails principal = user(1L);
        List<ScheduleDayDto> expected = List.of(new ScheduleDayDto(10L, "Monday", 7, 17));
        given(scheduleService.checkScheduleAccess(principal, 5L)).willReturn(true);
        given(scheduleDayService.prepareScheduleDayDtoListForSchedule(5L)).willReturn(expected);

        var result = schedulesController.getDaysForSchedule(principal, 5L);

        assertThat(result).isSameAs(expected);
        verify(scheduleDayService).prepareScheduleDayDtoListForSchedule(5L);
    }

    @Test
    void shouldForbidDaysWhenPrincipalHasNoScheduleAccess() {
        RestUserDetails principal = user(1L);
        given(scheduleService.checkScheduleAccess(principal, 5L)).willReturn(false);

        assertThatThrownBy(() -> schedulesController.getDaysForSchedule(principal, 5L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(scheduleDayService);
    }

    @Test
    void shouldReturnDayOnlyWhenPrincipalHasScheduleAccess() {
        RestUserDetails principal = user(1L);
        ScheduleDayDto expected = new ScheduleDayDto(10L, "Monday", 7, 17);
        given(scheduleService.checkScheduleAccess(principal, 5L)).willReturn(true);
        given(scheduleDayService.prepareScheduleDayDtoForParticipant(10L, 5L)).willReturn(expected);

        var result = schedulesController.getDayForSchedule(principal, 5L, 10L);

        assertThat(result).isSameAs(expected);
        verify(scheduleDayService).prepareScheduleDayDtoForParticipant(10L, 5L);
    }
}
