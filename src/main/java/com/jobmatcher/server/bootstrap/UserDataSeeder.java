package com.jobmatcher.server.bootstrap;

import com.jobmatcher.server.domain.*;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.repository.*;
import com.jobmatcher.server.service.ISkillService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

        List<Address> addressList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Address address = new Address();
            address.setStreet(i + " Main St");
            address.setCity("Metropolis");
            address.setState("NY");
            address.setPostalCode("10001");
            address.setCountry("USA");
            addressList.add(address);
        }

        for (int i = 0; i < 1000; i++) {
            Address address = addressList.get(i);

            User user = new User();
            user.setEmail("user" + i + "@jobmatcher.com");
            user.setPassword(passwordEncoder.encode("Password!23"));
            user.setRole(i % 2 == 0 ? Role.STAFF : Role.CUSTOMER);
            user.setPhone("1234567890");
            user.setAddress(address);
            user.setFirstName("John");
            user.setLastName("Doe");
            userRepository.save(user);
        }

        List<User> allUsers = userRepository.findAll();
        List<JobSubcategory> jobSubcategories = jobSubcategoryRepository.findAll();
        List<Language> languages = languageRepository.findAll();

        Skill skill1 = skillService.findOrCreateByName("Java");
        Skill skill2 = skillService.findOrCreateByName("Spring Boot");

        int count = 0;
        for (User user : allUsers) {
            if (user.getRole() == Role.STAFF) {
                FreelancerProfile freelancerProfile = new FreelancerProfile();
                freelancerProfile.setUser(user);
                freelancerProfile.setUsername(user.getFirstName().toLowerCase() + "_" + count);
                freelancerProfile.setAbout("This is a freelancer profile for " + user.getFirstName());
                freelancerProfile.setLanguages(Set.of(
                        languages.get(10),
                        languages.get(14)
                ));
                freelancerProfile.setHeadline("Freelancer " + user.getFirstName());
                freelancerProfile.setHourlyRate(20.0 + Math.random() * 80.0);
                freelancerProfile.setAvailableForHire(true);
                freelancerProfile.setJobSubcategories(Set.of(
                        jobSubcategories.get(0),
                        jobSubcategories.get(1)
                ));
                freelancerProfile.setSkills(Set.of(skill1, skill2));
                freelancerProfile.setExperienceLevel(ExperienceLevel.JUNIOR);
                freelancerProfileRepository.save(freelancerProfile);
            } else if (user.getRole() == Role.CUSTOMER) {
                CustomerProfile customerProfile = new CustomerProfile();
                customerProfile.setUser(user);
                customerProfile.setUsername(user.getFirstName().toLowerCase() + "_" + count);
                customerProfile.setAbout("This is a customer profile for " + user.getFirstName());
                customerProfile.setLanguages(Set.of(
                        languages.get(10),
                        languages.get(14)
                ));
                customerProfile.setCompany(user.getFirstName() + " Corp");
                customerProfileRepository.save(customerProfile);
            }
            count++;
        }
        System.out.println("Seeded users, addresses & profiles.");

    }
}
