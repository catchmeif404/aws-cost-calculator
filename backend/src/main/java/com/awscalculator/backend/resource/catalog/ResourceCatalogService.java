package com.awscalculator.backend.resource.catalog;

import com.awscalculator.backend.resource.ResourceType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceCatalogService {

    private final ResourceCatalogEntryRepository resourceCatalogEntryRepository;
    private final ResourceCatalogFieldOptionRepository fieldOptionRepository;

    @Transactional(readOnly = true)
    public List<ResourceCatalogItem> items() {
        List<ResourceCatalogEntry> entries = resourceCatalogEntryRepository.findAllByOrderByIdAsc();
        Map<Long, Map<String, List<ResourceCatalogOption>>> optionsByEntryAndField = optionsByEntryAndField(entries);

        return entries.stream()
                .map(e -> new ResourceCatalogItem(
                        e.getType(), e.getKind(), e.getTitle(), e.getShortName(), e.getCategory(),
                        e.getDescription(), e.getTone(), e.getDefaults(),
                        withNormalizedOptions(e.getFields(), optionsByEntryAndField.getOrDefault(e.getId(), Map.of()))
                ))
                .toList();
    }

    @Transactional
    public void seedIfMissing() {
        for (ResourceCatalogItem seedItem : SEED_ITEMS) {
            ResourceCatalogEntry entry = resourceCatalogEntryRepository.findByType(seedItem.type())
                    .orElseGet(() -> resourceCatalogEntryRepository.save(new ResourceCatalogEntry(
                            seedItem.type(), seedItem.kind(), seedItem.title(), seedItem.shortName(),
                            seedItem.category(), seedItem.description(), seedItem.tone(),
                            seedItem.defaults(), withoutOptions(seedItem.fields())
                    )));

            updateEntry(entry, seedItem);
            replaceOptions(entry, seedItem.fields());
        }
    }

    private void updateEntry(ResourceCatalogEntry entry, ResourceCatalogItem seedItem) {
        entry.setKind(seedItem.kind());
        entry.setTitle(seedItem.title());
        entry.setShortName(seedItem.shortName());
        entry.setCategory(seedItem.category());
        entry.setDescription(seedItem.description());
        entry.setTone(seedItem.tone());
        entry.setDefaults(seedItem.defaults());
        entry.setFields(withoutOptions(seedItem.fields()));
    }

    private Map<Long, Map<String, List<ResourceCatalogOption>>> optionsByEntryAndField(List<ResourceCatalogEntry> entries) {
        List<Long> entryIds = entries.stream()
                .map(ResourceCatalogEntry::getId)
                .toList();
        if (entryIds.isEmpty()) {
            return Map.of();
        }

        return fieldOptionRepository.findAllByCatalogEntryIdInOrderByCatalogEntryIdAscFieldKeyAscOptionOrderAsc(entryIds)
                .stream()
                .collect(Collectors.groupingBy(
                        option -> option.getCatalogEntry().getId(),
                        Collectors.groupingBy(
                                ResourceCatalogFieldOption::getFieldKey,
                                Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new), options -> options.stream()
                                        .sorted(Comparator.comparingInt(ResourceCatalogFieldOption::getOptionOrder))
                                        .map(option -> new ResourceCatalogOption(option.getLabel(), option.getValue()))
                                        .toList())
                        )
                ));
    }

    private List<ResourceCatalogField> withNormalizedOptions(
            List<ResourceCatalogField> fields,
            Map<String, List<ResourceCatalogOption>> optionsByField
    ) {
        return fields.stream()
                .map(field -> new ResourceCatalogField(
                        field.key(), field.label(), field.type(), field.defaultValue(),
                        optionsByField.getOrDefault(field.key(), List.of()),
                        field.min(), field.max(), field.step(), field.costRelevant()
                ))
                .toList();
    }

    private void replaceOptions(ResourceCatalogEntry entry, List<ResourceCatalogField> seedFields) {
        fieldOptionRepository.deleteByCatalogEntry(entry);
        fieldOptionRepository.flush();
        for (ResourceCatalogField field : seedFields) {
            List<ResourceCatalogOption> options = field.options();
            for (int i = 0; i < options.size(); i++) {
                ResourceCatalogOption option = options.get(i);
                fieldOptionRepository.save(new ResourceCatalogFieldOption(
                        entry, field.key(), option.label(), option.value(), i
                ));
            }
        }
    }

    private List<ResourceCatalogField> withoutOptions(List<ResourceCatalogField> fields) {
        return fields.stream()
                .map(field -> new ResourceCatalogField(
                        field.key(), field.label(), field.type(), field.defaultValue(), List.of(),
                        field.min(), field.max(), field.step(), field.costRelevant()
                ))
                .toList();
    }

    // Seed data — moved here from the old hardcoded ResourceCatalog.java, which the frontend used
    // to read directly on every request. Now this is only read once per newly-added type, at
    // boot, to populate resource_catalog_entries; ResourceCatalogController reads from the DB.
    private static final List<ResourceCatalogItem> SEED_ITEMS = List.of(
            item(ResourceType.CLOUDFRONT, "cloudFront", "CloudFront", "CDN", "Networking & Content Delivery", "CDN 캐싱",
                    "bg-violet-100 text-violet-800 border-violet-200",
                    Map.of("dataTransferGb", 50, "originType", "alb"),
                    List.of(
                            select("originType", "Origin", "alb", List.of(opt("ALB", "alb"), opt("S3", "s3"), opt("API Gateway", "api_gateway")), false),
                            number("dataTransferGb", "월 데이터 전송 (GB)", 50, 0, null, 1, true)
                    )),
            item(ResourceType.ALB, "alb", "ALB", "ALB", "Networking & Content Delivery", "로드밸런서",
                    "bg-violet-100 text-violet-800 border-violet-200",
                    Map.of("scheme", "internet_facing", "estimatedLcu", 1),
                    List.of(
                            select("scheme", "Scheme", "internet_facing", List.of(opt("Internet-facing", "internet_facing"), opt("Internal", "internal")), false),
                            number("estimatedLcu", "예상 LCU", 1, 1, null, 1, true)
                    )),
            item(ResourceType.ECS, "ecs", "ECS Fargate", "TASK", "Compute", "API/웹 서버",
                    "bg-orange-100 text-orange-800 border-orange-200",
                    Map.of("cpu", 0.5, "memory", 1, "tasks", 2, "architecture", "x86_64", "platform", "linux", "deployment", "single_az"),
                    List.of(
                            select("computePreset", "CPU / Memory", "0.5:1", List.of(
                                    opt("0.25 vCPU / 0.5 GB", "0.25:0.5"),
                                    opt("0.5 vCPU / 1 GB", "0.5:1"),
                                    opt("1 vCPU / 2 GB", "1:2"),
                                    opt("2 vCPU / 4 GB", "2:4"),
                                    opt("4 vCPU / 8 GB", "4:8"),
                                    opt("8 vCPU / 16 GB", "8:16")
                            ), true),
                            number("tasks", "Task 수", 2, 1, 100, 1, true),
                            select("architecture", "아키텍처", "x86_64", List.of(opt("x86_64", "x86_64"), opt("ARM64 (Graviton)", "arm64")), true),
                            select("platform", "플랫폼", "linux", List.of(opt("Linux", "linux"), opt("Windows", "windows")), false),
                            select("deployment", "배치", "single_az", List.of(opt("Single-AZ", "single_az"), opt("Multi-AZ", "multi_az")), false)
                    )),
            item(ResourceType.RDS, "rds", "RDS", "RDS", "Database", "관계형 DB",
                    "bg-blue-100 text-blue-800 border-blue-200",
                    Map.of("engine", "postgres", "instanceType", "db.t4g.micro", "storageGb", 20, "deployment", "single_az", "backupRetentionDays", 7),
                    List.of(
                            select("engine", "Engine", "postgres", List.of(opt("PostgreSQL", "postgres"), opt("MySQL", "mysql"), opt("MariaDB", "mariadb")), false),
                            select("instanceType", "Instance Type", "db.t4g.micro", List.of(opt("db.t4g.micro", "db.t4g.micro"), opt("db.t4g.small", "db.t4g.small"), opt("db.t4g.medium", "db.t4g.medium"), opt("db.r6g.large", "db.r6g.large")), true),
                            number("storageGb", "Storage (GB)", 20, 20, null, 1, true),
                            select("deployment", "배치", "single_az", List.of(opt("Single-AZ", "single_az"), opt("Multi-AZ", "multi_az")), false),
                            number("backupRetentionDays", "백업 보관일", 7, 0, 35, 1, false)
                    )),
            item(ResourceType.REDIS, "redis", "ElastiCache", "REDIS", "Database", "캐시/세션",
                    "bg-blue-100 text-blue-800 border-blue-200",
                    Map.of("nodeType", "cache.t4g.micro", "nodes", 1),
                    List.of(
                            select("nodeType", "Node Type", "cache.t4g.micro", List.of(opt("cache.t4g.micro", "cache.t4g.micro"), opt("cache.t4g.small", "cache.t4g.small"), opt("cache.t4g.medium", "cache.t4g.medium"), opt("cache.r6g.large", "cache.r6g.large")), true),
                            number("nodes", "Node 수", 1, 1, 20, 1, true)
                    )),
            item(ResourceType.S3, "s3", "S3", "S3", "Storage", "파일 저장소",
                    "bg-green-100 text-green-800 border-green-200",
                    Map.of("storageGb", 10, "storageClass", "standard"),
                    List.of(
                            select("storageClass", "Storage Class", "standard", List.of(opt("Standard", "standard"), opt("Intelligent-Tiering", "intelligent_tiering"), opt("Standard-IA", "standard_ia")), false),
                            number("storageGb", "Storage (GB)", 10, 1, null, 1, true)
                    )),
            item(ResourceType.DYNAMODB, "dynamoDb", "DynamoDB", "DDB", "Database", "NoSQL DB",
                    "bg-blue-100 text-blue-800 border-blue-200",
                    Map.of("capacityMode", "on_demand", "monthlyReadRequests", 1_000_000, "monthlyWriteRequests", 200_000, "storageGb", 5),
                    List.of(
                            select("capacityMode", "Capacity Mode", "on_demand", List.of(opt("On-demand", "on_demand"), opt("Provisioned", "provisioned")), false),
                            number("monthlyReadRequests", "월 읽기 요청", 1_000_000, 0, null, 1, true),
                            number("monthlyWriteRequests", "월 쓰기 요청", 200_000, 0, null, 1, true),
                            number("storageGb", "Storage (GB)", 5, 0, null, 1, true)
                    )),
            item(ResourceType.LAMBDA, "lambda", "Lambda", "LMB", "Compute", "서버리스",
                    "bg-orange-100 text-orange-800 border-orange-200",
                    Map.of("requestsPerMonth", 1_000_000, "avgDurationMs", 200, "memoryMb", 512, "runtime", "nodejs", "architecture", "x86_64"),
                    List.of(
                            select("runtime", "Runtime", "nodejs", List.of(opt("Node.js", "nodejs"), opt("Java", "java"), opt("Python", "python"), opt("Go", "go")), false),
                            select("architecture", "아키텍처", "x86_64", List.of(opt("x86_64", "x86_64"), opt("ARM64", "arm64")), false),
                            number("requestsPerMonth", "월 요청 수", 1_000_000, 0, null, 1, true),
                            number("avgDurationMs", "실행 시간 (ms)", 200, 0, null, 1, true),
                            number("memoryMb", "메모리 (MB)", 512, 128, 10240, 64, true)
                    )),
            item(ResourceType.EC2, "ec2", "EC2", "EC2", "Compute", "가상 서버",
                    "bg-orange-100 text-orange-800 border-orange-200",
                    Map.of("instanceType", "t3.micro", "count", 1),
                    List.of(
                            select("instanceType", "Instance Type", "t3.micro", List.of(opt("t3.micro", "t3.micro"), opt("t3.small", "t3.small"), opt("t3.medium", "t3.medium"), opt("m5.large", "m5.large")), true),
                            number("count", "인스턴스 수", 1, 1, 100, 1, true)
                    )),
            item(ResourceType.EBS, "ebs", "EBS", "EBS", "Storage", "블록 스토리지",
                    "bg-green-100 text-green-800 border-green-200",
                    Map.of("volumeType", "gp3", "volumeGb", 20, "volumeCount", 1),
                    List.of(
                            select("volumeType", "Volume Type", "gp3", List.of(opt("gp3", "gp3"), opt("gp2", "gp2"), opt("io2", "io2")), false),
                            number("volumeGb", "볼륨 크기 (GB)", 20, 1, null, 1, true),
                            number("volumeCount", "볼륨 수", 1, 1, 100, 1, true)
                    )),
            item(ResourceType.NAT_GATEWAY, "natGateway", "NAT Gateway", "NAT", "Networking & Content Delivery", "외부 통신",
                    "bg-violet-100 text-violet-800 border-violet-200",
                    Map.of("gatewayCount", 1, "processedGb", 50),
                    List.of(number("gatewayCount", "게이트웨이 수", 1, 1, 20, 1, true), number("processedGb", "월 처리량 (GB)", 50, 0, null, 1, true))),
            item(ResourceType.DATA_TRANSFER, "dataTransfer", "Data Transfer", "OUT", "Networking & Content Delivery", "아웃바운드",
                    "bg-violet-100 text-violet-800 border-violet-200",
                    Map.of("outboundGb", 10, "destination", "internet"),
                    List.of(select("destination", "대상", "internet", List.of(opt("Internet", "internet"), opt("Cross-AZ", "cross_az"), opt("Cross-Region", "cross_region")), false), number("outboundGb", "아웃바운드 (GB)", 10, 0, null, 1, true))),
            item(ResourceType.VPC_ENDPOINT, "vpcEndpoint", "VPC Endpoint", "VPCE", "Networking & Content Delivery", "프라이빗 연결",
                    "bg-violet-100 text-violet-800 border-violet-200",
                    Map.of("endpointType", "interface", "count", 1, "processedGb", 50),
                    List.of(select("endpointType", "Endpoint Type", "interface", List.of(opt("Interface", "interface"), opt("Gateway", "gateway")), false), number("count", "Endpoint 수", 1, 1, 50, 1, true), number("processedGb", "처리량 (GB)", 50, 0, null, 1, true))),
            item(ResourceType.TRANSIT_GATEWAY, "transitGateway", "Transit Gateway", "TGW", "Networking & Content Delivery", "VPC 허브",
                    "bg-violet-100 text-violet-800 border-violet-200",
                    Map.of("attachmentCount", 1, "processedGb", 100),
                    List.of(number("attachmentCount", "Attachment 수", 1, 1, 50, 1, true), number("processedGb", "처리량 (GB)", 100, 0, null, 1, true))),
            item(ResourceType.VPC_PEERING, "vpcPeering", "VPC Peering", "PEER", "Networking & Content Delivery", "VPC 연결",
                    "bg-violet-100 text-violet-800 border-violet-200",
                    Map.of("processedGb", 50),
                    List.of(number("processedGb", "처리량 (GB)", 50, 0, null, 1, true))),

            // --- Added along with the RDB catalog migration ---
            item(ResourceType.API_GATEWAY, "apiGateway", "API Gateway", "APIGW", "Compute", "서버리스 API",
                    "bg-orange-100 text-orange-800 border-orange-200",
                    Map.of("requestsPerMonth", 1_000_000),
                    List.of(number("requestsPerMonth", "월 요청 수", 1_000_000, 0, null, 1, true))),
            item(ResourceType.SQS, "sqs", "SQS", "SQS", "Compute", "메시지 큐",
                    "bg-orange-100 text-orange-800 border-orange-200",
                    Map.of("requestsPerMonth", 1_000_000),
                    List.of(number("requestsPerMonth", "월 요청 수", 1_000_000, 0, null, 1, true))),
            item(ResourceType.SNS, "sns", "SNS", "SNS", "Compute", "알림 발행",
                    "bg-orange-100 text-orange-800 border-orange-200",
                    Map.of("requestsPerMonth", 100_000),
                    List.of(number("requestsPerMonth", "월 발행 수", 100_000, 0, null, 1, true))),
            item(ResourceType.EFS, "efs", "EFS", "EFS", "Storage", "공유 파일 스토리지",
                    "bg-green-100 text-green-800 border-green-200",
                    Map.of("storageGb", 100),
                    List.of(number("storageGb", "Storage (GB)", 100, 1, null, 1, true))),
            item(ResourceType.AURORA, "aurora", "Aurora Serverless", "AURORA", "Database", "서버리스 관계형 DB",
                    "bg-blue-100 text-blue-800 border-blue-200",
                    Map.of("acu", 2, "storageGb", 20),
                    List.of(
                            number("acu", "ACU (Capacity Unit)", 2, 1, 128, 1, true),
                            number("storageGb", "Storage (GB)", 20, 1, null, 1, true)
                    )),
            item(ResourceType.OPENSEARCH, "openSearch", "OpenSearch", "OS", "Database", "로그/검색",
                    "bg-blue-100 text-blue-800 border-blue-200",
                    Map.of("instanceType", "t3.small.search", "nodeCount", 1, "storageGb", 20),
                    List.of(
                            select("instanceType", "Instance Type", "t3.small.search", List.of(opt("t3.small.search", "t3.small.search"), opt("m6g.large.search", "m6g.large.search")), true),
                            number("nodeCount", "Node 수", 1, 1, 20, 1, true),
                            number("storageGb", "Storage (GB)", 20, 10, null, 1, true)
                    )),
            item(ResourceType.SECRETS_MANAGER, "secretsManager", "Secrets Manager", "SECRET", "Security", "비밀 관리",
                    "bg-red-100 text-red-800 border-red-200",
                    Map.of("secretCount", 1, "apiRequestsPerMonth", 10_000),
                    List.of(
                            number("secretCount", "Secret 수", 1, 1, null, 1, true),
                            number("apiRequestsPerMonth", "월 API 호출 수", 10_000, 0, null, 1, true)
                    )),
            item(ResourceType.ROUTE53, "route53", "Route 53", "R53", "Networking & Content Delivery", "DNS",
                    "bg-violet-100 text-violet-800 border-violet-200",
                    Map.of("hostedZoneCount", 1, "monthlyQueries", 1_000_000),
                    List.of(
                            number("hostedZoneCount", "Hosted Zone 수", 1, 1, null, 1, true),
                            number("monthlyQueries", "월 쿼리 수", 1_000_000, 0, null, 1, true)
                    )),
            item(ResourceType.EVENTBRIDGE, "eventBridge", "EventBridge", "EVT", "Application Integration", "이벤트 버스",
                    "bg-fuchsia-100 text-fuchsia-800 border-fuchsia-200",
                    Map.of("eventSource", "custom", "monthlyEvents", 1_000_000),
                    List.of(
                            select("eventSource", "Event Source", "custom", List.of(
                                    opt("Custom events", "custom"),
                                    opt("Partner events", "partner"),
                                    opt("AWS management events", "aws_management")
                            ), false),
                            number("monthlyEvents", "월 이벤트 수", 1_000_000, 0, null, 1, true)
                    )),
            item(ResourceType.STEP_FUNCTIONS, "stepFunctions", "Step Functions", "SFN", "Application Integration", "워크플로 오케스트레이션",
                    "bg-fuchsia-100 text-fuchsia-800 border-fuchsia-200",
                    Map.of("workflowType", "standard", "monthlyExecutions", 100_000, "transitionsPerExecution", 5),
                    List.of(
                            select("workflowType", "Workflow Type", "standard", List.of(
                                    opt("Standard", "standard"),
                                    opt("Express (추정 제외)", "express")
                            ), false),
                            number("monthlyExecutions", "월 실행 수", 100_000, 0, null, 1, true),
                            number("transitionsPerExecution", "실행당 상태 전이", 5, 1, null, 1, true)
                    )),
            item(ResourceType.CLOUDWATCH, "cloudWatch", "CloudWatch", "CW", "Management & Governance", "로그/알람 모니터링",
                    "bg-cyan-100 text-cyan-800 border-cyan-200",
                    Map.of("monthlyIngestGb", 30, "storedGb", 6, "alarmCount", 3),
                    List.of(
                            number("monthlyIngestGb", "월 로그 수집량 (GB)", 30, 0, null, 1, true),
                            number("storedGb", "보관 로그 (GB-월)", 6, 0, null, 1, true),
                            number("alarmCount", "표준 알람 수", 3, 0, null, 1, true)
                    )),
            item(ResourceType.KINESIS, "kinesis", "Kinesis Data Streams", "KDS", "Analytics", "실시간 스트리밍",
                    "bg-pink-100 text-pink-800 border-pink-200",
                    Map.of("streamMode", "provisioned", "shardCount", 1, "monthlyPutPayloadUnits", 1_000_000),
                    List.of(
                            select("streamMode", "Stream Mode", "provisioned", List.of(
                                    opt("Provisioned", "provisioned"),
                                    opt("On-demand (추정 제외)", "on_demand")
                            ), false),
                            number("shardCount", "Shard 수", 1, 1, null, 1, true),
                            number("monthlyPutPayloadUnits", "월 PUT Payload Unit", 1_000_000, 0, null, 1, true)
                    )),
            item(ResourceType.WAF, "waf", "AWS WAF", "WAF", "Security", "웹 요청 보호",
                    "bg-red-100 text-red-800 border-red-200",
                    Map.of("webAclCount", 1, "ruleCount", 5, "monthlyRequests", 10_000_000),
                    List.of(
                            number("webAclCount", "Web ACL 수", 1, 1, null, 1, true),
                            number("ruleCount", "Rule 수", 5, 0, null, 1, true),
                            number("monthlyRequests", "월 요청 수", 10_000_000, 0, null, 1, true)
                    )),
            item(ResourceType.SES, "ses", "SES", "SES", "Application Integration", "SMTP/이메일 발송",
                    "bg-fuchsia-100 text-fuchsia-800 border-fuchsia-200",
                    Map.of("recipientsPerMonth", 100_000, "attachmentGb", 0),
                    List.of(
                            number("recipientsPerMonth", "월 수신자 수", 100_000, 0, null, 1, true),
                            number("attachmentGb", "월 첨부/페이로드 (GB)", 0, 0, null, 1, true)
                    )),
            item(ResourceType.EKS, "eks", "EKS", "EKS", "Compute", "Kubernetes 제어 플레인",
                    "bg-orange-100 text-orange-800 border-orange-200",
                    Map.of("controlPlaneTier", "standard", "clusterCount", 1),
                    List.of(
                            select("controlPlaneTier", "Control Plane Tier", "standard", List.of(
                                    opt("Standard", "standard"),
                                    opt("Extended Support", "extended"),
                                    opt("Provisioned XL", "provisioned_xl"),
                                    opt("Provisioned 2XL", "provisioned_2xl"),
                                    opt("Provisioned 4XL", "provisioned_4xl"),
                                    opt("Provisioned 8XL", "provisioned_8xl")
                            ), true),
                            number("clusterCount", "클러스터 수", 1, 1, null, 1, true)
                    )),
            item(ResourceType.ECR, "ecr", "ECR", "ECR", "Storage", "컨테이너 이미지 저장소",
                    "bg-green-100 text-green-800 border-green-200",
                    Map.of("storageClass", "standard", "storageGb", 20),
                    List.of(
                            select("storageClass", "Storage Class", "standard", List.of(
                                    opt("Standard", "standard"),
                                    opt("Archive", "archive")
                            ), true),
                            number("storageGb", "이미지 저장량 (GB-월)", 20, 0, null, 1, true)
                    )),
            item(ResourceType.CLOUDTRAIL, "cloudTrail", "CloudTrail", "CT", "Management & Governance", "감사 이벤트 추적",
                    "bg-cyan-100 text-cyan-800 border-cyan-200",
                    Map.of("managementEvents", 100_000, "dataEvents", 100_000, "queryGb", 0),
                    List.of(
                            number("managementEvents", "월 관리 이벤트", 100_000, 0, null, 1, true),
                            number("dataEvents", "월 데이터 이벤트", 100_000, 0, null, 1, true),
                            number("queryGb", "Lake 쿼리 스캔 (GB)", 0, 0, null, 1, true)
                    )),
            item(ResourceType.GLUE, "glue", "Glue", "GLUE", "Analytics", "ETL/크롤러",
                    "bg-pink-100 text-pink-800 border-pink-200",
                    Map.of("jobType", "standard", "etlDpuHours", 10, "crawlerDpuHours", 2),
                    List.of(
                            select("jobType", "Job Type", "standard", List.of(
                                    opt("Standard", "standard"),
                                    opt("Glue 6.0+", "glue_6"),
                                    opt("Flex", "flex"),
                                    opt("Flex Glue 6.0+", "flex_glue_6")
                            ), true),
                            number("etlDpuHours", "ETL DPU-Hour", 10, 0, null, 1, true),
                            number("crawlerDpuHours", "Crawler DPU-Hour", 2, 0, null, 1, true)
                    )),
            item(ResourceType.ATHENA, "athena", "Athena", "ATH", "Analytics", "쿼리 스캔",
                    "bg-pink-100 text-pink-800 border-pink-200",
                    Map.of("scannedTb", 1),
                    List.of(number("scannedTb", "월 스캔량 (TB)", 1, 0, null, 1, true))),
            item(ResourceType.REDSHIFT, "redshift", "Redshift", "RS", "Database", "데이터 웨어하우스",
                    "bg-blue-100 text-blue-800 border-blue-200",
                    Map.of("nodeType", "ra3.large", "nodeCount", 1, "storageGb", 100),
                    List.of(
                            select("nodeType", "Node Type", "ra3.large", List.of(
                                    opt("ra3.large", "ra3.large"),
                                    opt("ra3.xlplus", "ra3.xlplus"),
                                    opt("dc2.large", "dc2.large")
                            ), true),
                            number("nodeCount", "노드 수", 1, 1, null, 1, true),
                            number("storageGb", "Managed Storage (GB-월)", 100, 0, null, 1, true)
                    )),
            item(ResourceType.MSK, "msk", "MSK", "MSK", "Analytics", "관리형 Kafka",
                    "bg-pink-100 text-pink-800 border-pink-200",
                    Map.of("brokerType", "m5.large", "brokerCount", 3, "storageGb", 100),
                    List.of(
                            select("brokerType", "Broker Type", "m5.large", List.of(
                                    opt("kafka.t3.small", "t3.small"),
                                    opt("kafka.m5.large", "m5.large"),
                                    opt("kafka.m7g.large", "m7g.large")
                            ), true),
                            number("brokerCount", "브로커 수", 3, 1, null, 1, true),
                            number("storageGb", "스토리지 (GB-월)", 100, 0, null, 1, true)
                    )),
            item(ResourceType.APPSYNC, "appSync", "AppSync", "GQL", "Application Integration", "GraphQL API",
                    "bg-fuchsia-100 text-fuchsia-800 border-fuchsia-200",
                    Map.of("graphqlRequests", 1_000_000, "realtimeMinutes", 0),
                    List.of(
                            number("graphqlRequests", "월 GraphQL 요청", 1_000_000, 0, null, 1, true),
                            number("realtimeMinutes", "실시간 연결 분", 0, 0, null, 1, true)
                    )),
            item(ResourceType.COGNITO, "cognito", "Cognito", "COG", "Security", "사용자 인증",
                    "bg-red-100 text-red-800 border-red-200",
                    Map.of("tier", "lite", "monthlyActiveUsers", 10_000),
                    List.of(
                            select("tier", "User Pool Tier", "lite", List.of(
                                    opt("Lite", "lite"),
                                    opt("Essentials", "essentials"),
                                    opt("Plus", "plus")
                            ), true),
                            number("monthlyActiveUsers", "월 활성 사용자 (MAU)", 10_000, 0, null, 1, true)
                    )),
            item(ResourceType.BEDROCK, "bedrock", "Bedrock", "BR", "AI/ML", "Nova Lite 토큰",
                    "bg-teal-100 text-teal-800 border-teal-200",
                    Map.of("model", "nova_lite", "input1kTokens", 1000, "output1kTokens", 200),
                    List.of(
                            select("model", "Model", "nova_lite", List.of(
                                    opt("Nova Micro", "nova_micro"),
                                    opt("Nova Lite", "nova_lite"),
                                    opt("Nova Pro", "nova_pro")
                            ), true),
                            number("input1kTokens", "입력 토큰 (1K 단위)", 1000, 0, null, 1, true),
                            number("output1kTokens", "출력 토큰 (1K 단위)", 200, 0, null, 1, true)
                    )),
            item(ResourceType.SAGEMAKER, "sageMaker", "SageMaker", "SM", "AI/ML", "모델 호스팅",
                    "bg-teal-100 text-teal-800 border-teal-200",
                    Map.of("instanceType", "ml.m5.large", "instanceCount", 1),
                    List.of(
                            select("instanceType", "Hosting Instance", "ml.m5.large", List.of(
                                    opt("ml.m5.large", "ml.m5.large"),
                                    opt("ml.c6g.large", "ml.c6g.large"),
                                    opt("ml.g4dn.xlarge", "ml.g4dn.xlarge")
                            ), true),
                            number("instanceCount", "호스팅 인스턴스 수", 1, 1, null, 1, true)
                    )),
            item(ResourceType.SSM, "ssm", "Systems Manager", "SSM", "Management & Governance", "Parameter Store",
                    "bg-cyan-100 text-cyan-800 border-cyan-200",
                    Map.of("parameterTier", "advanced", "advancedParameters", 10, "apiRequests", 100_000),
                    List.of(
                            select("parameterTier", "Parameter Tier", "advanced", List.of(
                                    opt("Standard", "standard"),
                                    opt("Advanced", "advanced")
                            ), true),
                            number("advancedParameters", "Advanced Parameter 수", 10, 0, null, 1, true),
                            number("apiRequests", "Parameter API 요청", 100_000, 0, null, 1, true)
                    )),
            item(ResourceType.KMS, "kms", "KMS", "KMS", "Security", "키 관리",
                    "bg-red-100 text-red-800 border-red-200",
                    Map.of("requestType", "symmetric", "keyCount", 1, "requests", 100_000),
                    List.of(
                            select("requestType", "Request Type", "symmetric", List.of(
                                    opt("Symmetric", "symmetric"),
                                    opt("Asymmetric", "asymmetric"),
                                    opt("RSA 2048", "rsa_2048")
                            ), true),
                            number("keyCount", "Customer managed key 수", 1, 0, null, 1, true),
                            number("requests", "월 KMS 요청", 100_000, 0, null, 1, true)
                    )),
            item(ResourceType.CLOUDHSM, "cloudHsm", "CloudHSM", "HSM", "Security", "전용 HSM",
                    "bg-red-100 text-red-800 border-red-200",
                    Map.of("hsmType", "hsm1", "hsmCount", 1),
                    List.of(
                            select("hsmType", "HSM Type", "hsm1", List.of(
                                    opt("CloudHSM v2", "hsm1"),
                                    opt("CloudHSM hsm2m", "hsm2m")
                            ), true),
                            number("hsmCount", "HSM 수", 1, 1, null, 1, true)
                    )),
            item(ResourceType.BACKUP, "backup", "AWS Backup", "BKP", "Storage", "백업 저장소",
                    "bg-green-100 text-green-800 border-green-200",
                    Map.of("backupResource", "ebs_lagv", "warmStorageGb", 100),
                    List.of(
                            select("backupResource", "Backup Resource", "ebs_lagv", List.of(
                                    opt("EBS LAGV", "ebs_lagv"),
                                    opt("EFS LAGV", "efs_lagv"),
                                    opt("DynamoDB LAGV", "dynamodb_lagv")
                            ), true),
                            number("warmStorageGb", "Warm backup (GB-월)", 100, 0, null, 1, true)
                    )),
            item(ResourceType.NLB, "nlb", "Network Load Balancer", "NLB", "Networking & Content Delivery", "L4 로드 밸런서",
                    "bg-violet-100 text-violet-800 border-violet-200",
                    Map.of("balancerCount", 1, "lcu", 1),
                    List.of(
                            number("balancerCount", "NLB 수", 1, 1, null, 1, true),
                            number("lcu", "월 평균 NLCU", 1, 0, null, 1, true)
                    )),
            item(ResourceType.GWLB, "gwlb", "Gateway Load Balancer", "GWLB", "Networking & Content Delivery", "네트워크 어플라이언스",
                    "bg-violet-100 text-violet-800 border-violet-200",
                    Map.of("balancerCount", 1, "lcu", 1),
                    List.of(
                            number("balancerCount", "GWLB 수", 1, 1, null, 1, true),
                            number("lcu", "월 평균 GLCU", 1, 0, null, 1, true)
                    ))
    );

    private static ResourceCatalogItem item(ResourceType type, String kind, String title, String shortName,
                                            String category, String description, String tone,
                                            Map<String, Object> defaults, List<ResourceCatalogField> fields) {
        return new ResourceCatalogItem(type, kind, title, shortName, category, description, tone, defaults, fields);
    }

    private static ResourceCatalogField select(String key, String label, Object defaultValue,
                                               List<ResourceCatalogOption> options, boolean costRelevant) {
        return new ResourceCatalogField(key, label, "select", defaultValue, options, null, null, null, costRelevant);
    }

    private static ResourceCatalogField number(String key, String label, Object defaultValue,
                                               Integer min, Integer max, Integer step, boolean costRelevant) {
        return new ResourceCatalogField(key, label, "number", defaultValue, List.of(), min, max, step, costRelevant);
    }

    private static ResourceCatalogOption opt(String label, Object value) {
        return new ResourceCatalogOption(label, value);
    }
}
