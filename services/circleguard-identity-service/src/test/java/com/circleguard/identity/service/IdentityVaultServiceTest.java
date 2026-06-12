package com.circleguard.identity.service;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdentityVaultServiceTest {

    private IdentityMappingRepository repository;
    private IdentityVaultService service;

    @BeforeEach
    void setUp() {
        repository = mock(IdentityMappingRepository.class);
        service = new IdentityVaultService(repository);
        ReflectionTestUtils.setField(service, "hashSalt", "unit-test-salt");
    }

    @Test
    void getOrCreateAnonymousIdReturnsExistingMappingWhenHashAlreadyExists() {
        UUID anonymousId = UUID.randomUUID();
        IdentityMapping existing = IdentityMapping.builder()
                .anonymousId(anonymousId)
                .realIdentity("student@example.edu")
                .identityHash("existing-hash")
                .salt("existing-salt")
                .build();
        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.of(existing));

        UUID result = service.getOrCreateAnonymousId("student@example.edu");

        assertEquals(anonymousId, result);
        verify(repository, never()).save(any());
    }

    @Test
    void getOrCreateAnonymousIdPersistsNewMappingWhenHashDoesNotExist() {
        UUID generatedId = UUID.randomUUID();
        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(IdentityMapping.class))).thenAnswer(invocation -> {
            IdentityMapping mapping = invocation.getArgument(0);
            mapping.setAnonymousId(generatedId);
            return mapping;
        });

        UUID result = service.getOrCreateAnonymousId("new-student@example.edu");

        assertEquals(generatedId, result);
        ArgumentCaptor<IdentityMapping> captor = ArgumentCaptor.forClass(IdentityMapping.class);
        verify(repository).save(captor.capture());
        assertEquals("new-student@example.edu", captor.getValue().getRealIdentity());
        assertNotNull(captor.getValue().getIdentityHash());
        assertFalse(captor.getValue().getIdentityHash().isBlank());
        assertNotNull(captor.getValue().getSalt());
    }

    @Test
    void getOrCreateAnonymousIdUsesStableHashForSameIdentityAndSalt() {
        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(IdentityMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.getOrCreateAnonymousId("repeat@example.edu");
        service.getOrCreateAnonymousId("repeat@example.edu");

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository, times(2)).findByIdentityHash(hashCaptor.capture());
        assertEquals(hashCaptor.getAllValues().get(0), hashCaptor.getAllValues().get(1));
    }

    @Test
    void resolveRealIdentityReturnsStoredIdentity() {
        UUID anonymousId = UUID.randomUUID();
        IdentityMapping mapping = IdentityMapping.builder()
                .anonymousId(anonymousId)
                .realIdentity("health_user")
                .identityHash("hash")
                .salt("salt")
                .build();
        when(repository.findById(anonymousId)).thenReturn(Optional.of(mapping));

        assertEquals("health_user", service.resolveRealIdentity(anonymousId));
    }

    @Test
    void resolveRealIdentityThrowsNotFoundWhenMappingIsMissing() {
        UUID anonymousId = UUID.randomUUID();
        when(repository.findById(anonymousId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.resolveRealIdentity(anonymousId));
        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Identity not found", ex.getReason());
    }
}
