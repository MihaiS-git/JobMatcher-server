package com.jobmatcher.server.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.MilestoneMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.ContractRepository;
import com.jobmatcher.server.repository.MilestoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceImplTest {
    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private MilestoneMapper milestoneMapper;

    @Mock
    private IContractService contractService;

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private MilestoneServiceImpl milestoneService;

    private Milestone milestone;
    private MilestoneResponseDTO milestoneDTO;
    private Contract contract;

    @BeforeEach
    void setUp() {
        contract = new Contract();
        contract.setId(UUID.randomUUID());
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setMilestones(new HashSet<>());

        milestone = new Milestone();
        milestone.setId(UUID.randomUUID());
        milestone.setContract(contract);
        milestone.setStatus(MilestoneStatus.PENDING);

        milestoneDTO = MilestoneResponseDTO.builder().id(milestone.getId()).build();
    }

    @Test
    void getMilestonesByContractId_shouldReturnPagedResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Milestone> page = new PageImpl<>(List.of(milestone), pageable, 1);

        when(milestoneRepository.findByContractId(eq(contract.getId()), any(Pageable.class)))
                .thenReturn(page);
        when(milestoneMapper.toDto(milestone)).thenReturn(milestoneDTO);

        Page<MilestoneResponseDTO> result = milestoneService.getMilestonesByContractId(contract.getId(), pageable);

        assertThat(result.getContent()).containsExactly(milestoneDTO);
    }


    @Test
    void getMilestoneById_found() {
        when(milestoneRepository.findById(milestone.getId())).thenReturn(Optional.of(milestone));
        when(milestoneMapper.toDto(milestone)).thenReturn(milestoneDTO);

        MilestoneResponseDTO result = milestoneService.getMilestoneById(milestone.getId());

        assertThat(result).isEqualTo(milestoneDTO);
    }

    @Test
    void getMilestoneById_notFound() {
        UUID id = UUID.randomUUID();
        when(milestoneRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> milestoneService.getMilestoneById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Milestone not found");
    }

    @Test
    void createMilestone_success() {
        MilestoneRequestDTO request = MilestoneRequestDTO.builder()
                .contractId(contract.getId()).title("Title").build();
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(milestoneMapper.toEntity(request, contract)).thenReturn(milestone);
        when(milestoneRepository.save(milestone)).thenReturn(milestone);
        when(milestoneMapper.toDto(milestone)).thenReturn(milestoneDTO);

        MilestoneResponseDTO result = milestoneService.createMilestone(request);

        assertThat(result).isEqualTo(milestoneDTO);
        assertThat(contract.getMilestones()).contains(milestone);
    }

    @Test
    void createMilestone_contractCompletedOrCancelled_shouldThrow() {
        MilestoneRequestDTO request = MilestoneRequestDTO.builder()
                .contractId(contract.getId()).build();
        contract.setStatus(ContractStatus.COMPLETED);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> milestoneService.createMilestone(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot add milestone");
    }

    @Test
    void createMilestone_contractNotFound_shouldThrow() {
        UUID contractId = UUID.randomUUID();
        MilestoneRequestDTO request = MilestoneRequestDTO.builder()
                .contractId(contractId).build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> milestoneService.createMilestone(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Contract not found");
    }


    @Test
    void updateMilestone_updatesFields() {
        UUID id = milestone.getId();
        when(milestoneRepository.findById(id)).thenReturn(Optional.of(milestone));
        when(milestoneRepository.save(any())).thenReturn(milestone);
        when(milestoneMapper.toDto(milestone)).thenReturn(milestoneDTO);

        MilestoneRequestDTO request = MilestoneRequestDTO.builder()
                .title("Updated")
                .description("Desc")
                .amount(BigDecimal.TEN)
                .priority(Priority.LOW)
                .build();

        MilestoneResponseDTO result = milestoneService.updateMilestone(id, request);

        assertThat(result).isEqualTo(milestoneDTO);
        assertThat(milestone.getTitle()).isEqualTo("Updated");
        assertThat(milestone.getDescription()).isEqualTo("Desc");
        assertThat(milestone.getAmount()).isEqualTo(BigDecimal.TEN);
        assertThat(milestone.getPriority()).isEqualTo(Priority.LOW);
    }

    @Test
    void updateMilestoneStatusById_pending_shouldUpdateContract() {
        milestone.setStatus(MilestoneStatus.PENDING);
        milestone.setContract(contract);
        contract.setMilestones(Set.of(milestone));

        when(milestoneRepository.findById(milestone.getId())).thenReturn(Optional.of(milestone));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(milestoneRepository.save(milestone)).thenReturn(milestone);
        when(milestoneMapper.toDto(milestone)).thenReturn(milestoneDTO);

        MilestoneStatusRequestDTO requestDTO = MilestoneStatusRequestDTO.builder()
                .status(MilestoneStatus.PENDING)
                .build();

        MilestoneResponseDTO result = milestoneService.updateMilestoneStatusById(milestone.getId(), requestDTO);

        assertThat(result).isEqualTo(milestoneDTO);
        verify(contractService, times(1)).updateContractStatusById(eq(contract.getId()), any());
    }

    @Test
    void updateMilestoneStatusById_inProgress_shouldUpdateContract() {
        milestone.setStatus(MilestoneStatus.PENDING);
        milestone.setContract(contract);
        contract.setMilestones(Set.of(milestone));

        when(milestoneRepository.findById(milestone.getId())).thenReturn(Optional.of(milestone));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(milestoneRepository.save(milestone)).thenReturn(milestone);
        when(milestoneMapper.toDto(milestone)).thenReturn(milestoneDTO);

        MilestoneStatusRequestDTO requestDTO = MilestoneStatusRequestDTO.builder()
                .status(MilestoneStatus.IN_PROGRESS)
                .build();

        MilestoneResponseDTO result = milestoneService.updateMilestoneStatusById(milestone.getId(), requestDTO);

        assertThat(result).isEqualTo(milestoneDTO);
        verify(contractService, times(1)).updateContractStatusById(eq(contract.getId()), any());
    }

    @Test
    void updateMilestoneStatusById_cancelled_shouldUpdateContract() {
        milestone.setStatus(MilestoneStatus.PENDING);
        milestone.setContract(contract);
        contract.setMilestones(Set.of(milestone));

        when(milestoneRepository.findById(milestone.getId())).thenReturn(Optional.of(milestone));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(milestoneRepository.save(milestone)).thenReturn(milestone);
        when(milestoneMapper.toDto(milestone)).thenReturn(milestoneDTO);

        MilestoneStatusRequestDTO requestDTO = MilestoneStatusRequestDTO.builder()
                .status(MilestoneStatus.CANCELLED)
                .build();

        MilestoneResponseDTO result = milestoneService.updateMilestoneStatusById(milestone.getId(), requestDTO);

        assertThat(result).isEqualTo(milestoneDTO);
        verify(contractService, times(1)).updateContractStatusById(eq(contract.getId()), any());
    }


    @Test
    void updateMilestoneStatusById_completed_paid_allCompleted() {
        Milestone m2 = new Milestone();
        m2.setStatus(MilestoneStatus.COMPLETED);
        contract.setMilestones(Set.of(milestone, m2));
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        when(milestoneRepository.findById(milestone.getId())).thenReturn(Optional.of(milestone));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(milestoneRepository.save(milestone)).thenReturn(milestone);
        when(milestoneMapper.toDto(milestone)).thenReturn(milestoneDTO);

        for (MilestoneStatus status : List.of(MilestoneStatus.COMPLETED, MilestoneStatus.PAID)) {
            MilestoneStatusRequestDTO requestDTO = MilestoneStatusRequestDTO.builder().status(status).build();
            MilestoneResponseDTO result = milestoneService.updateMilestoneStatusById(milestone.getId(), requestDTO);
            assertThat(result).isEqualTo(milestoneDTO);
        }
    }

    @Test
    void updateMilestoneStatusById_nullStatus_shouldThrowNPE() {
        milestone.setContract(contract);
        when(milestoneRepository.findById(milestone.getId())).thenReturn(Optional.of(milestone));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> milestoneService.updateMilestoneStatusById(
                milestone.getId(),
                MilestoneStatusRequestDTO.builder().status(null).build()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void updateMilestone_milestoneNotFound_shouldThrow() {
        UUID milestoneId = UUID.randomUUID();
        MilestoneRequestDTO request = MilestoneRequestDTO.builder().build();

        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> milestoneService.updateMilestone(milestoneId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Milestone not found");
    }

    @Test
    void updateMilestone_allFieldsSet_shouldUpdateCorrectly() {
        UUID id = milestone.getId();

        // Fill all fields in the request
        MilestoneRequestDTO request = MilestoneRequestDTO.builder()
                .title("New Title")
                .description("Desc")
                .amount(BigDecimal.TEN)
                .penaltyAmount(BigDecimal.ONE)
                .bonusAmount(BigDecimal.valueOf(2))
                .estimatedDuration(5)
                .notes("Notes")
                .plannedStartDate(LocalDate.now())
                .plannedEndDate(LocalDate.now().plusDays(1))
                .actualStartDate(LocalDate.now())
                .actualEndDate(LocalDate.now().plusDays(1))
                .priority(Priority.HIGH)
                .build();

        when(milestoneRepository.findById(id)).thenReturn(Optional.of(milestone));
        when(milestoneRepository.save(any())).thenReturn(milestone);
        when(milestoneMapper.toDto(milestone)).thenReturn(milestoneDTO);

        MilestoneResponseDTO result = milestoneService.updateMilestone(id, request);

        assertThat(result).isEqualTo(milestoneDTO);
        assertThat(milestone.getTitle()).isEqualTo("New Title");
        assertThat(milestone.getDescription()).isEqualTo("Desc");
        assertThat(milestone.getAmount()).isEqualTo(BigDecimal.TEN);
        assertThat(milestone.getPenaltyAmount()).isEqualTo(BigDecimal.ONE);
        assertThat(milestone.getBonusAmount()).isEqualTo(BigDecimal.valueOf(2));
        assertThat(milestone.getEstimatedDuration()).isEqualTo(5);
        assertThat(milestone.getNotes()).isEqualTo("Notes");
        assertThat(milestone.getPlannedStartDate()).isEqualTo(LocalDate.now());
        assertThat(milestone.getPlannedEndDate()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(milestone.getActualStartDate()).isEqualTo(LocalDate.now());
        assertThat(milestone.getActualEndDate()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(milestone.getPriority()).isEqualTo(Priority.HIGH);
    }


    @Test
    void deleteMilestone_success() {
        when(milestoneRepository.findById(milestone.getId())).thenReturn(Optional.of(milestone));
        doNothing().when(milestoneRepository).delete(milestone);

        milestoneService.deleteMilestone(milestone.getId());

        verify(milestoneRepository).delete(milestone);
    }

    @Test
    void deleteMilestone_notFound() {
        UUID id = UUID.randomUUID();
        when(milestoneRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> milestoneService.deleteMilestone(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}