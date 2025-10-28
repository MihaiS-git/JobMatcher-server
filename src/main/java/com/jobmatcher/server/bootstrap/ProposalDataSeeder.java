package com.jobmatcher.server.bootstrap;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@Order(5)
public class ProposalDataSeeder implements ApplicationRunner {

    private final ProjectRepository projectRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final ProposalRepository proposalRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoicesRepository;
    private final PaymentRepository paymentRepository;

    public ProposalDataSeeder(ProjectRepository projectRepository, FreelancerProfileRepository freelancerProfileRepository, ProposalRepository proposalRepository, ContractRepository contractRepository, InvoiceRepository invoicesRepository, PaymentRepository paymentRepository) {
        this.projectRepository = projectRepository;
        this.freelancerProfileRepository = freelancerProfileRepository;
        this.proposalRepository = proposalRepository;
        this.contractRepository = contractRepository;
        this.invoicesRepository = invoicesRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {

        List<Project> projects = projectRepository.findAll();
        List<FreelancerProfile> freelancers = freelancerProfileRepository.findAll();

        if (projects.isEmpty() || freelancers.isEmpty()) {
            System.out.println("No projects or freelancers found. Skipping proposal seeding...");
            return;
        }

        if (proposalRepository.count() > 0 || contractRepository.count() > 0 || invoicesRepository.count() > 0 || paymentRepository.count() > 0) {
            System.out.println("Proposals, Contracts, Invoices or Payments already exist. Skipping seeding...");
            return;
        }

        System.out.println("Seeding proposals...");
        Proposal proposal1 = new Proposal();
        proposal1.setProject(projects.getFirst());
        proposal1.setFreelancer(freelancers.getFirst());
        proposal1.setCoverLetter("I am very interested in your project and believe I have the skills to complete it successfully.");
        proposal1.setAmount(java.math.BigDecimal.valueOf(4500));
        proposal1.setEstimatedDuration(30);
        proposal1.setNotes("Looking forward to working with you.");
        proposal1.setPlannedStartDate(OffsetDateTime.now().minusDays(160));
        proposal1.setPlannedEndDate(OffsetDateTime.now().minusDays(130));
        proposal1.setActualStartDate(OffsetDateTime.now().minusDays(160));
        proposal1.setActualEndDate(OffsetDateTime.now().minusDays(130));
        proposal1.setStatus(ProposalStatus.ACCEPTED);
        projects.getFirst().setAcceptedProposal(proposal1);

        Proposal proposal2 = new Proposal();
        proposal2.setProject(projects.getFirst());
        proposal2.setFreelancer(freelancers.get(1));
        proposal2.setCoverLetter("I have extensive experience in similar projects and can deliver high-quality results.");
        proposal2.setAmount(java.math.BigDecimal.valueOf(4800));
        proposal2.setEstimatedDuration(25);
        proposal2.setNotes("Please consider my proposal.");
        proposal2.setPlannedStartDate(OffsetDateTime.now().plusDays(2));
        proposal2.setPlannedEndDate(OffsetDateTime.now().plusDays(32));
        proposal2.setActualStartDate(OffsetDateTime.now().plusDays(2));
        proposal2.setActualEndDate(OffsetDateTime.now().plusDays(32));
        proposal2.setStatus(ProposalStatus.REJECTED);

        Proposal proposal3 = new Proposal();
        proposal3.setProject(projects.getFirst());
        proposal3.setFreelancer(freelancers.get(2));
        proposal3.setCoverLetter("I am confident that I can exceed your expectations for this project.");
        proposal3.setAmount(java.math.BigDecimal.valueOf(4700));
        proposal3.setEstimatedDuration(28);
        proposal3.setNotes("Excited to collaborate on this project.");
        proposal3.setPlannedStartDate(OffsetDateTime.now().plusDays(2));
        proposal3.setPlannedEndDate(OffsetDateTime.now().plusDays(32));
        proposal3.setActualStartDate(OffsetDateTime.now().plusDays(2));
        proposal3.setActualEndDate(OffsetDateTime.now().plusDays(32));
        proposal3.setStatus(ProposalStatus.REJECTED);

        Proposal proposal4 = new Proposal();
        proposal4.setProject(projects.get(1));
        proposal4.setFreelancer(freelancers.getFirst());
        proposal4.setCoverLetter("I specialize in website development and can create a stunning site for you.");
        proposal4.setAmount(java.math.BigDecimal.valueOf(6500));
        proposal4.setEstimatedDuration(35);
        proposal4.setNotes("Ready to start immediately.");
        proposal4.setPlannedStartDate(OffsetDateTime.now().minusDays(130));
        proposal4.setPlannedEndDate(OffsetDateTime.now().minusDays(130).plusDays(35));
        proposal4.setActualStartDate(OffsetDateTime.now().minusDays(130));
        proposal4.setActualEndDate(OffsetDateTime.now().minusDays(130).plusDays(35));
        proposal4.setStatus(ProposalStatus.ACCEPTED);
        projects.get(1).setAcceptedProposal(proposal4);

        Proposal proposal5 = new Proposal();
        proposal5.setProject(projects.get(2));
        proposal5.setFreelancer(freelancers.getFirst());
        proposal5.setCoverLetter("My design skills will ensure your website stands out.");
        proposal5.setAmount(java.math.BigDecimal.valueOf(6800));
        proposal5.setEstimatedDuration(40);
        proposal5.setNotes("Let's create something amazing together.");
        proposal5.setPlannedStartDate(OffsetDateTime.now().minusDays(90));
        proposal5.setPlannedEndDate(OffsetDateTime.now().minusDays(90).plusDays(40));
        proposal5.setActualStartDate(OffsetDateTime.now().minusDays(90));
        proposal5.setActualEndDate(OffsetDateTime.now().minusDays(90).plusDays(40));
        proposal5.setStatus(ProposalStatus.ACCEPTED);
        projects.get(2).setAcceptedProposal(proposal5);

        Proposal proposal6 = new Proposal();
        proposal6.setProject(projects.get(3));
        proposal6.setFreelancer(freelancers.getFirst());
        proposal6.setCoverLetter("I have a proven track record in SEO optimization and web design.");
        proposal6.setAmount(java.math.BigDecimal.valueOf(8500));
        proposal6.setEstimatedDuration(45);
        proposal6.setNotes("Eager to contribute to your project's success.");
        proposal6.setPlannedStartDate(OffsetDateTime.now().minusDays(50));
        proposal6.setPlannedEndDate(OffsetDateTime.now().minusDays(50).plusDays(45));
        proposal6.setActualStartDate(OffsetDateTime.now().minusDays(50));
        proposal6.setActualEndDate(OffsetDateTime.now().minusDays(50).plusDays(45));
        proposal6.setStatus(ProposalStatus.ACCEPTED);
        projects.get(3).setAcceptedProposal(proposal6);

        Proposal proposal7 = new Proposal();
        proposal7.setProject(projects.get(4));
        proposal7.setFreelancer(freelancers.getFirst());
        proposal7.setCoverLetter("With my marketing expertise, I can help boost your online presence.");
        proposal7.setAmount(java.math.BigDecimal.valueOf(11000));
        proposal7.setEstimatedDuration(50);
        proposal7.setNotes("Looking forward to a fruitful collaboration.");
        proposal7.setPlannedStartDate(OffsetDateTime.now().minusDays(200));
        proposal7.setPlannedEndDate(OffsetDateTime.now().minusDays(150));
        proposal7.setActualStartDate(OffsetDateTime.now().minusDays(200));
        proposal7.setActualEndDate(OffsetDateTime.now().minusDays(150));
        proposal7.setStatus(ProposalStatus.ACCEPTED);
        projects.get(4).setAcceptedProposal(proposal7);

        Proposal proposal8 = new Proposal();
        proposal8.setProject(projects.get(5));
        proposal8.setFreelancer(freelancers.get(1));
        proposal8.setCoverLetter("I can deliver a high-quality website with effective SEO strategies.");
        proposal8.setAmount(java.math.BigDecimal.valueOf(7200));
        proposal8.setEstimatedDuration(38);
        proposal8.setNotes("Excited to take on this project.");
        proposal8.setPlannedStartDate(OffsetDateTime.now().minusDays(120));
        proposal8.setPlannedEndDate(OffsetDateTime.now().minusDays(82));
        proposal8.setActualStartDate(OffsetDateTime.now().minusDays(120));
        proposal8.setActualEndDate(OffsetDateTime.now().minusDays(82));
        proposal8.setStatus(ProposalStatus.ACCEPTED);
        projects.get(5).setAcceptedProposal(proposal8);

        Proposal proposal9 = new Proposal();
        proposal9.setProject(projects.get(5));
        proposal9.setFreelancer(freelancers.get(2));
        proposal9.setCoverLetter("My skills in web development and SEO will ensure your project's success.");
        proposal9.setAmount(java.math.BigDecimal.valueOf(7500));
        proposal9.setEstimatedDuration(42);
        proposal9.setNotes("Ready to deliver exceptional results.");
        proposal9.setPlannedStartDate(OffsetDateTime.now().plusDays(2));
        proposal9.setPlannedEndDate(OffsetDateTime.now().plusDays(32));
        proposal9.setActualStartDate(OffsetDateTime.now().plusDays(2));
        proposal9.setActualEndDate(OffsetDateTime.now().plusDays(32));
        proposal9.setStatus(ProposalStatus.REJECTED);

        Proposal proposal10 = new Proposal();
        proposal10.setProject(projects.get(4));
        proposal10.setFreelancer(freelancers.get(2));
        proposal10.setCoverLetter("I bring a unique blend of design and marketing skills to the table.");
        proposal10.setAmount(java.math.BigDecimal.valueOf(11500));
        proposal10.setEstimatedDuration(48);
        proposal10.setNotes("Let's make this project a success together.");
        proposal10.setPlannedStartDate(OffsetDateTime.now().plusDays(2));
        proposal10.setPlannedEndDate(OffsetDateTime.now().plusDays(32));
        proposal10.setActualStartDate(OffsetDateTime.now().plusDays(2));
        proposal10.setActualEndDate(OffsetDateTime.now().plusDays(32));
        proposal10.setStatus(ProposalStatus.REJECTED);

        Proposal proposal11 = new Proposal();
        proposal11.setProject(projects.get(2));
        proposal11.setFreelancer(freelancers.get(1));
        proposal11.setCoverLetter("I am passionate about web design and SEO, and I am eager to contribute.");
        proposal11.setAmount(java.math.BigDecimal.valueOf(7000));
        proposal11.setEstimatedDuration(36);
        proposal11.setNotes("Looking forward to hearing from you.");
        proposal11.setPlannedStartDate(OffsetDateTime.now().plusDays(2));
        proposal11.setPlannedEndDate(OffsetDateTime.now().plusDays(32));
        proposal11.setActualStartDate(OffsetDateTime.now().plusDays(2));
        proposal11.setActualEndDate(OffsetDateTime.now().plusDays(32));
        proposal11.setStatus(ProposalStatus.REJECTED);

        Proposal proposal12 = new Proposal();
        proposal12.setProject(projects.get(3));
        proposal12.setFreelancer(freelancers.get(1));
        proposal12.setCoverLetter("My expertise in SEO and marketing will help elevate your project.");
        proposal12.setAmount(java.math.BigDecimal.valueOf(9000));
        proposal12.setEstimatedDuration(44);
        proposal12.setNotes("Excited to collaborate.");
        proposal12.setPlannedStartDate(OffsetDateTime.now().plusDays(2));
        proposal12.setPlannedEndDate(OffsetDateTime.now().plusDays(32));
        proposal12.setActualStartDate(OffsetDateTime.now().plusDays(2));
        proposal12.setActualEndDate(OffsetDateTime.now().plusDays(32));
        proposal12.setStatus(ProposalStatus.REJECTED);

        Proposal proposal13 = new Proposal();
        proposal13.setProject(projects.get(5));
        proposal13.setFreelancer(freelancers.getFirst());
        proposal13.setCoverLetter("I have a strong background in web development and SEO.");
        proposal13.setAmount(java.math.BigDecimal.valueOf(7800));
        proposal13.setEstimatedDuration(40);
        proposal13.setNotes("Ready to start working on your project.");
        proposal13.setPlannedStartDate(OffsetDateTime.now().plusDays(2));
        proposal13.setPlannedEndDate(OffsetDateTime.now().plusDays(32));
        proposal13.setActualStartDate(OffsetDateTime.now().plusDays(2));
        proposal13.setActualEndDate(OffsetDateTime.now().plusDays(32));
        proposal13.setStatus(ProposalStatus.REJECTED);

        proposalRepository.saveAll(List.of(proposal1, proposal2, proposal3, proposal4, proposal5, proposal6, proposal7, proposal8, proposal9, proposal10, proposal11, proposal12, proposal13));
        System.out.println("Proposals seeded successfully.");
        System.out.println("-------------------------------------");

        System.out.println("Contract seeding...");

        Contract contract1 = new Contract();
        contract1.setProposal(proposal1);
        contract1.setProject(proposal1.getProject());
        contract1.setCustomer(proposal1.getProject().getCustomer());
        contract1.setFreelancer(proposal1.getFreelancer());
        contract1.setTitle(proposal1.getProject().getTitle());
        contract1.setDescription(proposal1.getProject().getDescription());
        contract1.setAmount(proposal1.getAmount());
        contract1.setStartDate(OffsetDateTime.now().minusDays(160));
        contract1.setEndDate(OffsetDateTime.now().minusDays(160).plusDays(proposal1.getEstimatedDuration()));
        contract1.setSignedAt(OffsetDateTime.now().minusDays(162));
        contract1.setStatus(ContractStatus.COMPLETED);
        contract1.setCompletedAt(OffsetDateTime.now().minusDays(160).plusDays(proposal1.getEstimatedDuration()));
        contract1.setTotalPaid(proposal1.getAmount());
        contract1.setRemainingBalance(proposal1.getAmount().subtract(proposal1.getAmount()));

        Contract contract2 = new Contract();
        contract2.setProposal(proposal4);
        contract2.setProject(proposal4.getProject());
        contract2.setCustomer(proposal4.getProject().getCustomer());
        contract2.setFreelancer(proposal4.getFreelancer());
        contract2.setTitle(proposal4.getProject().getTitle());
        contract2.setDescription(proposal4.getProject().getDescription());
        contract2.setAmount(proposal4.getAmount());
        contract2.setStartDate(OffsetDateTime.now().minusDays(130));
        contract2.setEndDate(OffsetDateTime.now().minusDays(130).plusDays(35));
        contract2.setSignedAt(OffsetDateTime.now().minusDays(132));
        contract2.setStatus(ContractStatus.COMPLETED);
        contract2.setCompletedAt(OffsetDateTime.now().minusDays(130).plusDays(35));
        contract2.setTotalPaid(proposal4.getAmount());
        contract2.setRemainingBalance(proposal4.getAmount().subtract(proposal4.getAmount()));

        Contract contract3 = new Contract();
        contract3.setProposal(proposal5);
        contract3.setProject(proposal5.getProject());
        contract3.setCustomer(proposal5.getProject().getCustomer());
        contract3.setFreelancer(proposal5.getFreelancer());
        contract3.setTitle(proposal5.getProject().getTitle());
        contract3.setDescription(proposal5.getProject().getDescription());
        contract3.setAmount(proposal5.getAmount());
        contract3.setStartDate(OffsetDateTime.now().minusDays(90));
        contract3.setEndDate(OffsetDateTime.now().minusDays(90).plusDays(40));
        contract3.setSignedAt(OffsetDateTime.now().minusDays(93));
        contract3.setStatus(ContractStatus.COMPLETED);
        contract3.setCompletedAt(OffsetDateTime.now().minusDays(90).plusDays(40));
        contract3.setTotalPaid(proposal5.getAmount());
        contract3.setRemainingBalance(proposal5.getAmount().subtract(proposal5.getAmount()));

        Contract contract4 = new Contract();
        contract4.setProposal(proposal6);
        contract4.setProject(proposal6.getProject());
        contract4.setCustomer(proposal6.getProject().getCustomer());
        contract4.setFreelancer(proposal6.getFreelancer());
        contract4.setTitle(proposal6.getProject().getTitle());
        contract4.setDescription(proposal6.getProject().getDescription());
        contract4.setAmount(proposal6.getAmount());
        contract4.setStartDate(OffsetDateTime.now().minusDays(50));
        contract4.setEndDate(OffsetDateTime.now().minusDays(50).plusDays(45));
        contract4.setSignedAt(OffsetDateTime.now().minusDays(55));
        contract4.setStatus(ContractStatus.COMPLETED);
        contract4.setCompletedAt(OffsetDateTime.now().minusDays(50).plusDays(45));
        contract4.setTotalPaid(proposal6.getAmount());
        contract4.setRemainingBalance(proposal6.getAmount().subtract(proposal6.getAmount()));

        Contract contract5 = new Contract();
        contract5.setProposal(proposal7);
        contract5.setProject(proposal7.getProject());
        contract5.setCustomer(proposal7.getProject().getCustomer());
        contract5.setFreelancer(proposal7.getFreelancer());
        contract5.setTitle(proposal7.getProject().getTitle());
        contract5.setDescription(proposal7.getProject().getDescription());
        contract5.setAmount(proposal7.getAmount());
        contract5.setStartDate(OffsetDateTime.now().minusDays(200));
        contract5.setEndDate(OffsetDateTime.now().minusDays(150));
        contract5.setSignedAt(OffsetDateTime.now().minusDays(203));
        contract5.setStatus(ContractStatus.COMPLETED);
        contract5.setCompletedAt(OffsetDateTime.now().minusDays(150));
        contract5.setTotalPaid(proposal7.getAmount());
        contract5.setRemainingBalance(proposal7.getAmount().subtract(proposal7.getAmount()));

        Contract contract6 = new Contract();
        contract6.setProposal(proposal8);
        contract6.setProject(proposal8.getProject());
        contract6.setCustomer(proposal8.getProject().getCustomer());
        contract6.setFreelancer(proposal8.getFreelancer());
        contract6.setTitle(proposal8.getProject().getTitle());
        contract6.setDescription(proposal8.getProject().getDescription());
        contract6.setAmount(proposal8.getAmount());
        contract6.setStartDate(OffsetDateTime.now().minusDays(120));
        contract6.setEndDate(OffsetDateTime.now().minusDays(82));
        contract6.setSignedAt(OffsetDateTime.now().minusDays(130));
        contract6.setStatus(ContractStatus.COMPLETED);
        contract6.setCompletedAt(OffsetDateTime.now().minusDays(82));
        contract6.setTotalPaid(proposal8.getAmount());
        contract6.setRemainingBalance(proposal8.getAmount().subtract(proposal8.getAmount()));

        contractRepository.saveAll(List.of(contract1, contract2, contract3, contract4, contract5, contract6));
        System.out.println("Contracts seeded successfully.");
        System.out.println("-------------------------------------");

        System.out.println("Invoices seeding...");

        Invoice invoice1 = new Invoice();
        invoice1.setContract(contract1);
        invoice1.setAmount(contract1.getAmount());
        invoice1.setIssuedAt(contract1.getCompletedAt());
        invoice1.setDueDate(contract1.getCompletedAt().plusDays(15));
        invoice1.setStatus(InvoiceStatus.PAID);

        Invoice invoice2 = new Invoice();
        invoice2.setContract(contract2);
        invoice2.setAmount(contract2.getAmount());
        invoice2.setIssuedAt(contract2.getCompletedAt());
        invoice2.setDueDate(contract2.getCompletedAt().plusDays(1));
        invoice2.setStatus(InvoiceStatus.PAID);

        Invoice invoice3 = new Invoice();
        invoice3.setContract(contract3);
        invoice3.setAmount(contract3.getAmount());
        invoice3.setIssuedAt(contract3.getCompletedAt());
        invoice3.setDueDate(contract3.getCompletedAt().plusDays(5));
        invoice3.setStatus(InvoiceStatus.PAID);

        Invoice invoice4 = new Invoice();
        invoice4.setContract(contract4);
        invoice4.setAmount(contract4.getAmount());
        invoice4.setIssuedAt(contract4.getCompletedAt());
        invoice4.setDueDate(contract4.getCompletedAt().plusDays(2));
        invoice4.setStatus(InvoiceStatus.PAID);

        Invoice invoice5 = new Invoice();
        invoice5.setContract(contract5);
        invoice5.setAmount(contract5.getAmount());
        invoice5.setIssuedAt(contract5.getCompletedAt());
        invoice5.setDueDate(contract5.getCompletedAt().plusDays(1));
        invoice5.setStatus(InvoiceStatus.PAID);

        Invoice invoice6 = new Invoice();
        invoice6.setContract(contract6);
        invoice6.setAmount(contract6.getAmount());
        invoice6.setIssuedAt(contract6.getCompletedAt());
        invoice6.setDueDate(contract6.getCompletedAt().plusDays(15));
        invoice6.setStatus(InvoiceStatus.PAID);

        invoicesRepository.saveAll(List.of(invoice1, invoice2, invoice3, invoice4, invoice5, invoice6));
        System.out.println("Invoices seeded successfully.");
        System.out.println("-------------------------------------");

        System.out.println("Payments seeding...");

        Payment payment1 = new Payment();
        payment1.setContract(invoice1.getContract());
        payment1.setInvoice(invoice1);
        payment1.setAmount(invoice1.getAmount());
        payment1.setPaidAt(invoice1.getDueDate());
        payment1.setNotes("Payment for invoice 1");
        invoice1.setPayment(payment1);
        invoice1.getContract().setPayment(payment1);

        Payment payment2 = new Payment();
        payment2.setContract(invoice2.getContract());
        payment2.setInvoice(invoice2);
        payment2.setAmount(invoice2.getAmount());
        payment2.setPaidAt(invoice2.getDueDate());
        payment2.setNotes("Payment for invoice 2");
        invoice2.setPayment(payment2);
        invoice2.getContract().setPayment(payment2);

        Payment payment3 = new Payment();
        payment3.setContract(invoice3.getContract());
        payment3.setInvoice(invoice3);
        payment3.setAmount(invoice3.getAmount());
        payment3.setPaidAt(invoice3.getDueDate());
        payment3.setNotes("Payment for invoice 3");
        invoice3.setPayment(payment3);
        invoice3.getContract().setPayment(payment3);

        Payment payment4 = new Payment();
        payment4.setContract(invoice4.getContract());
        payment4.setInvoice(invoice4);
        payment4.setAmount(invoice4.getAmount());
        payment4.setPaidAt(invoice4.getDueDate());
        payment4.setNotes("Payment for invoice 4");
        invoice4.setPayment(payment4);
        invoice4.getContract().setPayment(payment4);

        Payment payment5 = new Payment();
        payment5.setContract(invoice5.getContract());
        payment5.setInvoice(invoice5);
        payment5.setAmount(invoice5.getAmount());
        payment5.setPaidAt(invoice5.getDueDate());
        payment5.setNotes("Payment for invoice 5");
        invoice5.setPayment(payment5);
        invoice5.getContract().setPayment(payment5);

        Payment payment6 = new Payment();
        payment6.setContract(invoice6.getContract());
        payment6.setInvoice(invoice6);
        payment6.setAmount(invoice6.getAmount());
        payment6.setPaidAt(invoice6.getDueDate());
        payment6.setNotes("Payment for invoice 6");
        invoice6.setPayment(payment6);
        invoice6.getContract().setPayment(payment6);

        paymentRepository.saveAll(List.of(payment1, payment2, payment3, payment4, payment5, payment6));
        System.out.println("Payments seeded successfully.");

    }
}

