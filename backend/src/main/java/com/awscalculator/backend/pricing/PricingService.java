package com.awscalculator.backend.pricing;

import static com.awscalculator.backend.pricing.AwsPriceDimension.DEFAULT_OPTION;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PricingService {

    private static final String HOURS_PER_MONTH = "hours_per_month";
    private static final String USD_TO_KRW = "usd_to_krw";
    private static final String EC2_INSTANCE_HOUR = "ec2_instance_hour";
    private static final String EBS_GP3_GB_MONTH = "ebs_gp3_gb_month";
    private static final String NAT_GATEWAY_HOUR = "nat_gateway_hour";
    private static final String NAT_GATEWAY_GB_PROCESSED = "nat_gateway_gb_processed";
    private static final String RDS_INSTANCE_HOUR = "rds_instance_hour";
    private static final String RDS_STORAGE_GB_MONTH = "rds_storage_gb_month";
    private static final String REDIS_NODE_HOUR = "redis_node_hour";
    private static final String FARGATE_VCPU_HOUR_X86 = "fargate_vcpu_hour_x86";
    private static final String FARGATE_GB_HOUR_X86 = "fargate_gb_hour_x86";
    private static final String FARGATE_VCPU_HOUR_ARM = "fargate_vcpu_hour_arm";
    private static final String FARGATE_GB_HOUR_ARM = "fargate_gb_hour_arm";
    private static final String ALB_HOUR = "alb_hour";
    private static final String ALB_LCU_HOUR = "alb_lcu_hour";
    private static final String S3_STANDARD_GB_MONTH = "s3_standard_gb_month";
    private static final String CLOUDFRONT_GB_TRANSFER = "cloudfront_gb_transfer";
    private static final String LAMBDA_PER_REQUEST = "lambda_per_request";
    private static final String LAMBDA_GB_SECOND = "lambda_gb_second";
    private static final String DYNAMODB_PER_READ_UNIT = "dynamodb_per_read_unit";
    private static final String DYNAMODB_PER_WRITE_UNIT = "dynamodb_per_write_unit";
    private static final String DYNAMODB_STORAGE_GB_MONTH = "dynamodb_storage_gb_month";
    private static final String DATA_TRANSFER_OUT_GB = "data_transfer_out_gb";
    private static final String VPC_ENDPOINT_HOUR = "vpc_endpoint_hour";
    private static final String VPC_ENDPOINT_GB_PROCESSED = "vpc_endpoint_gb_processed";
    private static final String TRANSIT_GATEWAY_HOUR = "transit_gateway_hour";
    private static final String TRANSIT_GATEWAY_GB_PROCESSED = "transit_gateway_gb_processed";
    private static final String VPC_PEERING_GB_PROCESSED = "vpc_peering_gb_processed";
    private static final String API_GATEWAY_HTTP_PER_REQUEST = "api_gateway_http_per_request";
    private static final String SQS_PER_REQUEST = "sqs_per_request";
    private static final String SNS_PER_REQUEST = "sns_per_request";
    private static final String EFS_STANDARD_GB_MONTH = "efs_standard_gb_month";
    private static final String S3_STANDARD_IA_GB_MONTH = "s3_standard_ia_gb_month";
    private static final String EBS_GP2_GB_MONTH = "ebs_gp2_gb_month";
    private static final String EBS_IO2_GB_MONTH = "ebs_io2_gb_month";
    private static final String RDS_INSTANCE_HOUR_MULTI_AZ = "rds_instance_hour_multi_az";
    private static final String LAMBDA_PER_REQUEST_ARM = "lambda_per_request_arm";
    private static final String LAMBDA_GB_SECOND_ARM = "lambda_gb_second_arm";
    private static final String DYNAMODB_PROVISIONED_READ_UNIT_HOUR = "dynamodb_provisioned_read_unit_hour";
    private static final String DYNAMODB_PROVISIONED_WRITE_UNIT_HOUR = "dynamodb_provisioned_write_unit_hour";
    private static final String AURORA_SERVERLESS_ACU_HOUR = "aurora_serverless_acu_hour";
    private static final String AURORA_STORAGE_GB_MONTH = "aurora_storage_gb_month";
    private static final String OPENSEARCH_INSTANCE_HOUR = "opensearch_instance_hour";
    private static final String OPENSEARCH_GP3_GB_MONTH = "opensearch_gp3_gb_month";
    private static final String SECRETS_MANAGER_PER_SECRET_MONTH = "secrets_manager_per_secret_month";
    private static final String SECRETS_MANAGER_PER_API_REQUEST = "secrets_manager_per_api_request";
    private static final String ROUTE53_HOSTED_ZONE_MONTH = "route53_hosted_zone_month";
    private static final String ROUTE53_PER_QUERY = "route53_per_query";
    private static final String EVENTBRIDGE_PER_EVENT = "eventbridge_per_event";
    private static final String STEP_FUNCTIONS_PER_STATE_TRANSITION = "step_functions_per_state_transition";
    private static final String CLOUDWATCH_LOGS_INGEST_GB = "cloudwatch_logs_ingest_gb";
    private static final String CLOUDWATCH_LOGS_STORAGE_GB_MONTH = "cloudwatch_logs_storage_gb_month";
    private static final String CLOUDWATCH_ALARM_MONTH = "cloudwatch_alarm_month";
    private static final String KINESIS_SHARD_HOUR = "kinesis_shard_hour";
    private static final String KINESIS_PUT_PAYLOAD_UNIT = "kinesis_put_payload_unit";
    private static final String WAF_WEB_ACL_MONTH = "waf_web_acl_month";
    private static final String WAF_RULE_MONTH = "waf_rule_month";
    private static final String WAF_PER_REQUEST = "waf_per_request";
    private static final String SES_PER_RECIPIENT = "ses_per_recipient";
    private static final String SES_ATTACHMENT_GB = "ses_attachment_gb";
    private static final String EKS_CLUSTER_HOUR = "eks_cluster_hour";
    private static final String ECR_STORAGE_GB_MONTH = "ecr_storage_gb_month";
    private static final String CLOUDTRAIL_PAID_EVENT = "cloudtrail_paid_event";
    private static final String CLOUDTRAIL_DATA_EVENT = "cloudtrail_data_event";
    private static final String CLOUDTRAIL_QUERY_GB = "cloudtrail_query_gb";
    private static final String GLUE_DPU_HOUR = "glue_dpu_hour";
    private static final String GLUE_CRAWLER_DPU_HOUR = "glue_crawler_dpu_hour";
    private static final String ATHENA_SCANNED_TB = "athena_scanned_tb";
    private static final String REDSHIFT_RA3_LARGE_HOUR = "redshift_ra3_large_hour";
    private static final String REDSHIFT_STORAGE_GB_MONTH = "redshift_storage_gb_month";
    private static final String MSK_BROKER_HOUR = "msk_broker_hour";
    private static final String MSK_STORAGE_GB_MONTH = "msk_storage_gb_month";
    private static final String APPSYNC_GRAPHQL_REQUEST = "appsync_graphql_request";
    private static final String APPSYNC_REALTIME_MINUTE = "appsync_realtime_minute";
    private static final String COGNITO_MAU = "cognito_mau";
    private static final String BEDROCK_NOVA_LITE_INPUT_1K_TOKENS = "bedrock_nova_lite_input_1k_tokens";
    private static final String BEDROCK_NOVA_LITE_OUTPUT_1K_TOKENS = "bedrock_nova_lite_output_1k_tokens";
    private static final String SAGEMAKER_ML_M5_LARGE_HOUR = "sagemaker_ml_m5_large_hour";
    private static final String SSM_ADVANCED_PARAMETER_MONTH = "ssm_advanced_parameter_month";
    private static final String SSM_PARAMETER_API_REQUEST = "ssm_parameter_api_request";
    private static final String KMS_KEY_MONTH = "kms_key_month";
    private static final String KMS_REQUEST = "kms_request";
    private static final String CLOUDHSM_HOUR = "cloudhsm_hour";
    private static final String BACKUP_WARM_STORAGE_GB_MONTH = "backup_warm_storage_gb_month";
    private static final String NLB_HOUR = "nlb_hour";
    private static final String NLB_LCU_HOUR = "nlb_lcu_hour";
    private static final String GWLB_HOUR = "gwlb_hour";
    private static final String GWLB_LCU_HOUR = "gwlb_lcu_hour";

    private final AwsPriceDimensionRepository priceDimensionRepository;

    @Transactional(readOnly = true)
    public PricingSnapshot forRegion(String region) {
        if (!PricingCatalog.SUPPORTED_REGIONS.contains(region)) {
            throw new IllegalArgumentException(
                    "Unsupported region: " + region + ". Supported: " + PricingCatalog.SUPPORTED_REGIONS);
        }
        Map<String, Map<String, BigDecimal>> grouped = group(priceDimensionRepository.findByRegion(region));
        if (grouped.isEmpty()) {
            return PricingCatalog.forRegion(region);
        }

        return new PricingSnapshot(
                new PricingSnapshot.Meta(region, "database:aws_price_dimensions", null),
                scalar(grouped, HOURS_PER_MONTH),
                scalar(grouped, USD_TO_KRW),
                options(grouped, EC2_INSTANCE_HOUR),
                scalar(grouped, EBS_GP3_GB_MONTH),
                scalar(grouped, NAT_GATEWAY_HOUR),
                scalar(grouped, NAT_GATEWAY_GB_PROCESSED),
                options(grouped, RDS_INSTANCE_HOUR),
                scalar(grouped, RDS_STORAGE_GB_MONTH),
                options(grouped, REDIS_NODE_HOUR),
                scalar(grouped, FARGATE_VCPU_HOUR_X86),
                scalar(grouped, FARGATE_GB_HOUR_X86),
                scalar(grouped, FARGATE_VCPU_HOUR_ARM),
                scalar(grouped, FARGATE_GB_HOUR_ARM),
                scalar(grouped, ALB_HOUR),
                scalar(grouped, ALB_LCU_HOUR),
                scalar(grouped, S3_STANDARD_GB_MONTH),
                scalar(grouped, CLOUDFRONT_GB_TRANSFER),
                scalar(grouped, LAMBDA_PER_REQUEST),
                scalar(grouped, LAMBDA_GB_SECOND),
                scalar(grouped, DYNAMODB_PER_READ_UNIT),
                scalar(grouped, DYNAMODB_PER_WRITE_UNIT),
                scalar(grouped, DYNAMODB_STORAGE_GB_MONTH),
                scalar(grouped, DATA_TRANSFER_OUT_GB),
                scalar(grouped, VPC_ENDPOINT_HOUR),
                scalar(grouped, VPC_ENDPOINT_GB_PROCESSED),
                scalar(grouped, TRANSIT_GATEWAY_HOUR),
                scalar(grouped, TRANSIT_GATEWAY_GB_PROCESSED),
                scalar(grouped, VPC_PEERING_GB_PROCESSED),
                scalar(grouped, API_GATEWAY_HTTP_PER_REQUEST),
                scalar(grouped, SQS_PER_REQUEST),
                scalar(grouped, SNS_PER_REQUEST),
                scalar(grouped, EFS_STANDARD_GB_MONTH),
                scalar(grouped, S3_STANDARD_IA_GB_MONTH),
                scalar(grouped, EBS_GP2_GB_MONTH),
                scalar(grouped, EBS_IO2_GB_MONTH),
                options(grouped, RDS_INSTANCE_HOUR_MULTI_AZ),
                scalar(grouped, LAMBDA_PER_REQUEST_ARM),
                scalar(grouped, LAMBDA_GB_SECOND_ARM),
                scalar(grouped, DYNAMODB_PROVISIONED_READ_UNIT_HOUR),
                scalar(grouped, DYNAMODB_PROVISIONED_WRITE_UNIT_HOUR),
                scalar(grouped, AURORA_SERVERLESS_ACU_HOUR),
                scalar(grouped, AURORA_STORAGE_GB_MONTH),
                options(grouped, OPENSEARCH_INSTANCE_HOUR),
                scalar(grouped, OPENSEARCH_GP3_GB_MONTH),
                scalar(grouped, SECRETS_MANAGER_PER_SECRET_MONTH),
                scalar(grouped, SECRETS_MANAGER_PER_API_REQUEST),
                scalar(grouped, ROUTE53_HOSTED_ZONE_MONTH),
                scalar(grouped, ROUTE53_PER_QUERY),
                scalar(grouped, EVENTBRIDGE_PER_EVENT),
                scalar(grouped, STEP_FUNCTIONS_PER_STATE_TRANSITION),
                scalar(grouped, CLOUDWATCH_LOGS_INGEST_GB),
                scalar(grouped, CLOUDWATCH_LOGS_STORAGE_GB_MONTH),
                scalar(grouped, CLOUDWATCH_ALARM_MONTH),
                scalar(grouped, KINESIS_SHARD_HOUR),
                scalar(grouped, KINESIS_PUT_PAYLOAD_UNIT),
                scalar(grouped, WAF_WEB_ACL_MONTH),
                scalar(grouped, WAF_RULE_MONTH),
                scalar(grouped, WAF_PER_REQUEST),
                scalar(grouped, SES_PER_RECIPIENT),
                scalar(grouped, SES_ATTACHMENT_GB),
                options(grouped, EKS_CLUSTER_HOUR),
                options(grouped, ECR_STORAGE_GB_MONTH),
                scalar(grouped, CLOUDTRAIL_PAID_EVENT),
                scalar(grouped, CLOUDTRAIL_DATA_EVENT),
                scalar(grouped, CLOUDTRAIL_QUERY_GB),
                options(grouped, GLUE_DPU_HOUR),
                scalar(grouped, GLUE_CRAWLER_DPU_HOUR),
                scalar(grouped, ATHENA_SCANNED_TB),
                options(grouped, REDSHIFT_RA3_LARGE_HOUR),
                scalar(grouped, REDSHIFT_STORAGE_GB_MONTH),
                options(grouped, MSK_BROKER_HOUR),
                scalar(grouped, MSK_STORAGE_GB_MONTH),
                scalar(grouped, APPSYNC_GRAPHQL_REQUEST),
                scalar(grouped, APPSYNC_REALTIME_MINUTE),
                options(grouped, COGNITO_MAU),
                options(grouped, BEDROCK_NOVA_LITE_INPUT_1K_TOKENS),
                options(grouped, BEDROCK_NOVA_LITE_OUTPUT_1K_TOKENS),
                options(grouped, SAGEMAKER_ML_M5_LARGE_HOUR),
                options(grouped, SSM_ADVANCED_PARAMETER_MONTH),
                scalar(grouped, SSM_PARAMETER_API_REQUEST),
                scalar(grouped, KMS_KEY_MONTH),
                options(grouped, KMS_REQUEST),
                options(grouped, CLOUDHSM_HOUR),
                options(grouped, BACKUP_WARM_STORAGE_GB_MONTH),
                scalar(grouped, NLB_HOUR),
                scalar(grouped, NLB_LCU_HOUR),
                scalar(grouped, GWLB_HOUR),
                scalar(grouped, GWLB_LCU_HOUR)
        );
    }

    @Transactional(readOnly = true)
    public PricingDimensionIndex dimensionsForRegion(String region) {
        if (!PricingCatalog.SUPPORTED_REGIONS.contains(region)) {
            throw new IllegalArgumentException(
                    "Unsupported region: " + region + ". Supported: " + PricingCatalog.SUPPORTED_REGIONS);
        }

        Map<String, Map<String, BigDecimal>> grouped = group(priceDimensionRepository.findByRegion(region));
        if (grouped.isEmpty()) {
            PricingSnapshot snapshot = PricingCatalog.forRegion(region);
            return new PricingDimensionIndex(
                    region,
                    snapshot.hoursPerMonth(),
                    snapshot.usdToKrw(),
                    dimensionsFromSnapshot(snapshot)
            );
        }

        return new PricingDimensionIndex(region, scalar(grouped, HOURS_PER_MONTH), scalar(grouped, USD_TO_KRW), grouped);
    }

    @Transactional
    public void seedFromSnapshotsIfMissing() {
        for (String region : PricingCatalog.SUPPORTED_REGIONS) {
            PricingSnapshot snapshot = PricingCatalog.forRegion(region);
            String source = "classpath:/pricing/" + region + "-pricing.json";
            Instant effectiveAt = parseInstant(snapshot.meta().fetchedAt());
            seedScalar(region, HOURS_PER_MONTH, "month_hours", snapshot.hoursPerMonth(), source, effectiveAt);
            seedScalar(region, USD_TO_KRW, "fx", snapshot.usdToKrw(), source, effectiveAt);
            seedOptions(region, EC2_INSTANCE_HOUR, "hour", snapshot.ec2InstanceHour(), source, effectiveAt);
            seedScalar(region, EBS_GP3_GB_MONTH, "gb_month", snapshot.ebsGp3GbMonth(), source, effectiveAt);
            seedScalar(region, NAT_GATEWAY_HOUR, "hour", snapshot.natGatewayHour(), source, effectiveAt);
            seedScalar(region, NAT_GATEWAY_GB_PROCESSED, "gb", snapshot.natGatewayGbProcessed(), source, effectiveAt);
            seedOptions(region, RDS_INSTANCE_HOUR, "hour", snapshot.rdsInstanceHour(), source, effectiveAt);
            seedScalar(region, RDS_STORAGE_GB_MONTH, "gb_month", snapshot.rdsStorageGbMonth(), source, effectiveAt);
            seedOptions(region, REDIS_NODE_HOUR, "hour", snapshot.redisNodeHour(), source, effectiveAt);
            seedScalar(region, FARGATE_VCPU_HOUR_X86, "vcpu_hour", snapshot.fargateVcpuHourX86(), source, effectiveAt);
            seedScalar(region, FARGATE_GB_HOUR_X86, "gb_hour", snapshot.fargateGbHourX86(), source, effectiveAt);
            seedScalar(region, FARGATE_VCPU_HOUR_ARM, "vcpu_hour", snapshot.fargateVcpuHourArm(), source, effectiveAt);
            seedScalar(region, FARGATE_GB_HOUR_ARM, "gb_hour", snapshot.fargateGbHourArm(), source, effectiveAt);
            seedScalar(region, ALB_HOUR, "hour", snapshot.albHour(), source, effectiveAt);
            seedScalar(region, ALB_LCU_HOUR, "lcu_hour", snapshot.albLcuHour(), source, effectiveAt);
            seedScalar(region, S3_STANDARD_GB_MONTH, "gb_month", snapshot.s3StandardGbMonth(), source, effectiveAt);
            seedScalar(region, CLOUDFRONT_GB_TRANSFER, "gb", snapshot.cloudFrontGbTransfer(), source, effectiveAt);
            seedScalar(region, LAMBDA_PER_REQUEST, "request", snapshot.lambdaPerRequest(), source, effectiveAt);
            seedScalar(region, LAMBDA_GB_SECOND, "gb_second", snapshot.lambdaGbSecond(), source, effectiveAt);
            seedScalar(region, DYNAMODB_PER_READ_UNIT, "read_request", snapshot.dynamoDbPerReadUnit(), source, effectiveAt);
            seedScalar(region, DYNAMODB_PER_WRITE_UNIT, "write_request", snapshot.dynamoDbPerWriteUnit(), source, effectiveAt);
            seedScalar(region, DYNAMODB_STORAGE_GB_MONTH, "gb_month", snapshot.dynamoDbStorageGbMonth(), source, effectiveAt);
            seedScalar(region, DATA_TRANSFER_OUT_GB, "gb", snapshot.dataTransferOutGb(), source, effectiveAt);
            seedScalar(region, VPC_ENDPOINT_HOUR, "hour", snapshot.vpcEndpointHour(), source, effectiveAt);
            seedScalar(region, VPC_ENDPOINT_GB_PROCESSED, "gb", snapshot.vpcEndpointGbProcessed(), source, effectiveAt);
            seedScalar(region, TRANSIT_GATEWAY_HOUR, "hour", snapshot.transitGatewayHour(), source, effectiveAt);
            seedScalar(region, TRANSIT_GATEWAY_GB_PROCESSED, "gb", snapshot.transitGatewayGbProcessed(), source, effectiveAt);
            seedScalar(region, VPC_PEERING_GB_PROCESSED, "gb", snapshot.vpcPeeringGbProcessed(), source, effectiveAt);
            seedScalar(region, API_GATEWAY_HTTP_PER_REQUEST, "request", snapshot.apiGatewayHttpPerRequest(), source, effectiveAt);
            seedScalar(region, SQS_PER_REQUEST, "request", snapshot.sqsPerRequest(), source, effectiveAt);
            seedScalar(region, SNS_PER_REQUEST, "request", snapshot.snsPerRequest(), source, effectiveAt);
            seedScalar(region, EFS_STANDARD_GB_MONTH, "gb_month", snapshot.efsStandardGbMonth(), source, effectiveAt);
            seedScalar(region, S3_STANDARD_IA_GB_MONTH, "gb_month", snapshot.s3StandardIaGbMonth(), source, effectiveAt);
            seedScalar(region, EBS_GP2_GB_MONTH, "gb_month", snapshot.ebsGp2GbMonth(), source, effectiveAt);
            seedScalar(region, EBS_IO2_GB_MONTH, "gb_month", snapshot.ebsIo2GbMonth(), source, effectiveAt);
            seedOptions(region, RDS_INSTANCE_HOUR_MULTI_AZ, "hour", snapshot.rdsInstanceHourMultiAz(), source, effectiveAt);
            seedScalar(region, LAMBDA_PER_REQUEST_ARM, "request", snapshot.lambdaPerRequestArm(), source, effectiveAt);
            seedScalar(region, LAMBDA_GB_SECOND_ARM, "gb_second", snapshot.lambdaGbSecondArm(), source, effectiveAt);
            seedScalar(region, DYNAMODB_PROVISIONED_READ_UNIT_HOUR, "read_unit_hour", snapshot.dynamoDbProvisionedReadUnitHour(), source, effectiveAt);
            seedScalar(region, DYNAMODB_PROVISIONED_WRITE_UNIT_HOUR, "write_unit_hour", snapshot.dynamoDbProvisionedWriteUnitHour(), source, effectiveAt);
            seedScalar(region, AURORA_SERVERLESS_ACU_HOUR, "acu_hour", snapshot.auroraServerlessAcuHour(), source, effectiveAt);
            seedScalar(region, AURORA_STORAGE_GB_MONTH, "gb_month", snapshot.auroraStorageGbMonth(), source, effectiveAt);
            seedOptions(region, OPENSEARCH_INSTANCE_HOUR, "hour", snapshot.openSearchInstanceHour(), source, effectiveAt);
            seedScalar(region, OPENSEARCH_GP3_GB_MONTH, "gb_month", snapshot.openSearchGp3GbMonth(), source, effectiveAt);
            seedScalar(region, SECRETS_MANAGER_PER_SECRET_MONTH, "secret_month", snapshot.secretsManagerPerSecretMonth(), source, effectiveAt);
            seedScalar(region, SECRETS_MANAGER_PER_API_REQUEST, "request", snapshot.secretsManagerPerApiRequest(), source, effectiveAt);
            seedScalar(region, ROUTE53_HOSTED_ZONE_MONTH, "zone_month", snapshot.route53HostedZoneMonth(), source, effectiveAt);
            seedScalar(region, ROUTE53_PER_QUERY, "million_queries", snapshot.route53PerQuery(), source, effectiveAt);
            seedScalar(region, EVENTBRIDGE_PER_EVENT, "event", snapshot.eventBridgePerEvent(), source, effectiveAt);
            seedScalar(region, STEP_FUNCTIONS_PER_STATE_TRANSITION, "state_transition", snapshot.stepFunctionsPerStateTransition(), source, effectiveAt);
            seedScalar(region, CLOUDWATCH_LOGS_INGEST_GB, "gb", snapshot.cloudWatchLogsIngestGb(), source, effectiveAt);
            seedScalar(region, CLOUDWATCH_LOGS_STORAGE_GB_MONTH, "gb_month", snapshot.cloudWatchLogsStorageGbMonth(), source, effectiveAt);
            seedScalar(region, CLOUDWATCH_ALARM_MONTH, "alarm_month", snapshot.cloudWatchAlarmMonth(), source, effectiveAt);
            seedScalar(region, KINESIS_SHARD_HOUR, "shard_hour", snapshot.kinesisShardHour(), source, effectiveAt);
            seedScalar(region, KINESIS_PUT_PAYLOAD_UNIT, "put_payload_unit", snapshot.kinesisPutPayloadUnit(), source, effectiveAt);
            seedScalar(region, WAF_WEB_ACL_MONTH, "web_acl_month", snapshot.wafWebAclMonth(), source, effectiveAt);
            seedScalar(region, WAF_RULE_MONTH, "rule_month", snapshot.wafRuleMonth(), source, effectiveAt);
            seedScalar(region, WAF_PER_REQUEST, "request", snapshot.wafPerRequest(), source, effectiveAt);
            seedScalar(region, SES_PER_RECIPIENT, "recipient", snapshot.sesPerRecipient(), source, effectiveAt);
            seedScalar(region, SES_ATTACHMENT_GB, "gb", snapshot.sesAttachmentGb(), source, effectiveAt);
            seedOptions(region, EKS_CLUSTER_HOUR, "hour", snapshot.eksClusterHour(), source, effectiveAt);
            seedOptions(region, ECR_STORAGE_GB_MONTH, "gb_month", snapshot.ecrStorageGbMonth(), source, effectiveAt);
            seedScalar(region, CLOUDTRAIL_PAID_EVENT, "event", snapshot.cloudTrailPaidEvent(), source, effectiveAt);
            seedScalar(region, CLOUDTRAIL_DATA_EVENT, "event", snapshot.cloudTrailDataEvent(), source, effectiveAt);
            seedScalar(region, CLOUDTRAIL_QUERY_GB, "gb", snapshot.cloudTrailQueryGb(), source, effectiveAt);
            seedOptions(region, GLUE_DPU_HOUR, "dpu_hour", snapshot.glueDpuHour(), source, effectiveAt);
            seedScalar(region, GLUE_CRAWLER_DPU_HOUR, "dpu_hour", snapshot.glueCrawlerDpuHour(), source, effectiveAt);
            seedScalar(region, ATHENA_SCANNED_TB, "tb", snapshot.athenaScannedTb(), source, effectiveAt);
            seedOptions(region, REDSHIFT_RA3_LARGE_HOUR, "hour", snapshot.redshiftRa3LargeHour(), source, effectiveAt);
            seedScalar(region, REDSHIFT_STORAGE_GB_MONTH, "gb_month", snapshot.redshiftStorageGbMonth(), source, effectiveAt);
            seedOptions(region, MSK_BROKER_HOUR, "hour", snapshot.mskBrokerHour(), source, effectiveAt);
            seedScalar(region, MSK_STORAGE_GB_MONTH, "gb_month", snapshot.mskStorageGbMonth(), source, effectiveAt);
            seedScalar(region, APPSYNC_GRAPHQL_REQUEST, "request", snapshot.appSyncGraphqlRequest(), source, effectiveAt);
            seedScalar(region, APPSYNC_REALTIME_MINUTE, "minute", snapshot.appSyncRealtimeMinute(), source, effectiveAt);
            seedOptions(region, COGNITO_MAU, "mau", snapshot.cognitoMau(), source, effectiveAt);
            seedOptions(region, BEDROCK_NOVA_LITE_INPUT_1K_TOKENS, "1k_tokens", snapshot.bedrockNovaLiteInput1kTokens(), source, effectiveAt);
            seedOptions(region, BEDROCK_NOVA_LITE_OUTPUT_1K_TOKENS, "1k_tokens", snapshot.bedrockNovaLiteOutput1kTokens(), source, effectiveAt);
            seedOptions(region, SAGEMAKER_ML_M5_LARGE_HOUR, "hour", snapshot.sageMakerMlM5LargeHour(), source, effectiveAt);
            seedOptions(region, SSM_ADVANCED_PARAMETER_MONTH, "parameter_month", snapshot.ssmAdvancedParameterMonth(), source, effectiveAt);
            seedScalar(region, SSM_PARAMETER_API_REQUEST, "request", snapshot.ssmParameterApiRequest(), source, effectiveAt);
            seedScalar(region, KMS_KEY_MONTH, "key_month", snapshot.kmsKeyMonth(), source, effectiveAt);
            seedOptions(region, KMS_REQUEST, "request", snapshot.kmsRequest(), source, effectiveAt);
            seedOptions(region, CLOUDHSM_HOUR, "hour", snapshot.cloudHsmHour(), source, effectiveAt);
            seedOptions(region, BACKUP_WARM_STORAGE_GB_MONTH, "gb_month", snapshot.backupWarmStorageGbMonth(), source, effectiveAt);
            seedScalar(region, NLB_HOUR, "hour", snapshot.nlbHour(), source, effectiveAt);
            seedScalar(region, NLB_LCU_HOUR, "lcu_hour", snapshot.nlbLcuHour(), source, effectiveAt);
            seedScalar(region, GWLB_HOUR, "hour", snapshot.gwlbHour(), source, effectiveAt);
            seedScalar(region, GWLB_LCU_HOUR, "lcu_hour", snapshot.gwlbLcuHour(), source, effectiveAt);
        }
    }

    private Map<String, Map<String, BigDecimal>> group(List<AwsPriceDimension> dimensions) {
        Map<String, Map<String, BigDecimal>> grouped = new HashMap<>();
        for (AwsPriceDimension dimension : dimensions) {
            grouped.computeIfAbsent(dimension.getPriceKey(), ignored -> new HashMap<>())
                    .put(dimension.getOptionKey(), dimension.getPriceUsd());
        }
        return grouped;
    }

    private Map<String, Map<String, BigDecimal>> dimensionsFromSnapshot(PricingSnapshot snapshot) {
        Map<String, Map<String, BigDecimal>> dimensions = new HashMap<>();
        putScalar(dimensions, HOURS_PER_MONTH, snapshot.hoursPerMonth());
        putScalar(dimensions, USD_TO_KRW, snapshot.usdToKrw());
        putOptions(dimensions, EC2_INSTANCE_HOUR, snapshot.ec2InstanceHour());
        putScalar(dimensions, EBS_GP3_GB_MONTH, snapshot.ebsGp3GbMonth());
        putScalar(dimensions, NAT_GATEWAY_HOUR, snapshot.natGatewayHour());
        putScalar(dimensions, NAT_GATEWAY_GB_PROCESSED, snapshot.natGatewayGbProcessed());
        putOptions(dimensions, RDS_INSTANCE_HOUR, snapshot.rdsInstanceHour());
        putScalar(dimensions, RDS_STORAGE_GB_MONTH, snapshot.rdsStorageGbMonth());
        putOptions(dimensions, REDIS_NODE_HOUR, snapshot.redisNodeHour());
        putScalar(dimensions, FARGATE_VCPU_HOUR_X86, snapshot.fargateVcpuHourX86());
        putScalar(dimensions, FARGATE_GB_HOUR_X86, snapshot.fargateGbHourX86());
        putScalar(dimensions, FARGATE_VCPU_HOUR_ARM, snapshot.fargateVcpuHourArm());
        putScalar(dimensions, FARGATE_GB_HOUR_ARM, snapshot.fargateGbHourArm());
        putScalar(dimensions, ALB_HOUR, snapshot.albHour());
        putScalar(dimensions, ALB_LCU_HOUR, snapshot.albLcuHour());
        putScalar(dimensions, S3_STANDARD_GB_MONTH, snapshot.s3StandardGbMonth());
        putScalar(dimensions, CLOUDFRONT_GB_TRANSFER, snapshot.cloudFrontGbTransfer());
        putScalar(dimensions, LAMBDA_PER_REQUEST, snapshot.lambdaPerRequest());
        putScalar(dimensions, LAMBDA_GB_SECOND, snapshot.lambdaGbSecond());
        putScalar(dimensions, DYNAMODB_PER_READ_UNIT, snapshot.dynamoDbPerReadUnit());
        putScalar(dimensions, DYNAMODB_PER_WRITE_UNIT, snapshot.dynamoDbPerWriteUnit());
        putScalar(dimensions, DYNAMODB_STORAGE_GB_MONTH, snapshot.dynamoDbStorageGbMonth());
        putScalar(dimensions, DATA_TRANSFER_OUT_GB, snapshot.dataTransferOutGb());
        putScalar(dimensions, VPC_ENDPOINT_HOUR, snapshot.vpcEndpointHour());
        putScalar(dimensions, VPC_ENDPOINT_GB_PROCESSED, snapshot.vpcEndpointGbProcessed());
        putScalar(dimensions, TRANSIT_GATEWAY_HOUR, snapshot.transitGatewayHour());
        putScalar(dimensions, TRANSIT_GATEWAY_GB_PROCESSED, snapshot.transitGatewayGbProcessed());
        putScalar(dimensions, VPC_PEERING_GB_PROCESSED, snapshot.vpcPeeringGbProcessed());
        putScalar(dimensions, API_GATEWAY_HTTP_PER_REQUEST, snapshot.apiGatewayHttpPerRequest());
        putScalar(dimensions, SQS_PER_REQUEST, snapshot.sqsPerRequest());
        putScalar(dimensions, SNS_PER_REQUEST, snapshot.snsPerRequest());
        putScalar(dimensions, EFS_STANDARD_GB_MONTH, snapshot.efsStandardGbMonth());
        putScalar(dimensions, S3_STANDARD_IA_GB_MONTH, snapshot.s3StandardIaGbMonth());
        putScalar(dimensions, EBS_GP2_GB_MONTH, snapshot.ebsGp2GbMonth());
        putScalar(dimensions, EBS_IO2_GB_MONTH, snapshot.ebsIo2GbMonth());
        putOptions(dimensions, RDS_INSTANCE_HOUR_MULTI_AZ, snapshot.rdsInstanceHourMultiAz());
        putScalar(dimensions, LAMBDA_PER_REQUEST_ARM, snapshot.lambdaPerRequestArm());
        putScalar(dimensions, LAMBDA_GB_SECOND_ARM, snapshot.lambdaGbSecondArm());
        putScalar(dimensions, DYNAMODB_PROVISIONED_READ_UNIT_HOUR, snapshot.dynamoDbProvisionedReadUnitHour());
        putScalar(dimensions, DYNAMODB_PROVISIONED_WRITE_UNIT_HOUR, snapshot.dynamoDbProvisionedWriteUnitHour());
        putScalar(dimensions, AURORA_SERVERLESS_ACU_HOUR, snapshot.auroraServerlessAcuHour());
        putScalar(dimensions, AURORA_STORAGE_GB_MONTH, snapshot.auroraStorageGbMonth());
        putOptions(dimensions, OPENSEARCH_INSTANCE_HOUR, snapshot.openSearchInstanceHour());
        putScalar(dimensions, OPENSEARCH_GP3_GB_MONTH, snapshot.openSearchGp3GbMonth());
        putScalar(dimensions, SECRETS_MANAGER_PER_SECRET_MONTH, snapshot.secretsManagerPerSecretMonth());
        putScalar(dimensions, SECRETS_MANAGER_PER_API_REQUEST, snapshot.secretsManagerPerApiRequest());
        putScalar(dimensions, ROUTE53_HOSTED_ZONE_MONTH, snapshot.route53HostedZoneMonth());
        putScalar(dimensions, ROUTE53_PER_QUERY, snapshot.route53PerQuery());
        putScalar(dimensions, EVENTBRIDGE_PER_EVENT, snapshot.eventBridgePerEvent());
        putScalar(dimensions, STEP_FUNCTIONS_PER_STATE_TRANSITION, snapshot.stepFunctionsPerStateTransition());
        putScalar(dimensions, CLOUDWATCH_LOGS_INGEST_GB, snapshot.cloudWatchLogsIngestGb());
        putScalar(dimensions, CLOUDWATCH_LOGS_STORAGE_GB_MONTH, snapshot.cloudWatchLogsStorageGbMonth());
        putScalar(dimensions, CLOUDWATCH_ALARM_MONTH, snapshot.cloudWatchAlarmMonth());
        putScalar(dimensions, KINESIS_SHARD_HOUR, snapshot.kinesisShardHour());
        putScalar(dimensions, KINESIS_PUT_PAYLOAD_UNIT, snapshot.kinesisPutPayloadUnit());
        putScalar(dimensions, WAF_WEB_ACL_MONTH, snapshot.wafWebAclMonth());
        putScalar(dimensions, WAF_RULE_MONTH, snapshot.wafRuleMonth());
        putScalar(dimensions, WAF_PER_REQUEST, snapshot.wafPerRequest());
        putScalar(dimensions, SES_PER_RECIPIENT, snapshot.sesPerRecipient());
        putScalar(dimensions, SES_ATTACHMENT_GB, snapshot.sesAttachmentGb());
        putOptions(dimensions, EKS_CLUSTER_HOUR, snapshot.eksClusterHour());
        putOptions(dimensions, ECR_STORAGE_GB_MONTH, snapshot.ecrStorageGbMonth());
        putScalar(dimensions, CLOUDTRAIL_PAID_EVENT, snapshot.cloudTrailPaidEvent());
        putScalar(dimensions, CLOUDTRAIL_DATA_EVENT, snapshot.cloudTrailDataEvent());
        putScalar(dimensions, CLOUDTRAIL_QUERY_GB, snapshot.cloudTrailQueryGb());
        putOptions(dimensions, GLUE_DPU_HOUR, snapshot.glueDpuHour());
        putScalar(dimensions, GLUE_CRAWLER_DPU_HOUR, snapshot.glueCrawlerDpuHour());
        putScalar(dimensions, ATHENA_SCANNED_TB, snapshot.athenaScannedTb());
        putOptions(dimensions, REDSHIFT_RA3_LARGE_HOUR, snapshot.redshiftRa3LargeHour());
        putScalar(dimensions, REDSHIFT_STORAGE_GB_MONTH, snapshot.redshiftStorageGbMonth());
        putOptions(dimensions, MSK_BROKER_HOUR, snapshot.mskBrokerHour());
        putScalar(dimensions, MSK_STORAGE_GB_MONTH, snapshot.mskStorageGbMonth());
        putScalar(dimensions, APPSYNC_GRAPHQL_REQUEST, snapshot.appSyncGraphqlRequest());
        putScalar(dimensions, APPSYNC_REALTIME_MINUTE, snapshot.appSyncRealtimeMinute());
        putOptions(dimensions, COGNITO_MAU, snapshot.cognitoMau());
        putOptions(dimensions, BEDROCK_NOVA_LITE_INPUT_1K_TOKENS, snapshot.bedrockNovaLiteInput1kTokens());
        putOptions(dimensions, BEDROCK_NOVA_LITE_OUTPUT_1K_TOKENS, snapshot.bedrockNovaLiteOutput1kTokens());
        putOptions(dimensions, SAGEMAKER_ML_M5_LARGE_HOUR, snapshot.sageMakerMlM5LargeHour());
        putOptions(dimensions, SSM_ADVANCED_PARAMETER_MONTH, snapshot.ssmAdvancedParameterMonth());
        putScalar(dimensions, SSM_PARAMETER_API_REQUEST, snapshot.ssmParameterApiRequest());
        putScalar(dimensions, KMS_KEY_MONTH, snapshot.kmsKeyMonth());
        putOptions(dimensions, KMS_REQUEST, snapshot.kmsRequest());
        putOptions(dimensions, CLOUDHSM_HOUR, snapshot.cloudHsmHour());
        putOptions(dimensions, BACKUP_WARM_STORAGE_GB_MONTH, snapshot.backupWarmStorageGbMonth());
        putScalar(dimensions, NLB_HOUR, snapshot.nlbHour());
        putScalar(dimensions, NLB_LCU_HOUR, snapshot.nlbLcuHour());
        putScalar(dimensions, GWLB_HOUR, snapshot.gwlbHour());
        putScalar(dimensions, GWLB_LCU_HOUR, snapshot.gwlbLcuHour());
        return dimensions;
    }

    private void putScalar(Map<String, Map<String, BigDecimal>> dimensions, String priceKey, BigDecimal price) {
        dimensions.put(priceKey, Map.of(DEFAULT_OPTION, price));
    }

    private void putOptions(Map<String, Map<String, BigDecimal>> dimensions, String priceKey, Map<String, BigDecimal> prices) {
        dimensions.put(priceKey, prices);
    }

    private BigDecimal scalar(Map<String, Map<String, BigDecimal>> grouped, String priceKey) {
        BigDecimal value = grouped.getOrDefault(priceKey, Map.of()).get(DEFAULT_OPTION);
        if (value == null) {
            throw new IllegalStateException("Missing price dimension: " + priceKey);
        }
        return value;
    }

    private Map<String, BigDecimal> options(Map<String, Map<String, BigDecimal>> grouped, String priceKey) {
        Map<String, BigDecimal> value = grouped.get(priceKey);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing price dimension options: " + priceKey);
        }
        return Map.copyOf(value);
    }

    private void seedScalar(String region, String priceKey, String unit, BigDecimal price, String source, Instant effectiveAt) {
        seed(region, priceKey, DEFAULT_OPTION, unit, price, source, effectiveAt);
    }

    private void seedOptions(String region, String priceKey, String unit, Map<String, BigDecimal> prices, String source, Instant effectiveAt) {
        priceDimensionRepository.deleteByRegionAndPriceKeyAndOptionKey(region, priceKey, DEFAULT_OPTION);
        prices.forEach((optionKey, price) -> seed(region, priceKey, optionKey, unit, price, source, effectiveAt));
    }

    private void seed(String region, String priceKey, String optionKey, String unit, BigDecimal price, String source, Instant effectiveAt) {
        if (price == null) {
            return;
        }
        priceDimensionRepository.findByRegionAndPriceKeyAndOptionKey(region, priceKey, optionKey)
                .ifPresentOrElse(
                        dimension -> dimension.updatePrice(unit, price, source, effectiveAt),
                        () -> priceDimensionRepository.save(new AwsPriceDimension(
                                region,
                                priceKey,
                                optionKey,
                                unit,
                                price,
                                source,
                                effectiveAt
                        ))
                );
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }
}
