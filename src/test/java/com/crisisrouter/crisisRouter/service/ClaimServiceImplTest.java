package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.model.entity.*;
import com.crisisrouter.crisisRouter.repository.ClaimRepository;
import com.crisisrouter.crisisRouter.repository.ResourceRequestRepository;
import com.crisisrouter.crisisRouter.repository.UserRepository;
import com.crisisrouter.crisisRouter.service.dto.ClaimDTO;
import com.crisisrouter.crisisRouter.service.impl.ClaimServiceImpl;
import com.crisisrouter.crisisRouter.testutil.SecurityTestUtil;
import com.crisisrouter.crisisRouter.testutil.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ResourceRequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ClaimServiceImpl claimService;

    @AfterEach
    void tearDown() {
        SecurityTestUtil.clearSecurityContext();
    }

    // ── claimRequest ──

    @Test
    void claimRequest_openRequest_createsClaimAndUpdatesStatus() {
        User creator = TestDataFactory.createUser("creator@test.com", "Creator", "User");
        User volunteer = TestDataFactory.createUser("vol@test.com", "Vol", "Teer");
        Category category = TestDataFactory.createCategory();
        ResourceRequest request = TestDataFactory.createResourceRequest(creator, category, RequestStatus.OPEN);

        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(userRepository.findById(volunteer.getId())).thenReturn(Optional.of(volunteer));
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> {
            Claim c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(requestRepository.save(request)).thenReturn(request);

        ClaimDTO result = claimService.claimRequest(request.getId(), volunteer.getId());

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CLAIMED);
        verify(messagingTemplate).convertAndSend(eq("/topic/claim/" + request.getId()), any(ClaimDTO.class));
        verify(auditService).logAction(eq("CLAIM_REQUEST"), eq("ResourceRequest"),
                eq(request.getId()), any(), any(), eq(volunteer));
    }

    @Test
    void claimRequest_requestNotFound_throwsRuntimeException() {
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.claimRequest(requestId, UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Request not found");
    }

    @Test
    void claimRequest_notOpenRequest_throwsIllegalStateException() {
        User creator = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest request = TestDataFactory.createResourceRequest(creator, category, RequestStatus.CLAIMED);

        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> claimService.claimRequest(request.getId(), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not OPEN");
    }

    @Test
    void claimRequest_volunteerNotFound_throwsRuntimeException() {
        User creator = TestDataFactory.createUser();
        Category category = TestDataFactory.createCategory();
        ResourceRequest request = TestDataFactory.createResourceRequest(creator, category, RequestStatus.OPEN);
        UUID volunteerId = UUID.randomUUID();

        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(userRepository.findById(volunteerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.claimRequest(request.getId(), volunteerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Volunteer not found");
    }

    // ── getVolunteerClaims ──

    @Test
    void getVolunteerClaims_returnsMappedDTOs() {
        User creator = TestDataFactory.createUser("creator@test.com", "Creator", "User");
        User volunteer = TestDataFactory.createUser("vol@test.com", "Vol", "Teer");
        Category category = TestDataFactory.createCategory();
        ResourceRequest request = TestDataFactory.createResourceRequest(creator, category, RequestStatus.CLAIMED);
        Claim claim = TestDataFactory.createClaim(request, volunteer);

        when(claimRepository.findByUserId(volunteer.getId())).thenReturn(List.of(claim));

        List<ClaimDTO> result = claimService.getVolunteerClaims(volunteer.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVolunteerFirstName()).isEqualTo("Vol");
        assertThat(result.get(0).getRequestTitle()).isEqualTo(request.getTitle());
    }

    @Test
    void getVolunteerClaims_emptyList_returnsEmptyList() {
        UUID volunteerId = UUID.randomUUID();
        when(claimRepository.findByUserId(volunteerId)).thenReturn(Collections.emptyList());

        List<ClaimDTO> result = claimService.getVolunteerClaims(volunteerId);

        assertThat(result).isEmpty();
    }

    // ── getClaimsByRequestId ──

    @Test
    void getClaimsByRequestId_returnsMappedDTOs() {
        User creator = TestDataFactory.createUser("creator@test.com", "Creator", "User");
        User volunteer = TestDataFactory.createUser("vol@test.com", "Vol", "Teer");
        Category category = TestDataFactory.createCategory();
        ResourceRequest request = TestDataFactory.createResourceRequest(creator, category, RequestStatus.CLAIMED);
        Claim claim = TestDataFactory.createClaim(request, volunteer);

        when(claimRepository.findByRequestId(request.getId())).thenReturn(List.of(claim));

        List<ClaimDTO> result = claimService.getClaimsByRequestId(request.getId());

        assertThat(result).hasSize(1);
    }

    // ── getMyClaims ──

    @Test
    void getMyClaims_authenticated_returnsClaimsForUser() {
        User creator = TestDataFactory.createUser("creator@test.com", "Creator", "User");
        User volunteer = TestDataFactory.createUser("vol@test.com", "Vol", "Teer");
        Category category = TestDataFactory.createCategory();
        ResourceRequest request = TestDataFactory.createResourceRequest(creator, category, RequestStatus.CLAIMED);
        Claim claim = TestDataFactory.createClaim(request, volunteer);

        SecurityTestUtil.mockSecurityContext("vol@test.com");
        when(userRepository.findByEmail("vol@test.com")).thenReturn(Optional.of(volunteer));
        when(claimRepository.findByUserId(volunteer.getId())).thenReturn(List.of(claim));

        List<ClaimDTO> result = claimService.getMyClaims();

        assertThat(result).hasSize(1);
    }

    @Test
    void getMyClaims_notAuthenticated_throwsRuntimeException() {
        SecurityTestUtil.mockSecurityContextNonOidc();

        assertThatThrownBy(() -> claimService.getMyClaims())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("authenticated");
    }

    @Test
    void getMyClaims_userNotFound_throwsRuntimeException() {
        SecurityTestUtil.mockSecurityContext("missing@test.com");
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.getMyClaims())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── completeClaim ──

    @Test
    void completeClaim_setsStatusToFulfilledOnBothClaimAndRequest() {
        User creator = TestDataFactory.createUser("creator@test.com", "Creator", "User");
        User volunteer = TestDataFactory.createUser("vol@test.com", "Vol", "Teer");
        Category category = TestDataFactory.createCategory();
        ResourceRequest request = TestDataFactory.createResourceRequest(creator, category, RequestStatus.CLAIMED);
        Claim claim = TestDataFactory.createClaim(request, volunteer, "ACTIVE");

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(claim)).thenReturn(claim);
        when(requestRepository.save(request)).thenReturn(request);

        claimService.completeClaim(claim.getId());

        assertThat(claim.getStatus()).isEqualTo("FULFILLED");
        assertThat(request.getStatus()).isEqualTo(RequestStatus.FULFILLED);
        verify(messagingTemplate).convertAndSend(eq("/topic/claim/" + request.getId()), any(ClaimDTO.class));
    }

    @Test
    void completeClaim_claimNotFound_throwsRuntimeException() {
        UUID claimId = UUID.randomUUID();
        when(claimRepository.findById(claimId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.completeClaim(claimId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Claim not found");
    }

    // ── dropClaim ──

    @Test
    void dropClaim_activeClaim_dropsAndRevertsRequestToOpen() {
        User creator = TestDataFactory.createUser("creator@test.com", "Creator", "User");
        User volunteer = TestDataFactory.createUser("vol@test.com", "Vol", "Teer");
        Category category = TestDataFactory.createCategory();
        ResourceRequest request = TestDataFactory.createResourceRequest(creator, category, RequestStatus.CLAIMED);
        Claim claim = TestDataFactory.createClaim(request, volunteer, "ACTIVE");

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(claim)).thenReturn(claim);
        when(requestRepository.save(request)).thenReturn(request);

        claimService.dropClaim(claim.getId());

        assertThat(claim.getStatus()).isEqualTo("DROPPED");
        assertThat(request.getStatus()).isEqualTo(RequestStatus.OPEN);
        verify(messagingTemplate).convertAndSend(eq("/topic/claim/" + request.getId()), any(ClaimDTO.class));
    }

    @Test
    void dropClaim_nonActiveClaim_throwsIllegalStateException() {
        User creator = TestDataFactory.createUser("creator@test.com", "Creator", "User");
        User volunteer = TestDataFactory.createUser("vol@test.com", "Vol", "Teer");
        Category category = TestDataFactory.createCategory();
        ResourceRequest request = TestDataFactory.createResourceRequest(creator, category, RequestStatus.FULFILLED);
        Claim claim = TestDataFactory.createClaim(request, volunteer, "FULFILLED");

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> claimService.dropClaim(claim.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void dropClaim_claimNotFound_throwsRuntimeException() {
        UUID claimId = UUID.randomUUID();
        when(claimRepository.findById(claimId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.dropClaim(claimId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Claim not found");
    }

    @Test
    void dropClaim_requestNotClaimed_doesNotRevertStatus() {
        User creator = TestDataFactory.createUser("creator@test.com", "Creator", "User");
        User volunteer = TestDataFactory.createUser("vol@test.com", "Vol", "Teer");
        Category category = TestDataFactory.createCategory();
        ResourceRequest request = TestDataFactory.createResourceRequest(creator, category, RequestStatus.FULFILLED);
        Claim claim = TestDataFactory.createClaim(request, volunteer, "ACTIVE");

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(claim)).thenReturn(claim);

        claimService.dropClaim(claim.getId());

        assertThat(claim.getStatus()).isEqualTo("DROPPED");
        assertThat(request.getStatus()).isEqualTo(RequestStatus.FULFILLED); // Not reverted
        verify(requestRepository, never()).save(request); // Request not saved
    }
}
