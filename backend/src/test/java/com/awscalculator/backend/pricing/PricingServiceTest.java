package com.awscalculator.backend.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private AwsPriceDimensionRepository priceDimensionRepository;

    @InjectMocks
    private PricingService pricingService;

    @Test
    void forRegionBuildsSnapshotFromDatabaseDimensions() {
        when(priceDimensionRepository.findByRegion("ap-northeast-2")).thenReturn(dimensions());

        PricingSnapshot snapshot = pricingService.forRegion("ap-northeast-2");

        assertThat(snapshot.meta().source()).isEqualTo("database:aws_price_dimensions");
        assertThat(snapshot.rdsInstanceHour()).containsEntry("db.t4g.micro", new BigDecimal("0.025"));
        assertThat(snapshot.ec2InstanceHour()).containsEntry("t3.micro", new BigDecimal("0.013"));
        assertThat(snapshot.usdToKrw()).isEqualByComparingTo("1380");
        assertThat(snapshot.fargateVcpuHourArm()).isEqualByComparingTo("0.03725");
    }

    private List<AwsPriceDimension> dimensions() {
        Instant effectiveAt = Instant.parse("2026-08-31T14:39:57Z");
        return List.of(
                scalar("hours_per_month", "month_hours", "730", effectiveAt),
                scalar("usd_to_krw", "fx", "1380", effectiveAt),
                option("ec2_instance_hour", "t3.micro", "hour", "0.013", effectiveAt),
                scalar("ebs_gp3_gb_month", "gb_month", "0.0912", effectiveAt),
                scalar("nat_gateway_hour", "hour", "0.059", effectiveAt),
                scalar("nat_gateway_gb_processed", "gb", "0.059", effectiveAt),
                option("rds_instance_hour", "db.t4g.micro", "hour", "0.025", effectiveAt),
                scalar("rds_storage_gb_month", "gb_month", "0.131", effectiveAt),
                option("redis_node_hour", "cache.t4g.micro", "hour", "0.024", effectiveAt),
                scalar("fargate_vcpu_hour_x86", "vcpu_hour", "0.04656", effectiveAt),
                scalar("fargate_gb_hour_x86", "gb_hour", "0.00511", effectiveAt),
                scalar("fargate_vcpu_hour_arm", "vcpu_hour", "0.03725", effectiveAt),
                scalar("fargate_gb_hour_arm", "gb_hour", "0.00409", effectiveAt),
                scalar("alb_hour", "hour", "0.0225", effectiveAt),
                scalar("alb_lcu_hour", "lcu_hour", "0.008", effectiveAt),
                scalar("s3_standard_gb_month", "gb_month", "0.025", effectiveAt),
                scalar("cloudfront_gb_transfer", "gb", "0.12", effectiveAt),
                scalar("lambda_per_request", "request", "0.0000002", effectiveAt),
                scalar("lambda_gb_second", "gb_second", "0.0000166667", effectiveAt),
                scalar("dynamodb_per_read_unit", "read_request", "0.000000125", effectiveAt),
                scalar("dynamodb_per_write_unit", "write_request", "0.000000625", effectiveAt),
                scalar("dynamodb_storage_gb_month", "gb_month", "0.25", effectiveAt),
                scalar("data_transfer_out_gb", "gb", "0.126", effectiveAt),
                scalar("vpc_endpoint_hour", "hour", "0.012", effectiveAt),
                scalar("vpc_endpoint_gb_processed", "gb", "0.01", effectiveAt),
                scalar("transit_gateway_hour", "hour", "0.05", effectiveAt),
                scalar("transit_gateway_gb_processed", "gb", "0.02", effectiveAt),
                scalar("vpc_peering_gb_processed", "gb", "0.01", effectiveAt),
                scalar("api_gateway_http_per_request", "request", "0.000001", effectiveAt),
                scalar("sqs_per_request", "request", "0.0000004", effectiveAt),
                scalar("sns_per_request", "request", "0.0000005", effectiveAt),
                scalar("efs_standard_gb_month", "gb_month", "0.36", effectiveAt),
                scalar("s3_standard_ia_gb_month", "gb_month", "0.0138", effectiveAt),
                scalar("ebs_gp2_gb_month", "gb_month", "0.12", effectiveAt),
                scalar("ebs_io2_gb_month", "gb_month", "0.142", effectiveAt),
                option("rds_instance_hour_multi_az", "db.t4g.micro", "hour", "0.05", effectiveAt),
                scalar("lambda_per_request_arm", "request", "0.0000002", effectiveAt),
                scalar("lambda_gb_second_arm", "gb_second", "0.0000133334", effectiveAt),
                scalar("dynamodb_provisioned_read_unit_hour", "read_unit_hour", "0.00013", effectiveAt),
                scalar("dynamodb_provisioned_write_unit_hour", "write_unit_hour", "0.00065", effectiveAt),
                scalar("aurora_serverless_acu_hour", "acu_hour", "0.2", effectiveAt),
                scalar("aurora_storage_gb_month", "gb_month", "0.12", effectiveAt),
                option("opensearch_instance_hour", "t3.small.search", "hour", "0.052", effectiveAt),
                scalar("opensearch_gp3_gb_month", "gb_month", "0.122", effectiveAt),
                scalar("secrets_manager_per_secret_month", "secret_month", "0.4", effectiveAt),
                scalar("secrets_manager_per_api_request", "request", "0.000005", effectiveAt),
                scalar("route53_hosted_zone_month", "zone_month", "0.5", effectiveAt),
                scalar("route53_per_query", "million_queries", "0.4", effectiveAt),
                scalar("eventbridge_per_event", "event", "0.000001", effectiveAt),
                scalar("step_functions_per_state_transition", "state_transition", "0.000025", effectiveAt),
                scalar("cloudwatch_logs_ingest_gb", "gb", "0.5", effectiveAt),
                scalar("cloudwatch_logs_storage_gb_month", "gb_month", "0.03", effectiveAt),
                scalar("cloudwatch_alarm_month", "alarm_month", "0.1", effectiveAt),
                scalar("kinesis_shard_hour", "shard_hour", "0.015", effectiveAt),
                scalar("kinesis_put_payload_unit", "put_payload_unit", "0.000000014", effectiveAt),
                scalar("waf_web_acl_month", "web_acl_month", "5", effectiveAt),
                scalar("waf_rule_month", "rule_month", "1", effectiveAt),
                scalar("waf_per_request", "request", "0.0000006", effectiveAt),
                scalar("ses_per_recipient", "recipient", "0.0001", effectiveAt),
                scalar("ses_attachment_gb", "gb", "0.12", effectiveAt),
                option("eks_cluster_hour", "standard", "hour", "0.1", effectiveAt),
                option("ecr_storage_gb_month", "standard", "gb_month", "0.1", effectiveAt),
                scalar("cloudtrail_paid_event", "event", "0.00002", effectiveAt),
                scalar("cloudtrail_data_event", "event", "0.000001", effectiveAt),
                scalar("cloudtrail_query_gb", "gb", "0.005", effectiveAt),
                option("glue_dpu_hour", "standard", "dpu_hour", "0.44", effectiveAt),
                scalar("glue_crawler_dpu_hour", "dpu_hour", "0.44", effectiveAt),
                scalar("athena_scanned_tb", "tb", "5", effectiveAt),
                option("redshift_ra3_large_hour", "ra3.large", "hour", "0.543", effectiveAt),
                scalar("redshift_storage_gb_month", "gb_month", "0.024", effectiveAt),
                option("msk_broker_hour", "m5.large", "hour", "0.21", effectiveAt),
                scalar("msk_storage_gb_month", "gb_month", "0.1", effectiveAt),
                scalar("appsync_graphql_request", "request", "0.000004", effectiveAt),
                scalar("appsync_realtime_minute", "minute", "0.00000008", effectiveAt),
                option("cognito_mau", "lite", "mau", "0.0055", effectiveAt),
                option("bedrock_nova_lite_input_1k_tokens", "nova_lite", "1k_tokens", "0.00006", effectiveAt),
                option("bedrock_nova_lite_output_1k_tokens", "nova_lite", "1k_tokens", "0.00024", effectiveAt),
                option("sagemaker_ml_m5_large_hour", "ml.m5.large", "hour", "0.115", effectiveAt),
                option("ssm_advanced_parameter_month", "advanced", "parameter_month", "0.00007", effectiveAt),
                scalar("ssm_parameter_api_request", "request", "0.000005", effectiveAt),
                scalar("kms_key_month", "key_month", "1", effectiveAt),
                option("kms_request", "symmetric", "request", "0.000003", effectiveAt),
                option("cloudhsm_hour", "hsm1", "hour", "1.6", effectiveAt),
                option("backup_warm_storage_gb_month", "ebs_lagv", "gb_month", "0.0575", effectiveAt),
                scalar("nlb_hour", "hour", "0.0225", effectiveAt),
                scalar("nlb_lcu_hour", "lcu_hour", "0.006", effectiveAt),
                scalar("gwlb_hour", "hour", "0.0125", effectiveAt),
                scalar("gwlb_lcu_hour", "lcu_hour", "0.004", effectiveAt)
        );
    }

    private AwsPriceDimension scalar(String priceKey, String unit, String price, Instant effectiveAt) {
        return option(priceKey, AwsPriceDimension.DEFAULT_OPTION, unit, price, effectiveAt);
    }

    private AwsPriceDimension option(String priceKey, String optionKey, String unit, String price, Instant effectiveAt) {
        return new AwsPriceDimension(
                "ap-northeast-2",
                priceKey,
                optionKey,
                unit,
                new BigDecimal(price),
                "test",
                effectiveAt
        );
    }
}
