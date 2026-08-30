package org.cartscheduler.service;

import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cartscheduler.support.TestEntityFactory.scheduleDay;
import static org.cartscheduler.support.TestEntityFactory.scheduleEntry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduleDayServiceTest {

    @Mock
    private ScheduleDayRepository scheduleDayRepository;

    @InjectMocks
    private ScheduleDayService scheduleDayService;

    @Test
    void shouldMapDaysForRequestedSchedule() {
        ScheduleDay monday = scheduleDay(10L, "Monday", List.of(
                scheduleEntry(1L, "07:00", (short) 7),
                scheduleEntry(2L, "17:00", (short) 17)
        ));
        ScheduleDay tuesday = scheduleDay(20L, "Tuesday", List.of(
                scheduleEntry(3L, "08:00", (short) 8),
                scheduleEntry(4L, "16:00", (short) 16)
        ));
        given(scheduleDayRepository.findForSchedule(5L)).willReturn(List.of(monday, tuesday));

        var result = scheduleDayService.prepareScheduleDayDtoListForSchedule(5L);

        assertThat(result)
                .extracting(dto -> dto.getId(), dto -> dto.getName(), dto -> dto.getFirstHour(), dto -> dto.getLastHour())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, "Monday", 7, 17),
                        org.assertj.core.groups.Tuple.tuple(20L, "Tuesday", 8, 16)
                );
        verify(scheduleDayRepository).findForSchedule(5L);
    }

    @Test
    void shouldMapSingleDayOnlyWhenItBelongsToSchedule() {
        ScheduleDay day = scheduleDay(10L, "Monday", List.of(
                scheduleEntry(1L, "08:00", (short) 8),
                scheduleEntry(2L, "15:00", (short) 15)
        ));
        given(scheduleDayRepository.findByIdForSchedule(10L, 5L)).willReturn(day);

        var result = scheduleDayService.prepareScheduleDayDtoForParticipant(10L, 5L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getFirstHour()).isEqualTo(8);
        assertThat(result.getLastHour()).isEqualTo(15);
        verify(scheduleDayRepository).findByIdForSchedule(10L, 5L);
    }

    @Test
    void shouldReturnNullWhenDayDoesNotBelongToSchedule() {
        given(scheduleDayRepository.findByIdForSchedule(10L, 5L)).willReturn(null);

        var result = scheduleDayService.prepareScheduleDayDtoForParticipant(10L, 5L);

        assertThat(result).isNull();
    }
}
