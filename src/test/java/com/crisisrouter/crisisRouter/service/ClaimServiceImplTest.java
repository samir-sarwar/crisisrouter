package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.model.entity.*;
import com.crisisrouter.crisisRouter.repository.ClaimRepository;
import com.crisisrouter.crisisRouter.repository.ResourceRequestRepository;
import com.crisisrouter.crisisRouter.repository.UserRepository;
import com.crisisrouter.crisisRouter.service.impl.ClaimServiceImpl;
import com.crisisrouter.crisisRouter.service.dto.ClaimDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito
class ClaimServiceImplTest {

    @Mock private ClaimRepository claimRepository;
    @Mock private ResourceRequestRepository requestRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private AuditService auditService;

    @InjectMocks // Inject the mocks above into the real service
    private ClaimServiceImpl claimService;

    @Test
    void claimRequest_Success() {
        // 1. SETUP DATA
        UUID requestId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();

        ResourceRequest mockRequest = new ResourceRequest();
        mockRequest.setId(requestId);
        mockRequest.setStatus(RequestStatus.OPEN);

        User mockVolunteer = new User();
        mockVolunteer.setId(volunteerId);

        // 2. TEACH THE MOCKS (When x is called, return y)
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(mockRequest));
        when(userRepository.findById(volunteerId)).thenReturn(Optional.of(mockVolunteer));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArguments()[0]); // Return what was saved

        // 3. RUN THE METHOD
        ClaimDTO result = claimService.claimRequest(requestId, volunteerId);

        // 4. VERIFY RESULTS
        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());

        // Verify the status changed in the entity
        assertEquals(RequestStatus.CLAIMED, mockRequest.getStatus());

        // Verify "Side Effects" happened (Audit log & WebSocket)
        verify(auditService).logAction(eq("CLAIM_REQUEST"), any(), any(), any(), any(), any());
        verify(messagingTemplate).convertAndSend(eq("/topic/claim/" + requestId), any(ClaimDTO.class));
    }

    @Test
    void claimRequest_Fails_IfAlreadyClaimed() {
        // 1. SETUP
        UUID requestId = UUID.randomUUID();
        ResourceRequest closedRequest = new ResourceRequest();
        closedRequest.setStatus(RequestStatus.CLAIMED); // Already claimed!

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(closedRequest));

        // 2. RUN & ASSERT ERROR
        assertThrows(IllegalStateException.class, () -> {
            claimService.claimRequest(requestId, UUID.randomUUID());
        });

        // 3. VERIFY NO CHANGES SAVED
        verify(claimRepository, never()).save(any());
    }
}