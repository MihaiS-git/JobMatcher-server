package com.jobmatcher.server.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.ProposalMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.ContractRepository;
import com.jobmatcher.server.repository.FreelancerProfileRepository;
import com.jobmatcher.server.repository.ProjectRepository;
import com.jobmatcher.server.repository.ProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalServiceImplTest {
    @Mock
    ProposalRepository proposalRepository;
    @Mock
    ProjectRepository projectRepository;
    @Mock
    FreelancerProfileRepository freelancerRepository;
    @Mock
    ProposalMapper proposalMapper;
    @Mock
    ContractRepository contractRepository;
    @Mock
    IProjectService projectService;

    @InjectMocks
    ProposalServiceImpl service;

    UUID proposalId;
    UUID projectId;
    UUID freelancerId;
    ProposalDetailDTO detailDTO;
    ProposalSummaryDTO summaryDTO;

    @BeforeEach
    void setUp() {
        proposalId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        freelancerId = UUID.randomUUID();

        detailDTO = ProposalDetailDTO.builder().id(proposalId).build();
        summaryDTO = ProposalSummaryDTO.builder().id(proposalId).build();
    }

    @Test
    void getProposalById_existing_returnsDetailDto() {
        Proposal proposal = new Proposal();
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(proposalMapper.toDetailDto(proposal)).thenReturn(detailDTO);

        ProposalDetailDTO result = service.getProposalById(proposalId);

        assertSame(detailDTO, result);
    }

    @Test
    void getProposalById_notFound_throwsResourceNotFound() {
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getProposalById(proposalId));
    }

    @Test
    void getProposalByFreelancerIdAndProjectId_returnsDtoOrNull() {
        Proposal proposal = new Proposal();
        ProposalDetailDTO dto = detailDTO;
        when(proposalRepository.findByFreelancerIdAndProjectId(freelancerId, projectId))
                .thenReturn(Optional.of(proposal));
        when(proposalMapper.toDetailDto(proposal)).thenReturn(dto);

        ProposalDetailDTO result = service.getProposalByFreelancerIdAndProjectId(freelancerId, projectId);
        assertSame(dto, result);

        // test null branch
        when(proposalRepository.findByFreelancerIdAndProjectId(freelancerId, projectId))
                .thenReturn(Optional.empty());
        assertNull(service.getProposalByFreelancerIdAndProjectId(freelancerId, projectId));
    }

    @Test
    void createProposal_success() {
        ProposalRequestDTO requestDTO = ProposalRequestDTO.builder()
                .freelancerId(freelancerId)
                .projectId(projectId)
                .build();

        Project project = new Project();
        project.setStatus(ProjectStatus.OPEN);
        FreelancerProfile freelancer = new FreelancerProfile();
        Proposal proposal = new Proposal();
        Proposal savedProposal = new Proposal();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(freelancerRepository.findById(freelancerId)).thenReturn(Optional.of(freelancer));
        when(proposalRepository.existsByFreelancerIdAndProjectId(freelancerId, projectId)).thenReturn(false);
        when(proposalMapper.toEntity(requestDTO, project, freelancer)).thenReturn(proposal);
        when(proposalRepository.save(proposal)).thenReturn(savedProposal);
        when(proposalMapper.toSummaryDto(savedProposal)).thenReturn(summaryDTO);

        ProposalSummaryDTO result = service.createProposal(requestDTO);
        assertSame(summaryDTO, result);
    }

    @Test
    void createProposal_projectNotFound_throws() {
        ProposalRequestDTO requestDTO = ProposalRequestDTO.builder()
                .freelancerId(freelancerId)
                .projectId(projectId)
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.createProposal(requestDTO));
    }

    @Test
    void createProposal_projectNotOpen_throws() {
        ProposalRequestDTO requestDTO = ProposalRequestDTO.builder()
                .freelancerId(freelancerId)
                .projectId(projectId)
                .build();
        Project project = new Project();
        project.setStatus(ProjectStatus.IN_PROGRESS);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        assertThrows(IllegalStateException.class, () -> service.createProposal(requestDTO));
    }

    @Test
    void createProposal_freelancerNotFound_throws() {
        ProposalRequestDTO requestDTO = ProposalRequestDTO.builder()
                .freelancerId(freelancerId)
                .projectId(projectId)
                .build();
        Project project = new Project();
        project.setStatus(ProjectStatus.OPEN);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(freelancerRepository.findById(freelancerId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.createProposal(requestDTO));
    }

    @Test
    void createProposal_alreadyExists_throws() {
        ProposalRequestDTO requestDTO = ProposalRequestDTO.builder()
                .freelancerId(freelancerId)
                .projectId(projectId)
                .build();
        Project project = new Project();
        project.setStatus(ProjectStatus.OPEN);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(freelancerRepository.findById(freelancerId)).thenReturn(Optional.of(new FreelancerProfile()));
        when(proposalRepository.existsByFreelancerIdAndProjectId(freelancerId, projectId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.createProposal(requestDTO));
    }

    @Test
    void updateProposalById_success() {
        Proposal existent = new Proposal();
        ProposalRequestDTO dto = ProposalRequestDTO.builder()
                .coverLetter("cover")
                .amount(BigDecimal.TEN)
                .build();
        Proposal updated = new Proposal();

        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(existent));
        when(proposalRepository.save(existent)).thenReturn(updated);
        when(proposalMapper.toDetailDto(updated)).thenReturn(detailDTO);

        ProposalDetailDTO result = service.updateProposalById(proposalId, dto);
        assertSame(detailDTO, result);
        assertEquals("cover", existent.getCoverLetter());
        assertEquals(BigDecimal.TEN, existent.getAmount());
    }

    @Test
    void updateProposalById_notFound_throws() {
        ProposalRequestDTO dto = ProposalRequestDTO.builder().build();
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateProposalById(proposalId, dto));
    }

    @Test
    void updateProposalStatusById_accept_properlyCreatesContract() {
        Proposal proposal = new Proposal();
        Project project = new Project();
        CustomerProfile customer = new CustomerProfile();
        User newUser = new User();
        newUser.setId(UUID.randomUUID());
        newUser.setFirstName("John");
        newUser.setLastName("Doe");
        customer.setUser(newUser);
        project.setCustomer(customer);
        project.setTitle("Title");
        FreelancerProfile freelancer = new FreelancerProfile();
        User newUser2 = new User();
        newUser2.setId(UUID.randomUUID());
        newUser2.setFirstName("John");
        newUser2.setLastName("Doe");
        freelancer.setUser(newUser2);
        proposal.setProject(project);
        proposal.setFreelancer(freelancer);
        proposal.setAmount(BigDecimal.valueOf(100));
        proposal.setPlannedStartDate(OffsetDateTime.now());
        proposal.setPlannedEndDate(OffsetDateTime.now().plusDays(10));
        proposal.setId(proposalId);

        ProposalStatusRequestDTO requestDTO = ProposalStatusRequestDTO.builder()
                .status(ProposalStatus.ACCEPTED)
                .build();

        Contract savedContract = new Contract();
        savedContract.setId(UUID.randomUUID());

        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(contractRepository.save(any(Contract.class))).thenReturn(savedContract);
        when(proposalRepository.save(any(Proposal.class))).thenReturn(proposal);
        when(projectRepository.save(project)).thenReturn(project);
        when(freelancerRepository.save(freelancer)).thenReturn(freelancer);
        when(proposalMapper.toDetailDto(proposal)).thenReturn(detailDTO);

        service.updateProposalStatusById(proposalId, requestDTO);

        assertEquals(ProposalStatus.ACCEPTED, proposal.getStatus());
        assertEquals(proposal.getContract(), savedContract);
        assertEquals(project.getAcceptedProposal(), proposal);
        assertEquals(project.getContract(), savedContract);
        assertEquals(project.getFreelancer(), freelancer);
        assertTrue(freelancer.getContracts().contains(savedContract));
    }

    @Test
    void updateProposalStatusById_rejectsOrWithdrawsOrPending() {
        Proposal proposal = new Proposal();
        Project project = new Project();
        proposal.setProject(project);

        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(proposal)).thenReturn(proposal);
        when(proposalMapper.toDetailDto(proposal)).thenReturn(detailDTO);

        // REJECTED
        service.updateProposalStatusById(proposalId, ProposalStatusRequestDTO.builder()
                .status(ProposalStatus.REJECTED).build());
        assertEquals(ProposalStatus.REJECTED, proposal.getStatus());

        // WITHDRAWN
        service.updateProposalStatusById(proposalId, ProposalStatusRequestDTO.builder()
                .status(ProposalStatus.WITHDRAWN).build());
        assertEquals(ProposalStatus.WITHDRAWN, proposal.getStatus());
        verify(projectService).updateProjectStatus(eq(project.getId()), any());

        // PENDING
        service.updateProposalStatusById(proposalId, ProposalStatusRequestDTO.builder()
                .status(ProposalStatus.PENDING).build());
        assertEquals(ProposalStatus.PENDING, proposal.getStatus());
    }

    @Test
    void updateProposalById_allFieldsUpdated() {
        Proposal existent = new Proposal();
        ProposalRequestDTO dto = ProposalRequestDTO.builder()
                .coverLetter("cover")
                .amount(BigDecimal.TEN)
                .penaltyAmount(BigDecimal.ONE)
                .bonusAmount(BigDecimal.valueOf(5))
                .estimatedDuration(15)
                .notes("Some notes")
                .plannedStartDate(OffsetDateTime.of(2025, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .plannedEndDate(OffsetDateTime.of(2025, 10, 15, 0, 0, 0, 0, ZoneOffset.UTC))
                .actualStartDate(OffsetDateTime.of(2025, 10, 2, 0, 0, 0, 0, ZoneOffset.UTC))
                .actualEndDate(OffsetDateTime.of(2025, 10, 16, 0, 0, 0, 0, ZoneOffset.UTC))
                .build();

        Proposal updated = new Proposal();

        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(existent));
        when(proposalRepository.save(existent)).thenReturn(updated);
        when(proposalMapper.toDetailDto(updated)).thenReturn(detailDTO);

        ProposalDetailDTO result = service.updateProposalById(proposalId, dto);

        assertSame(detailDTO, result);
        assertEquals("cover", existent.getCoverLetter());
        assertEquals(BigDecimal.TEN, existent.getAmount());
        assertEquals(BigDecimal.ONE, existent.getPenaltyAmount());
        assertEquals(BigDecimal.valueOf(5), existent.getBonusAmount());
        assertEquals(15, existent.getEstimatedDuration());
        assertEquals("Some notes", existent.getNotes());
        assertEquals(OffsetDateTime.of(2025, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC), existent.getPlannedStartDate());
        assertEquals(OffsetDateTime.of(2025, 10, 15, 0, 0, 0, 0, ZoneOffset.UTC), existent.getPlannedEndDate());
        assertEquals(OffsetDateTime.of(2025, 10, 2, 0, 0, 0, 0, ZoneOffset.UTC), existent.getActualStartDate());
        assertEquals(OffsetDateTime.of(2025, 10, 16, 0, 0, 0, 0, ZoneOffset.UTC), existent.getActualEndDate());
    }

    @Test
    void deleteProposalById_success() {
        Proposal proposal = new Proposal();
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        service.deleteProposalById(proposalId);
        verify(proposalRepository).delete(proposal);
    }

    @Test
    void deleteProposalById_withContract_throws() {
        Proposal proposal = new Proposal();
        proposal.setContract(new Contract());
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        assertThrows(IllegalStateException.class, () -> service.deleteProposalById(proposalId));
    }

    @Test
    void deleteProposalById_notFound_throws() {
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteProposalById(proposalId));
    }

    // Paging methods can be tested minimally to ensure mapper coverage
    @Test
    void getProposalsByProjectId_withStatus() {
        Proposal proposal = new Proposal();
        Page<Proposal> page = new PageImpl<>(List.of(proposal));
        when(proposalRepository.findByProjectIdAndStatus(eq(projectId), any(Pageable.class), eq(ProposalStatus.PENDING)))
                .thenReturn(page);
        when(proposalMapper.toSummaryDto(proposal)).thenReturn(summaryDTO);

        Page<ProposalSummaryDTO> result = service.getProposalsByProjectId(projectId, Pageable.unpaged(), ProposalStatus.PENDING);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProposalsByProjectId_noStatus() {
        Proposal proposal = new Proposal();
        Page<Proposal> page = new PageImpl<>(List.of(proposal));
        when(proposalRepository.findByProjectId(eq(projectId), any(Pageable.class))).thenReturn(page);
        when(proposalMapper.toSummaryDto(proposal)).thenReturn(summaryDTO);

        Page<ProposalSummaryDTO> result = service.getProposalsByProjectId(projectId, Pageable.unpaged(), null);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProposalsByFreelancerId_withStatus() {
        Proposal proposal = new Proposal();
        Page<Proposal> page = new PageImpl<>(List.of(proposal));
        when(proposalRepository.findByFreelancerIdAndStatus(eq(freelancerId), any(Pageable.class), eq(ProposalStatus.PENDING)))
                .thenReturn(page);
        when(proposalMapper.toSummaryDto(proposal)).thenReturn(summaryDTO);

        Page<ProposalSummaryDTO> result = service.getProposalsByFreelancerId(freelancerId, Pageable.unpaged(), ProposalStatus.PENDING);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProposalsByFreelancerId_noStatus() {
        Proposal proposal = new Proposal();
        Page<Proposal> page = new PageImpl<>(List.of(proposal));
        when(proposalRepository.findByFreelancerId(eq(freelancerId), any(Pageable.class))).thenReturn(page);
        when(proposalMapper.toSummaryDto(proposal)).thenReturn(summaryDTO);

        Page<ProposalSummaryDTO> result = service.getProposalsByFreelancerId(freelancerId, Pageable.unpaged(), null);
        assertEquals(1, result.getTotalElements());
    }

}