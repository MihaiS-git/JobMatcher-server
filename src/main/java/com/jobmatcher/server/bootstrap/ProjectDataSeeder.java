package com.jobmatcher.server.bootstrap;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
@Order(4)
public class ProjectDataSeeder implements ApplicationRunner {

    private final ProjectRepository projectRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final JobSubcategoryRepository jobSubcategoryRepository;

    public ProjectDataSeeder(ProjectRepository projectRepository, CustomerProfileRepository customerProfileRepository, FreelancerProfileRepository freelancerProfileRepository, JobCategoryRepository jobCategoryRepository, JobSubcategoryRepository jobSubcategoryRepository) {
        this.projectRepository = projectRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.freelancerProfileRepository = freelancerProfileRepository;
        this.jobCategoryRepository = jobCategoryRepository;
        this.jobSubcategoryRepository = jobSubcategoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (projectRepository.count() > 0) return;

        System.out.println("Seeding projects...");
        List<CustomerProfile> customers = customerProfileRepository.findAll();
        List<FreelancerProfile> freelancers = freelancerProfileRepository.findAll();
        List<JobCategory> categories = jobCategoryRepository.findAll();
        List<JobSubcategory> subcategories = jobSubcategoryRepository.findAll();

        Project project1 = new Project();
        project1.setCustomer(customers.get(0));
        project1.setFreelancer(freelancers.get(0));
        project1.setTitle("Website Development");
        project1.setDescription("Develop a responsive website for our new product.");
        project1.setBudget(java.math.BigDecimal.valueOf(5000));
        project1.setPaymentType(PaymentType.UPON_COMPLETION);
        project1.setDeadline(LocalDate.parse("2025-12-31"));
        project1.setCategory(categories.get(0));
        project1.setSubcategories(Set.of(subcategories.get(0)));
        project1.setStatus(ProjectStatus.COMPLETED);

        Project project2 = new Project();
        project2.setCustomer(customers.get(0));
        project2.setFreelancer(freelancers.get(1));
        project2.setTitle("Website Development & Design");
        project2.setDescription("Develop a responsive website for our new product with modern design.");
        project2.setBudget(java.math.BigDecimal.valueOf(7000));
        project2.setPaymentType(PaymentType.UPON_COMPLETION);
        project2.setDeadline(LocalDate.parse("2025-12-31"));
        project2.setCategory(categories.get(1));
        project2.setSubcategories(Set.of(subcategories.get(2)));
        project2.setStatus(ProjectStatus.COMPLETED);

        Project project3 = new Project();
        project3.setCustomer(customers.get(0));
        project3.setFreelancer(freelancers.get(2));
        project3.setTitle("Website Development & Design + SEO");
        project3.setDescription("Develop a responsive website for our new product with modern design and SEO optimization.");
        project3.setBudget(java.math.BigDecimal.valueOf(9000));
        project3.setPaymentType(PaymentType.UPON_COMPLETION);
        project3.setDeadline(LocalDate.parse("2025-12-31"));
        project3.setCategory(categories.get(3));
        project3.setSubcategories(Set.of(subcategories.get(2), subcategories.get(4)));
        project3.setStatus(ProjectStatus.COMPLETED);

        Project project4 = new Project();
        project4.setCustomer(customers.get(0));
        project4.setFreelancer(freelancers.get(2));
        project4.setTitle("Website Development & Design + SEO + Marketing");
        project4.setDescription("Develop a responsive website for our new product with modern design, SEO optimization, and marketing strategies.");
        project4.setBudget(java.math.BigDecimal.valueOf(12000));
        project4.setPaymentType(PaymentType.UPON_COMPLETION);
        project4.setDeadline(LocalDate.parse("2025-12-31"));
        project4.setCategory(categories.get(1));
        project4.setSubcategories(Set.of(subcategories.get(2), subcategories.get(4)));
        project4.setStatus(ProjectStatus.COMPLETED);

        Project project5 = new Project();
        project5.setCustomer(customers.get(1));
        project5.setFreelancer(freelancers.get(0));
        project5.setTitle("Website Development");
        project5.setDescription("Develop a responsive website for our new product.");
        project5.setBudget(java.math.BigDecimal.valueOf(5000));
        project5.setPaymentType(PaymentType.UPON_COMPLETION);
        project5.setDeadline(LocalDate.parse("2025-12-31"));
        project5.setCategory(categories.get(1));
        project5.setSubcategories(Set.of(subcategories.get(1), subcategories.get(3)));
        project5.setStatus(ProjectStatus.COMPLETED);

        Project project6 = new Project();
        project6.setCustomer(customers.get(2));
        project6.setFreelancer(freelancers.get(1));
        project6.setTitle("Website Development + SEO");
        project6.setDescription("Develop a responsive website for our new product with SEO optimization.");
        project6.setBudget(java.math.BigDecimal.valueOf(8000));
        project6.setPaymentType(PaymentType.UPON_COMPLETION);
        project6.setDeadline(LocalDate.parse("2025-12-31"));
        project6.setCategory(categories.get(3));
        project6.setSubcategories(Set.of(subcategories.get(0)));
        project6.setStatus(ProjectStatus.COMPLETED);

        projectRepository.saveAll(List.of(project1, project2, project3, project4, project5, project6));
        System.out.println("Projects seeded.");


        for (int i = 0; i < 1000; i++) {
            Project p = new Project();
            CustomerProfile customer = customers.get(i % customers.size());
            FreelancerProfile freelancer = freelancers.get(i % freelancers.size());
            JobCategory category = categories.get(i % categories.size());
            JobSubcategory subcategory = subcategories.get(i % subcategories.size());

            p.setCustomer(customer);
            p.setFreelancer(freelancer);
            p.setTitle("Generated Project " + (i + 1));
            p.setDescription("Auto-generated project for load testing #" + (i + 1));
            p.setBudget(java.math.BigDecimal.valueOf(1000 + (i % 100) * 50));
            p.setPaymentType(PaymentType.UPON_COMPLETION);
            p.setDeadline(LocalDate.now().plusDays(30 + (i % 60)));
            p.setCategory(category);
            p.setSubcategories(Set.of(subcategory));
            p.setStatus(ProjectStatus.OPEN);

            projectRepository.save(p);
        }
    }

}
