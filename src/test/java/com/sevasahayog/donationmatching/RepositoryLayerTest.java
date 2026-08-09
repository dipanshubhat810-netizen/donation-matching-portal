package com.sevasahayog.donationmatching;

import com.sevasahayog.donationmatching.entity.AuditRecord;
import com.sevasahayog.donationmatching.entity.Category;
import com.sevasahayog.donationmatching.entity.Condition;
import com.sevasahayog.donationmatching.entity.Donation;
import com.sevasahayog.donationmatching.entity.DonationPhoto;
import com.sevasahayog.donationmatching.entity.DonationStatus;
import com.sevasahayog.donationmatching.entity.Match;
import com.sevasahayog.donationmatching.entity.MatchStatus;
import com.sevasahayog.donationmatching.entity.QuantityUnit;
import com.sevasahayog.donationmatching.entity.Requirement;
import com.sevasahayog.donationmatching.entity.RequirementStatus;
import com.sevasahayog.donationmatching.entity.Role;
import com.sevasahayog.donationmatching.entity.Transaction;
import com.sevasahayog.donationmatching.entity.TransactionStatus;
import com.sevasahayog.donationmatching.entity.Urgency;
import com.sevasahayog.donationmatching.entity.User;
import com.sevasahayog.donationmatching.repository.AuditRecordRepository;
import com.sevasahayog.donationmatching.repository.DonationPhotoRepository;
import com.sevasahayog.donationmatching.repository.DonationRepository;
import com.sevasahayog.donationmatching.repository.MatchRepository;
import com.sevasahayog.donationmatching.repository.RequirementRepository;
import com.sevasahayog.donationmatching.repository.TransactionRepository;
import com.sevasahayog.donationmatching.repository.UserRepository;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RepositoryLayerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonationPhotoRepository donationPhotoRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void userRepositoryFindsByEmailAndChecksExistence() {
        User user = userRepository.save(User.builder()
                .name("Alice Donor")
                .email("alice@example.com")
                .password("placeholder-hash")
                .role(Role.DONOR)
                .build());

        Optional<User> found = userRepository.findByEmail("alice@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(user.getId());
        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void donationRepositorySupportsOwnershipStatusAndSearchFilters() {
        User donor = userRepository.save(User.builder()
                .name("Bob Donor").email("bob@example.com")
                .password("placeholder-hash").role(Role.DONOR).build());

        Donation donation = donationRepository.save(Donation.builder()
                .donor(donor)
                .title("Textbooks")
                .description("Used school textbooks for classes 6-8.")
                .category(Category.EDUCATION)
                .quantity(new BigDecimal("20"))
                .quantityUnit(QuantityUnit.PIECES)
                .condition(Condition.GOOD)
                .city("Mumbai")
                .build());

        assertThat(donation.getStatus()).isEqualTo(DonationStatus.SUBMITTED);
        assertThat(donationRepository.findByIdAndDonorId(donation.getId(), donor.getId())).isPresent();
        assertThat(donationRepository.findByIdAndDonorId(donation.getId(), donor.getId() + 999L)).isEmpty();

        assertThat(donationRepository.findAllByDonorId(donor.getId(), PageRequest.of(0, 10)))
                .contains(donation);
        assertThat(donationRepository.findAllByStatus(DonationStatus.SUBMITTED, PageRequest.of(0, 10)))
                .contains(donation);
        assertThat(donationRepository.findAllByStatusAndCategory(DonationStatus.SUBMITTED, Category.EDUCATION, PageRequest.of(0, 10)))
                .contains(donation);
        assertThat(donationRepository.findAllByStatusAndCategoryAndCity(
                        DonationStatus.SUBMITTED, Category.EDUCATION, "Mumbai", PageRequest.of(0, 10)))
                .contains(donation);
        assertThat(donationRepository.findAllByStatusAndCategoryAndCity(
                        DonationStatus.SUBMITTED, Category.EDUCATION, "Delhi", PageRequest.of(0, 10)))
                .doesNotContain(donation);
    }

    @Test
    void donationPhotoRepositoryFindsPhotosByDonation() {
        User donor = userRepository.save(User.builder()
                .name("Carol Donor").email("carol@example.com")
                .password("placeholder-hash").role(Role.DONOR).build());
        Donation donation = donationRepository.save(Donation.builder()
                .donor(donor).title("Blankets").description("Woollen blankets.")
                .category(Category.HOUSEHOLD).quantity(new BigDecimal("5"))
                .quantityUnit(QuantityUnit.PIECES).condition(Condition.NEW).city("Pune")
                .build());

        donationPhotoRepository.save(DonationPhoto.builder()
                .donation(donation).storageKey("donations/1/blankets.jpg")
                .contentType("image/jpeg").fileSize(1024L).build());
        donationPhotoRepository.save(DonationPhoto.builder()
                .donation(donation).storageKey("donations/1/blankets-2.jpg")
                .contentType("image/jpeg").fileSize(2048L).build());

        List<DonationPhoto> photos = donationPhotoRepository.findAllByDonationId(donation.getId());
        assertThat(photos).hasSize(2);
    }

    @Test
    void requirementRepositorySupportsOwnershipStatusAndSearchFilters() {
        User receiver = userRepository.save(User.builder()
                .name("School NGO").email("school@example.com")
                .password("placeholder-hash").role(Role.RECEIVER).build());

        Requirement requirement = requirementRepository.save(Requirement.builder()
                .receiver(receiver)
                .title("Need textbooks")
                .description("We need textbooks for underprivileged students.")
                .category(Category.EDUCATION)
                .quantityRequired(new BigDecimal("20"))
                .quantityUnit(QuantityUnit.PIECES)
                .city("Mumbai")
                .urgency(Urgency.HIGH)
                .build());

        assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.SUBMITTED);
        assertThat(requirementRepository.findByIdAndReceiverId(requirement.getId(), receiver.getId())).isPresent();
        assertThat(requirementRepository.findByIdAndReceiverId(requirement.getId(), receiver.getId() + 999L)).isEmpty();

        assertThat(requirementRepository.findAllByReceiverId(receiver.getId(), PageRequest.of(0, 10)))
                .contains(requirement);
        assertThat(requirementRepository.findAllByStatus(RequirementStatus.SUBMITTED, PageRequest.of(0, 10)))
                .contains(requirement);
        assertThat(requirementRepository.findAllByStatusAndCategoryAndCity(
                        RequirementStatus.SUBMITTED, Category.EDUCATION, "Mumbai", PageRequest.of(0, 10)))
                .contains(requirement);
    }

    @Test
    void matchRepositorySupportsLookupsByDonationRequirementStatusAndTopScoring() {
        User donor = userRepository.save(User.builder()
                .name("Dan Donor").email("dan@example.com")
                .password("placeholder-hash").role(Role.DONOR).build());
        User receiver = userRepository.save(User.builder()
                .name("Hostel NGO").email("hostel@example.com")
                .password("placeholder-hash").role(Role.RECEIVER).build());

        Donation donationA = donationRepository.save(Donation.builder()
                .donor(donor).title("Bed A").description("Single cot.")
                .category(Category.FURNITURE).quantity(new BigDecimal("1"))
                .quantityUnit(QuantityUnit.PIECES).condition(Condition.GOOD).city("Pune")
                .build());
        Donation donationB = donationRepository.save(Donation.builder()
                .donor(donor).title("Bed B").description("Another single cot.")
                .category(Category.FURNITURE).quantity(new BigDecimal("1"))
                .quantityUnit(QuantityUnit.PIECES).condition(Condition.GOOD).city("Pune")
                .build());
        Requirement requirement = requirementRepository.save(Requirement.builder()
                .receiver(receiver).title("Need a cot").description("Hostel needs a cot.")
                .category(Category.FURNITURE).quantityRequired(new BigDecimal("1"))
                .quantityUnit(QuantityUnit.PIECES).city("Pune").urgency(Urgency.MEDIUM)
                .build());

        Match low = matchRepository.save(Match.builder()
                .donation(donationA).requirement(requirement).score(new BigDecimal("60.00")).build());
        Match high = matchRepository.save(Match.builder()
                .donation(donationB).requirement(requirement).score(new BigDecimal("90.00")).build());

        assertThat(matchRepository.findByDonationId(donationA.getId())).containsExactly(low);
        assertThat(matchRepository.findByRequirementId(requirement.getId()))
                .containsExactlyInAnyOrder(low, high);
        assertThat(matchRepository.findByRequirementIdAndStatus(requirement.getId(), MatchStatus.SUGGESTED))
                .containsExactlyInAnyOrder(low, high);
        assertThat(matchRepository.findByDonationIdAndStatus(donationA.getId(), MatchStatus.SUGGESTED))
                .containsExactly(low);
        assertThat(matchRepository.existsByDonationIdAndRequirementId(donationA.getId(), requirement.getId())).isTrue();
        assertThat(matchRepository.existsByDonationIdAndRequirementId(donationA.getId(), requirement.getId() + 999L)).isFalse();

        List<Match> top = matchRepository.findTop5ByRequirementIdAndStatusOrderByScoreDesc(
                requirement.getId(), MatchStatus.SUGGESTED);
        assertThat(top).hasSize(2);
        assertThat(top.get(0).getId()).isEqualTo(high.getId());
        assertThat(top.get(1).getId()).isEqualTo(low.getId());

        assertThat(matchRepository.findAllByStatus(MatchStatus.SUGGESTED, PageRequest.of(0, 10)))
                .containsExactlyInAnyOrder(low, high);
    }

    @Test
    void transactionRepositoryFindsByMatchAndStatus() {
        User donor = userRepository.save(User.builder()
                .name("Eve Donor").email("eve@example.com")
                .password("placeholder-hash").role(Role.DONOR).build());
        User receiver = userRepository.save(User.builder()
                .name("NGO Receiver").email("ngo@example.com")
                .password("placeholder-hash").role(Role.RECEIVER).build());
        Donation donation = donationRepository.save(Donation.builder()
                .donor(donor).title("Fridge").description("Working refrigerator.")
                .category(Category.HOUSEHOLD).quantity(new BigDecimal("1"))
                .quantityUnit(QuantityUnit.PIECES).condition(Condition.GOOD).city("Nashik")
                .build());
        Requirement requirement = requirementRepository.save(Requirement.builder()
                .receiver(receiver).title("Fridge needed").description("Community kitchen fridge.")
                .category(Category.HOUSEHOLD).quantityRequired(new BigDecimal("1"))
                .quantityUnit(QuantityUnit.PIECES).city("Nashik").urgency(Urgency.LOW)
                .build());
        Match match = matchRepository.save(Match.builder()
                .donation(donation).requirement(requirement).score(new BigDecimal("80.00")).build());

        Transaction transaction = transactionRepository.save(Transaction.builder()
                .match(match).donor(donor).receiver(receiver).build());

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(transactionRepository.findByMatchId(match.getId())).isPresent();
        assertThat(transactionRepository.findAllByStatus(TransactionStatus.PENDING, PageRequest.of(0, 10)))
                .contains(transaction);
        assertThat(transactionRepository.findByDonorId(donor.getId())).contains(transaction);
        assertThat(transactionRepository.findByReceiverId(receiver.getId())).contains(transaction);
    }

    @Test
    void auditRecordRepositoryRetrievesByEntityAndActor() {
        User actor = userRepository.save(User.builder()
                .name("Admin User").email("admin@example.com")
                .password("placeholder-hash").role(Role.ADMIN).build());

        auditRecordRepository.save(AuditRecord.builder()
                .actor(actor).action("APPROVE").entityType("Donation").entityId("42")
                .details("approved").build());
        auditRecordRepository.save(AuditRecord.builder()
                .actor(actor).action("REJECT").entityType("Requirement").entityId("7")
                .details("rejected").build());

        assertThat(auditRecordRepository.findByEntityTypeAndEntityId("Donation", "42"))
                .hasSize(1);
        assertThat(auditRecordRepository.findByActorId(actor.getId())).hasSize(2);
        assertThat(auditRecordRepository.findAllByOrderByCreatedAtDesc()).hasSize(2);
    }

    @Test
    void duplicateUserEmailIsRejected() {
        userRepository.save(User.builder()
                .name("First").email("dup@example.com")
                .password("placeholder-hash").role(Role.DONOR).build());

        User duplicate = User.builder()
                .name("Second").email("dup@example.com")
                .password("placeholder-hash").role(Role.RECEIVER).build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateMatchPairIsRejected() {
        User donor = userRepository.save(User.builder()
                .name("Fred Donor").email("fred@example.com")
                .password("placeholder-hash").role(Role.DONOR).build());
        User receiver = userRepository.save(User.builder()
                .name("NGO2").email("ngo2@example.com")
                .password("placeholder-hash").role(Role.RECEIVER).build());
        Donation donation = donationRepository.save(Donation.builder()
                .donor(donor).title("Chair").description("Wooden chair.")
                .category(Category.FURNITURE).quantity(new BigDecimal("1"))
                .quantityUnit(QuantityUnit.PIECES).condition(Condition.GOOD).city("Pune")
                .build());
        Requirement requirement = requirementRepository.save(Requirement.builder()
                .receiver(receiver).title("Chair needed").description("Need a chair.")
                .category(Category.FURNITURE).quantityRequired(new BigDecimal("1"))
                .quantityUnit(QuantityUnit.PIECES).city("Pune").urgency(Urgency.LOW)
                .build());

        matchRepository.saveAndFlush(Match.builder()
                .donation(donation).requirement(requirement).score(new BigDecimal("75.00")).build());

        Match duplicate = Match.builder()
                .donation(donation).requirement(requirement).score(new BigDecimal("75.00")).build();

        assertThatThrownBy(() -> matchRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nonPositiveQuantityIsRejected() {
        User donor = userRepository.save(User.builder()
                .name("Grace Donor").email("grace@example.com")
                .password("placeholder-hash").role(Role.DONOR).build());

        Donation invalid = Donation.builder()
                .donor(donor).title("Empty box").description("Box with nothing in it.")
                .category(Category.OTHER).quantity(BigDecimal.ZERO)
                .quantityUnit(QuantityUnit.BOXES).condition(Condition.USED).city("Pune")
                .build();

        assertThatThrownBy(() -> donationRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void flushesAreConsistentWithPersistedGraph() {
        User donor = userRepository.save(User.builder()
                .name("Hank Donor").email("hank@example.com")
                .password("placeholder-hash").role(Role.DONOR).build());
        Donation donation = donationRepository.save(Donation.builder()
                .donor(donor).title("Rice").description("Five bags of rice.")
                .category(Category.FOOD).quantity(new BigDecimal("5"))
                .quantityUnit(QuantityUnit.BAGS).condition(Condition.NEW).city("Nagpur")
                .build());
        entityManager.flush();
        entityManager.clear();

        Donation reloaded = donationRepository.findById(donation.getId()).orElseThrow();
        assertThat(reloaded.getDonor().getId()).isEqualTo(donor.getId());
        assertThat(reloaded.getQuantity()).isEqualByComparingTo(new BigDecimal("5"));
    }
}
