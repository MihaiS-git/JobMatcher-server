package com.jobmatcher.server.bootstrap;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.repository.*;
import com.jobmatcher.server.service.ISkillService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@Order(3)
public class UserDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final JobSubcategoryRepository jobSubcategoryRepository;
    private final LanguageRepository languageRepository;

    private final ISkillService skillService;
    private final PasswordEncoder passwordEncoder;

    public UserDataSeeder(
            UserRepository userRepository,
            AddressRepository addressRepository,
            FreelancerProfileRepository freelancerProfileRepository,
            CustomerProfileRepository customerProfileRepository,
            JobSubcategoryRepository jobSubcategoryRepository,
            LanguageRepository languageRepository,
            ISkillService skillService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.freelancerProfileRepository = freelancerProfileRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.jobSubcategoryRepository = jobSubcategoryRepository;
        this.languageRepository = languageRepository;
        this.skillService = skillService;
        this.passwordEncoder = passwordEncoder;
    }

    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.count() > 0) return;

        System.out.println("User addresses seeding ...");

        Address address1 = new Address();
        address1.setStreet("123 Main St");
        address1.setCity("Metropolis");
        address1.setState("NY");
        address1.setPostalCode("10001");
        address1.setCountry("USA");

        Address address2 = new Address();
        address2.setStreet("123 Main St");
        address2.setCity("Metropolis");
        address2.setState("NY");
        address2.setPostalCode("10001");
        address2.setCountry("USA");

        Address address3 = new Address();
        address3.setStreet("123 Main St");
        address3.setCity("Metropolis");
        address3.setState("NY");
        address3.setPostalCode("10001");
        address3.setCountry("USA");

        Address address4 = new Address();
        address4.setStreet("123 Main St");
        address4.setCity("Metropolis");
        address4.setState("NY");
        address4.setPostalCode("10001");
        address4.setCountry("USA");

        Address address5 = new Address();
        address5.setStreet("123 Main St");
        address5.setCity("Metropolis");
        address5.setState("NY");
        address5.setPostalCode("10001");
        address5.setCountry("USA");

        Address address6 = new Address();
        address6.setStreet("123 Main St");
        address6.setCity("Metropolis");
        address6.setState("NY");
        address6.setPostalCode("10001");
        address6.setCountry("USA");

        System.out.println("Seeding user data...");
        User user1 = new User();
        user1.setEmail("user1@jobmatcher.com");
        user1.setPassword(passwordEncoder.encode("Password!23"));
        user1.setRole(Role.STAFF);
        user1.setPhone("1234567890");
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setAddress(address1);

        User user2 = new User();
        user2.setEmail("user2@jobmatcher.com");
        user2.setPassword(passwordEncoder.encode("Password!23"));
        user2.setRole(Role.STAFF);
        user2.setPhone("1234567890");
        user2.setFirstName("John2");
        user2.setLastName("Doe");
        user2.setAddress(address2);

        User user3 = new User();
        user3.setEmail("user3@jobmatcher.com");
        user3.setPassword(passwordEncoder.encode("Password!23"));
        user3.setRole(Role.STAFF);
        user3.setPhone("1234567890");
        user3.setFirstName("John3");
        user3.setLastName("Doe");
        user3.setAddress(address3);

        User user4 = new User();
        user4.setEmail("user4@jobmatcher.com");
        user4.setPassword(passwordEncoder.encode("Password!23"));
        user4.setRole(Role.CUSTOMER);
        user4.setPhone("1234567890");
        user4.setFirstName("Jane");
        user4.setLastName("Doe");
        user4.setAddress(address4);

        User user5 = new User();
        user5.setEmail("user5@jobmatcher.com");
        user5.setPassword(passwordEncoder.encode("Password!23"));
        user5.setRole(Role.CUSTOMER);
        user5.setPhone("1234567890");
        user5.setFirstName("Jane");
        user5.setLastName("Doe");
        user5.setAddress(address5);

        User user6 = new User();
        user6.setEmail("user6@jobmatcher.com");
        user6.setPassword(passwordEncoder.encode("Password!23"));
        user6.setRole(Role.CUSTOMER);
        user6.setPhone("1234567890");
        user6.setFirstName("Jane");
        user6.setLastName("Doe");
        user6.setAddress(address6);

        userRepository.saveAll(List.of(user1, user2, user3, user4, user5, user6));
        System.out.println("User and addresses data seeded.");

        System.out.println("-------------------------------------");


        System.out.println("Seed freelancer profiles data");

        List<JobSubcategory> jobSubcategories = jobSubcategoryRepository.findAll();
        List<Language> languages = languageRepository.findAll();

        Skill skill1 = skillService.findOrCreateByName("Java");
        Skill skill2 = skillService.findOrCreateByName("Spring Boot");
        Skill skill3 = skillService.findOrCreateByName("React");
        Skill skill4 = skillService.findOrCreateByName("Project Management");
        Skill skill5 = skillService.findOrCreateByName("Graphic Design");
        Skill skill6 = skillService.findOrCreateByName("SEO");
        Skill skill7 = skillService.findOrCreateByName("Content Writing");


        FreelancerProfile freelancer1 = new FreelancerProfile();
        freelancer1.setUser(user1);
        freelancer1.setUsername("freelancer_john");
        freelancer1.setAbout("Passionate web developer with 2 years of experience.");
        freelancer1.setLanguages(Set.of(
                languages.get(10),
                languages.get(14)
        ));
        freelancer1.setHeadline("Experienced Web Developer");
        freelancer1.setHourlyRate(45.0);
        freelancer1.setAvailableForHire(true);
        freelancer1.setJobSubcategories(Set.of(
                jobSubcategories.get(0),
                jobSubcategories.get(1)
        ));
        freelancer1.setSkills(Set.of(skill1, skill2));
        freelancer1.setExperienceLevel(ExperienceLevel.JUNIOR);

        FreelancerProfile freelancer2 = new FreelancerProfile();
        freelancer2.setUser(user2);
        freelancer2.setUsername("freelancer_john2");
        freelancer2.setAbout("Passionate web developer with 5 years of experience.");
        freelancer2.setLanguages(Set.of(
                languages.get(11),
                languages.get(15)
        ));
        freelancer2.setHeadline("Experienced Web Developer");
        freelancer2.setHourlyRate(50.0);
        freelancer2.setAvailableForHire(true);
        freelancer2.setJobSubcategories(Set.of(
                jobSubcategories.get(2),
                jobSubcategories.get(3)
        ));
        freelancer2.setSkills(Set.of(skill3, skill4));
        freelancer2.setExperienceLevel(ExperienceLevel.MID);

        FreelancerProfile freelancer3 = new FreelancerProfile();
        freelancer3.setUser(user3);
        freelancer3.setUsername("freelancer_john3");
        freelancer3.setAbout("Passionate web developer with 6 years of experience.");
        freelancer3.setLanguages(Set.of(
                languages.get(12),
                languages.get(16)
        ));
        freelancer3.setHeadline("Fullstack Web Developer");
        freelancer3.setHourlyRate(65.0);
        freelancer3.setAvailableForHire(true);
        freelancer3.setJobSubcategories(Set.of(
                jobSubcategories.get(4),
                jobSubcategories.get(5)
        ));
        freelancer3.setSkills(Set.of(skill5, skill6));
        freelancer3.setExperienceLevel(ExperienceLevel.SENIOR);

        freelancerProfileRepository.saveAll(List.of(freelancer1, freelancer2, freelancer3));
        System.out.println("Freelancer profiles data seeded.");

        System.out.println("-------------------------------------");

        System.out.println("Customer profiles data seeded.");
        CustomerProfile customer1 = new CustomerProfile();
        customer1.setUser(user4);
        customer1.setUsername("customer_jane");
        customer1.setAbout("Leading company in tech industry.");
        customer1.setLanguages(Set.of(
                languages.get(10),
                languages.get(14)
        ));
        customer1.setCompany("Tech Solutions");

        CustomerProfile customer2 = new CustomerProfile();
        customer2.setUser(user5);
        customer2.setUsername("customer_jane2");
        customer2.setAbout("Leading company in tech industry.");
        customer2.setLanguages(Set.of(
                languages.get(10),
                languages.get(14)
        ));
        customer2.setCompany("Tech Solutions Ltd");

        CustomerProfile customer3 = new CustomerProfile();
        customer3.setUser(user6);
        customer3.setUsername("customer_jane3");
        customer3.setAbout("Leading company in tech industry.");
        customer3.setLanguages(Set.of(
                languages.get(10),
                languages.get(14)
        ));
        customer3.setCompany("Tech Solutions Ltd & Co");

        customerProfileRepository.saveAll(List.of(customer1, customer2, customer3));
        System.out.println("Customer profiles data seeded.");
        System.out.println("-------------------------------------");

    }
}
