package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.mapper.ContractMapper;
import com.jobmatcher.server.mapper.InvoiceMapper;
import com.jobmatcher.server.mapper.MilestoneMapper;
import com.jobmatcher.server.mapper.PaymentMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.InvoiceRepository;
import com.jobmatcher.server.repository.PaymentRepository;
import com.jobmatcher.server.specification.PaymentSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Transactional(rollbackFor = Exception.class)
@Service
public class PaymentServiceImpl implements IPaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMapper paymentMapper;
    private final ContractMapper contractMapper;
    private final MilestoneMapper mileStoneMapper;
    private final InvoiceMapper invoiceMapper;
    private final IInvoiceService invoiceService;
    private final JwtService jwtService;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            PaymentMapper paymentMapper,
            ContractMapper contractMapper,
            MilestoneMapper mileStoneMapper,
            InvoiceMapper invoiceMapper,
            IInvoiceService invoiceService, JwtService jwtService
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentMapper = paymentMapper;
        this.contractMapper = contractMapper;
        this.mileStoneMapper = mileStoneMapper;
        this.invoiceMapper = invoiceMapper;
        this.invoiceService = invoiceService;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<PaymentSummaryDTO> getAllPaymentsByProfileId(
            String authHeader,
            String profileId,
            PaymentFilterDTO filter,
            Pageable pageable
    ) {
        String token = authHeader.replace("Bearer ", "").trim();
        Role role = jwtService.extractRole(token);

        Page<Payment> payments = paymentRepository.findAll(PaymentSpecification.withFiltersAndRole(filter, profileId, role), pageable);
        return payments.map(payment -> {
            PaymentDetail paymentDetail = getPaymentDetail(payment.getInvoice());
            return paymentMapper.toSummaryDto(payment, paymentDetail.contractSummaryDTO(), paymentDetail.milestoneResponseDTO(), paymentDetail.invoiceSummaryDTO());
        });
    }

    @Transactional(readOnly = true)
    @Override
    public PaymentDetailDTO getPaymentByInvoiceId(UUID invoiceId) {
        Payment payment = paymentRepository.findByInvoiceId(invoiceId).orElseThrow(() ->
                new ResourceNotFoundException("Payment not found"));
        PaymentDetail paymentDetail = getPaymentDetail(payment.getInvoice());
        return paymentMapper.toDetailDto(payment, paymentDetail.contractSummaryDTO, paymentDetail.milestoneResponseDTO, paymentDetail.invoiceSummaryDTO);
    }

    @Override
    public PaymentDetailDTO createPayment(PaymentRequestDTO request) {
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId()).orElseThrow(() ->
                new ResourceNotFoundException("Invoice not found"));
        Payment payment = new Payment();
        payment.setContract(invoice.getContract());
        payment.setMilestone(invoice.getMilestone());
        payment.setInvoice(invoice);
        payment.setAmount(invoice.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaidAt(OffsetDateTime.now(ZoneOffset.UTC));
        payment.setNotes(request.getNotes());

        Payment savedPayment = paymentRepository.save(payment);

        PaymentDetail paymentDetail = getPaymentDetail(invoice);

        return paymentMapper.toDetailDto(savedPayment, paymentDetail.contractSummaryDTO(), paymentDetail.milestoneResponseDTO(), paymentDetail.invoiceSummaryDTO());
    }

    @Override
    public PaymentDetailDTO updatePayment(UUID paymentId, PaymentRequestDTO request) {
        Payment existentPayment = paymentRepository.findById(paymentId).orElseThrow(() ->
                new ResourceNotFoundException("Payment not found"));
        if (request.getStatus() != null) {
            existentPayment.setStatus(request.getStatus());
            if (request.getStatus() == PaymentStatus.PAID) {
                InvoiceRequestDTO invoiceRequestDTO = InvoiceRequestDTO.builder()
                        .status(InvoiceStatus.PAID)
                        .build();
                invoiceService.updateInvoice(request.getInvoiceId(), invoiceRequestDTO);
            }
        }
        if (request.getNotes() != null) {
            existentPayment.setNotes(request.getNotes());
        }

        Payment updatedPayment = paymentRepository.save(existentPayment);

        PaymentDetail paymentDetail = getPaymentDetail(updatedPayment.getInvoice());
        return paymentMapper.toDetailDto(updatedPayment, paymentDetail.contractSummaryDTO(), paymentDetail.milestoneResponseDTO(), paymentDetail.invoiceSummaryDTO());
    }

    @Override
    public void deletePayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() ->
                new ResourceNotFoundException("Payment not found"));
        paymentRepository.deleteById(paymentId);
        Invoice invoice = invoiceRepository.findById(payment.getInvoice().getId()).orElseThrow(() ->
                new ResourceNotFoundException("Invoice not found"));
        InvoiceRequestDTO invoiceRequestDTO = InvoiceRequestDTO.builder()
                .status(InvoiceStatus.PENDING)
                .payment(null)
                .build();
        invoiceService.updateInvoice(invoice.getId(), invoiceRequestDTO);
    }

    private record PaymentDetail(ContractSummaryDTO contractSummaryDTO, MilestoneResponseDTO milestoneResponseDTO,
                                 InvoiceSummaryDTO invoiceSummaryDTO) {
    }

    private PaymentDetail getPaymentDetail(Invoice invoice) {
        Contract contract = invoice.getContract();
        ContractSummaryDTO contractSummaryDTO = contractMapper.toSummaryDto(contract);
        Milestone milestone = invoice.getMilestone();
        MilestoneResponseDTO milestoneResponseDTO = milestone != null ? mileStoneMapper.toDto(milestone) : null;
        InvoiceSummaryDTO invoiceSummaryDTO = invoiceMapper.toSummaryDto(invoice, contractSummaryDTO, milestoneResponseDTO);
        return new PaymentDetail(contractSummaryDTO, milestoneResponseDTO, invoiceSummaryDTO);
    }
}
