package com.jobmatcher.server.bootstrap;

import com.jobmatcher.server.domain.JobCategory;
import com.jobmatcher.server.domain.JobSubcategory;
import com.jobmatcher.server.repository.JobCategoryRepository;
import com.jobmatcher.server.repository.JobSubcategoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobCategoryDataSeeder implements ApplicationRunner {

    private final JobSubcategoryRepository subcategoryRepository;
    private final JobCategoryRepository categoryRepository;

    public JobCategoryDataSeeder(JobSubcategoryRepository subcategoryRepository, JobCategoryRepository categoryRepository) {
        this.subcategoryRepository = subcategoryRepository;
        this.categoryRepository = categoryRepository;
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        if(categoryRepository.count() > 0) return;

        JobCategory digital = new JobCategory("Digital & Creative", "Creative digital services");
        JobCategory business = new JobCategory("Business, Admin & Support", "Operational support roles");
        JobCategory specialized = new JobCategory("Specialized / Niche", "Specialist services");
        JobCategory tech = new JobCategory("Tech-Heavy / Advanced", "Advanced tech work");

        categoryRepository.saveAll(List.of(digital, business, specialized, tech));

        subcategoryRepository.saveAll(List.of(
                new JobSubcategory("Web Development", "Frontend, backend, fullstack", digital),
                new JobSubcategory("Mobile App Development", "iOS, Android, React Native", digital),
                new JobSubcategory("UI/UX Design", "User interface and experience", digital),
                new JobSubcategory("Graphic Design", "Visual design and branding", digital),
                new JobSubcategory("Digital Marketing", "PPC, email, performance", digital),
                new JobSubcategory("Content Writing / Copywriting", "Writing & SEO content", digital),
                new JobSubcategory("SEO Services", "Search engine optimization", digital),
                new JobSubcategory("Video Editing / Animation", "Motion graphics and editing", digital),
                new JobSubcategory("Social Media Management", "Managing platforms", digital),
                new JobSubcategory("E-commerce Support", "Store setup/support (Shopify, Amazon, etc.)", digital),

                new JobSubcategory("Virtual Assistance", "Admin and personal assistant tasks", business),
                new JobSubcategory("Customer Support", "Support via chat, email, voice", business),
                new JobSubcategory("Data Entry", "Manual data input", business),
                new JobSubcategory("Project Management", "Manage projects and teams", business),
                new JobSubcategory("Lead Generation / Sales Support", "Sales research & outreach", business),

                new JobSubcategory("Accounting & Bookkeeping", "Finance and bookkeeping", specialized),
                new JobSubcategory("Legal Consulting", "Freelance legal help", specialized),
                new JobSubcategory("Translation / Transcription", "Language-based services", specialized),
                new JobSubcategory("Market Research", "Industry and consumer research", specialized),
                new JobSubcategory("Technical Support", "Tech help desks and support", specialized),

                new JobSubcategory("Cloud Engineering / DevOps", "AWS, CI/CD, infra", tech),
                new JobSubcategory("AI & Machine Learning", "Data models and ML tools", tech),
                new JobSubcategory("Cybersecurity", "Security audits, testing", tech),
                new JobSubcategory("Blockchain Development", "Smart contracts, crypto apps", tech),
                new JobSubcategory("AR/VR Development", "Augmented & virtual reality", tech)
        ));
    }
}
