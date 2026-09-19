package com.prizm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.prizm.api.error.ApiException;
import com.prizm.domain.JoinCodeGenerator;
import com.prizm.repository.MemberRepository;
import com.prizm.repository.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpaceServiceJoinCodeTest {

    @Mock
    private SpaceRepository spaceRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private JoinCodeGenerator joinCodeGenerator;

    private SpaceService spaceService;

    @BeforeEach
    void setUp() {
        spaceService = new SpaceService(spaceRepository, memberRepository, joinCodeGenerator, null);
    }

    @Test
    void retriesUntilJoinCodeIsUnique() {
        when(joinCodeGenerator.nextCode()).thenReturn("AAAAAA", "BBBBBB");
        when(spaceRepository.existsByJoinCode("AAAAAA")).thenReturn(true);
        when(spaceRepository.existsByJoinCode("BBBBBB")).thenReturn(false);

        assertEquals("BBBBBB", spaceService.uniqueJoinCode());
        verify(joinCodeGenerator, times(2)).nextCode();
    }

    @Test
    void failsAfterMaxAttempts() {
        when(joinCodeGenerator.nextCode()).thenReturn("DUPDUP");
        when(spaceRepository.existsByJoinCode("DUPDUP")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> spaceService.uniqueJoinCode());
        assertEquals("JOIN_CODE_EXHAUSTED", ex.getError());
        verify(joinCodeGenerator, times(SpaceService.JOIN_CODE_MAX_ATTEMPTS)).nextCode();
    }
}
