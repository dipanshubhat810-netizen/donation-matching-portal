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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EntityPersistenceTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsRepresentativeEntitiesAndRelationships() {
        User donor = User.builder()
                .name("Rohit Donor")
                .email("rohit.donor@example.com")
                .password("placeholder-hash")
                .role(Role.DONOR)
                .build();
        User receiver = User.builder()
                .name("Seva NGO")
                .email("seva.receiver@example.com")
                .password("placeholder-hash")
                .role(Role.RECEIVER)
                .build();
        entityManager.persist(donor);
        entityManager.persist(receiver);

        Donation donation = Donation.builder()
                .donor(donor)
                .title("Winter blankets")
                .description("Ten woollen blankets in good condition.")
                .category(Category.HOUSEHOLD)
                .quantity(new BigDecimal("10"))
                .quantityUnit(QuantityUnit.PIECES)
                .condition(Condition.GOOD)
                .city("Pune")
                .locality("Kothrud")
                .pincode("411038")
                .build();
        entityManager.persist(donation);
        assertThat(donation.getStatus()).isEqualTo(DonationStatus.SUBMITTED);

        DonationPhoto photo = DonationPhoto.builder()
                .donation(donation)
                .storageKey("donations/1/blankets.jpg")
                .originalFilename("blankets.jpg")
                .contentType("image/jpeg")
                .fileSize(2048L)
                .build();
        entityManager.persist(photo);

        Requirement requirement = Requirement.builder()
                .receiver(receiver)
                .title("Blankets for old age home")
                .description("Need warm blankets for residents this winter.")
                .category(Category.HOUSEHOLD)
                .quantityRequired(new BigDecimal("10"))
                .quantityUnit(QuantityUnit.PIECES)
                .city("Pune")
                .urgency(Urgency.HIGH)
                .build();
        entityManager.persist(requirement);
        assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.SUBMITTED);

        Match match = Match.builder()
                .donation(donation)
                .requirement(requirement)
                .score(new BigDecimal("85.50"))
                .build();
        entityManager.persist(match);
        assertThat(match.getStatus()).isEqualTo(MatchStatus.SUGGESTED);

        Transaction transaction = Transaction.builder()
                .match(match)
                .donor(donor)
                .receiver(receiver)
                .build();
        entityManager.persist(transaction);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);

        entityManager.flush();

        AuditRecord audit = AuditRecord.builder()
                .actor(donor)
                .action("CREATE")
                .entityType("Donation")
                .entityId(donation.getId().toString())
                .details("donation created")
                .build();
        entityManager.persist(audit);

        entityManager.flush();

        assertThat(donation.getId()).isNotNull();
        assertThat(photo.getId()).isNotNull();
        assertThat(requirement.getId()).isNotNull();
        assertThat(match.getId()).isNotNull();
        assertThat(transaction.getId()).isNotNull();
        assertThat(audit.getId()).isNotNull();

        Donation reloaded = entityManager.find(Donation.class, donation.getId());
        assertThat(reloaded.getDonor().getId()).isEqualTo(donor.getId());
        assertThat(reloaded.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();

        Transaction reloadedTransaction = entityManager.find(Transaction.class, transaction.getId());
        assertThat(reloadedTransaction.getMatch().getId()).isEqualTo(match.getId());
        assertThat(reloadedTransaction.getDonor().getId()).isEqualTo(donor.getId());
        assertThat(reloadedTransaction.getReceiver().getId()).isEqualTo(receiver.getId());
    }
}
