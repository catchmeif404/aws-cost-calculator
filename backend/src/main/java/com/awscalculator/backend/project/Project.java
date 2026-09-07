package com.awscalculator.backend.project;

import com.awscalculator.backend.auth.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable: projects created before this column existed have no owner and are simply never
    // returned by the user-scoped history list — no backfill needed (see ProjectService/
    // CalculationService's listForUser).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String region;

    @Column(name = "monthly_users", nullable = false, columnDefinition = "integer default 10000")
    private int monthlyUsers;

    @Column(name = "requests_per_user", nullable = false, columnDefinition = "integer default 100")
    private int requestsPerUser;

    @Column(name = "busy_traffic_level", nullable = false, columnDefinition = "varchar(255) default 'similar'")
    private String busyTrafficLevel;

    @Column(name = "service_stage", nullable = false, columnDefinition = "varchar(255) default 'mvp'")
    private String serviceStage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Project(String name, String region) {
        this.name = name;
        this.region = region;
        this.monthlyUsers = 10_000;
        this.requestsPerUser = 100;
        this.busyTrafficLevel = "similar";
        this.serviceStage = "mvp";
    }

    public Project(String name, String region, int monthlyUsers, int requestsPerUser, String busyTrafficLevel, String serviceStage) {
        this.name = name;
        this.region = region;
        this.monthlyUsers = monthlyUsers;
        this.requestsPerUser = requestsPerUser;
        this.busyTrafficLevel = busyTrafficLevel;
        this.serviceStage = serviceStage;
    }
}
