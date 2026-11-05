package com.awscalculator.backend.optimization;

import static org.assertj.core.api.Assertions.assertThat;

import com.awscalculator.backend.auth.User;
import com.awscalculator.backend.auth.UserRepository;
import com.awscalculator.backend.optimization.dto.OptimizationResponse;
import com.awscalculator.backend.optimization.dto.Suggestion;
import com.awscalculator.backend.project.Project;
import com.awscalculator.backend.project.ProjectRepository;
import com.awscalculator.backend.resource.Resource;
import com.awscalculator.backend.resource.ResourceRepository;
import com.awscalculator.backend.resource.ResourceType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Goes through the real Spring proxy and real repositories (not a hand-built instance or mocks)
 * so that:
 * <ul>
 *   <li>re-adding {@code @Transactional} to {@link OptimizationService#optimize} would open a
 *   transaction around the advisor (LLM) call and fail this test, and</li>
 *   <li>the ownership check and prompt-building still work on the detached entities the
 *   repositories hand back once no outer transaction is holding a persistence context open
 *   (Project.user is LAZY and only touched via getId()).</li>
 * </ul>
 */
@SpringBootTest(properties = "ai.optimization.provider=" + OptimizationServiceTest.PROBE_PROVIDER)
class OptimizationServiceTest {

    static final String PROBE_PROVIDER = "transaction-probe";

    @Autowired
    private OptimizationService optimizationService;

    @Autowired
    private TransactionProbeAdvisor probeAdvisor;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    private User user;
    private Project project;
    private Resource resource;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User(
                "optimization-test-" + UUID.randomUUID() + "@example.test", "optimization-test", null));
        Project unsaved = new Project("optimization-test", "ap-northeast-2");
        unsaved.setUser(user);
        project = projectRepository.save(unsaved);
        resource = resourceRepository.save(new Resource(project, ResourceType.S3, Map.of("storageGb", 10)));
    }

    @AfterEach
    void tearDown() {
        resourceRepository.delete(resource);
        projectRepository.delete(project);
        userRepository.delete(user);
    }

    @Test
    void doesNotHoldATransactionOpenAcrossTheAdvisorCall() {
        OptimizationResponse response = optimizationService.optimize(project.getId(), user.getId(), "ko");

        assertThat(probeAdvisor.wasCalled()).isTrue();
        assertThat(probeAdvisor.sawActiveTransaction()).isFalse();
        assertThat(probeAdvisor.lastPrompt()).contains(String.valueOf(project.getMonthlyUsers()));
        assertThat(response.aiGenerated()).isTrue();
        assertThat(response.provider()).isEqualTo(PROBE_PROVIDER);
    }

    @TestConfiguration
    static class ProbeConfiguration {

        @Bean
        TransactionProbeAdvisor transactionProbeAdvisor() {
            return new TransactionProbeAdvisor();
        }
    }

    static class TransactionProbeAdvisor implements CostOptimizationAdvisor {

        private final AtomicBoolean called = new AtomicBoolean(false);
        private final AtomicBoolean activeTransaction = new AtomicBoolean(false);
        private String prompt = "";

        @Override
        public String name() {
            return PROBE_PROVIDER;
        }

        @Override
        public CostOptimizationAdvice advise(CostOptimizationContext context) {
            called.set(true);
            activeTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            // The real prompt the API advisors send — must build fine from detached entities.
            prompt = CostOptimizationPromptBuilder.build(context);
            return new CostOptimizationAdvice(
                    List.of(new Suggestion("probe", BigDecimal.ONE)), PROBE_PROVIDER, true);
        }

        boolean wasCalled() {
            return called.get();
        }

        boolean sawActiveTransaction() {
            return activeTransaction.get();
        }

        String lastPrompt() {
            return prompt;
        }
    }
}
