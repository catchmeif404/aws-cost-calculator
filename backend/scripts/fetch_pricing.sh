#!/usr/bin/env bash
# Fetches real on-demand pricing from AWS's public Price List Bulk API
# (https://pricing.us-east-1.amazonaws.com — no AWS credentials required) for one or more
# regions and writes a compact snapshot per region to
# src/main/resources/pricing/<region>-pricing.json, which PricingCatalog.java loads at startup.
#
# Usage:
#   scripts/fetch_pricing.sh                # fetch all supported regions
#   scripts/fetch_pricing.sh ap-northeast-2  # fetch just one region
#
# Re-run this periodically (AWS prices change occasionally) and restart the backend to pick up
# fresh numbers — this script is the "how to refresh" half of the pricing story documented in
# docs/PRODUCT_SPEC.md.
#
# Requires: curl, jq
set -euo pipefail

BASE_URL="https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws"
RESOURCES_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/src/main/resources/pricing"

# Supported regions, with the two bits of metadata the Price List doesn't let us derive from
# the region code alone: AWS's human "location" display string for that region, and which
# CloudFront viewer-geography group its edge locations fall into (CloudFront's public-facing
# data-transfer pricing is grouped by continent, not by the distribution's origin region).
region_display_name() {
  case "$1" in
    ap-northeast-2) echo "Asia Pacific (Seoul)" ;;
    ap-northeast-1) echo "Asia Pacific (Tokyo)" ;;
    us-east-1) echo "US East (N. Virginia)" ;;
    us-west-2) echo "US West (Oregon)" ;;
    eu-central-1) echo "EU (Frankfurt)" ;;
    *) echo "Unknown region: $1" >&2; exit 1 ;;
  esac
}
region_cloudfront_geo() {
  case "$1" in
    ap-northeast-2|ap-northeast-1) echo "Asia Pacific" ;;
    us-east-1|us-west-2) echo "United States" ;;
    eu-central-1) echo "Europe" ;;
    *) echo "Unknown region: $1" >&2; exit 1 ;;
  esac
}

ALL_REGIONS="ap-northeast-2 ap-northeast-1 us-east-1 us-west-2 eu-central-1"
TARGET_REGIONS="${1:-$ALL_REGIONS}"

JQ_PRICE_FN='def price(sku): .terms.OnDemand[sku] | to_entries[0].value.priceDimensions | to_entries[0].value.pricePerUnit.USD | tonumber;'

# Some products (SNS requests, DynamoDB provisioned capacity, ...) list a $0 "free tier"
# priceDimension ahead of the real per-unit rate. paidTier() skips zero-priced dimensions and
# returns the cheapest remaining one — i.e. the steady-state rate once usage clears the free
# tier, which is what a monthly cost estimate should reflect (same idea as the DynamoDB storage
# block below picking the tier starting at beginRange 25, past the always-free 25 GB).
JQ_PAID_TIER_FN='def paidTier(sku): .terms.OnDemand[sku] | to_entries[0].value.priceDimensions | [.[]] | map(select(.pricePerUnit.USD|tonumber > 0)) | min_by(.beginRange|tonumber) | .pricePerUnit.USD | tonumber;'

fetch_region() {
  local REGION="$1"
  local LOCATION_NAME
  local CF_GEO
  LOCATION_NAME="$(region_display_name "$REGION")"
  CF_GEO="$(region_cloudfront_geo "$REGION")"

  local RAW_DIR
  RAW_DIR="$(mktemp -d)"
  trap 'rm -rf "$RAW_DIR"' RETURN

  echo "[$REGION] downloading Price List files into $RAW_DIR ..." >&2

  local FILE_NAMES=(AmazonEC2 AmazonRDS AmazonElastiCache AmazonECS AWSELB AmazonS3 AmazonCloudFrontOther AWSLambda AmazonDynamoDB AWSDataTransfer AmazonVPC AmazonApiGateway AWSQueueService AmazonSNS AmazonEFS AmazonES AWSSecretsManager AWSEvents AmazonStates AmazonCloudWatch AmazonKinesis awswaf AmazonSES AmazonEKS AmazonECR AWSCloudTrail AWSGlue AmazonAthena AmazonRedshift AmazonMSK AWSAppSync AmazonCognito AmazonBedrock AmazonSageMaker AWSSystemsManager awskms CloudHSM AWSBackup)
  local FILE_URLS=(
    "$BASE_URL/AmazonEC2/current/$REGION/index.json"
    "$BASE_URL/AmazonRDS/current/$REGION/index.json"
    "$BASE_URL/AmazonElastiCache/current/$REGION/index.json"
    "$BASE_URL/AmazonECS/current/$REGION/index.json"
    "$BASE_URL/AWSELB/current/$REGION/index.json"
    "$BASE_URL/AmazonS3/current/$REGION/index.json"
    "$BASE_URL/AmazonCloudFront/current/aws-other/index.json"
    "$BASE_URL/AWSLambda/current/$REGION/index.json"
    "$BASE_URL/AmazonDynamoDB/current/$REGION/index.json"
    "$BASE_URL/AWSDataTransfer/current/$REGION/index.json"
    "$BASE_URL/AmazonVPC/current/$REGION/index.json"
    "$BASE_URL/AmazonApiGateway/current/$REGION/index.json"
    "$BASE_URL/AWSQueueService/current/$REGION/index.json"
    "$BASE_URL/AmazonSNS/current/$REGION/index.json"
    "$BASE_URL/AmazonEFS/current/$REGION/index.json"
    "$BASE_URL/AmazonES/current/$REGION/index.json"
    "$BASE_URL/AWSSecretsManager/current/$REGION/index.json"
    "$BASE_URL/AWSEvents/current/$REGION/index.json"
    "$BASE_URL/AmazonStates/current/$REGION/index.json"
    "$BASE_URL/AmazonCloudWatch/current/$REGION/index.json"
    "$BASE_URL/AmazonKinesis/current/$REGION/index.json"
    "$BASE_URL/awswaf/current/$REGION/index.json"
    "$BASE_URL/AmazonSES/current/$REGION/index.json"
    "$BASE_URL/AmazonEKS/current/$REGION/index.json"
    "$BASE_URL/AmazonECR/current/$REGION/index.json"
    "$BASE_URL/AWSCloudTrail/current/$REGION/index.json"
    "$BASE_URL/AWSGlue/current/$REGION/index.json"
    "$BASE_URL/AmazonAthena/current/$REGION/index.json"
    "$BASE_URL/AmazonRedshift/current/$REGION/index.json"
    "$BASE_URL/AmazonMSK/current/$REGION/index.json"
    "$BASE_URL/AWSAppSync/current/$REGION/index.json"
    "$BASE_URL/AmazonCognito/current/$REGION/index.json"
    "$BASE_URL/AmazonBedrock/current/$REGION/index.json"
    "$BASE_URL/AmazonSageMaker/current/$REGION/index.json"
    "$BASE_URL/AWSSystemsManager/current/$REGION/index.json"
    "$BASE_URL/awskms/current/$REGION/index.json"
    "$BASE_URL/CloudHSM/current/$REGION/index.json"
    "$BASE_URL/AWSBackup/current/$REGION/index.json"
  )

  local pids=()
  local i=0
  while [ "$i" -lt "${#FILE_NAMES[@]}" ]; do
    curl -sS --max-time 300 -o "$RAW_DIR/${FILE_NAMES[$i]}.json" "${FILE_URLS[$i]}" &
    pids+=($!)
    i=$((i + 1))
  done
  for pid in "${pids[@]}"; do wait "$pid"; done

  echo "[$REGION] downloads complete. Extracting prices with jq ..." >&2

  local ec2_out rds_out redis_out ecs_out alb_out s3_out cloudfront_out lambda_out dynamodb_out datatransfer_out vpc_out
  local apigateway_out sqs_out sns_out efs_out opensearch_out secretsmanager_out eventbridge_out stepfunctions_out cloudwatch_out kinesis_out waf_out ses_out
  local eks_out ecr_out cloudtrail_out glue_out athena_out redshift_out msk_out appsync_out cognito_out bedrock_out sagemaker_out ssm_out kms_out cloudhsm_out backup_out

  ec2_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | {
    ec2InstanceHour: (
      reduce ($root.products[] | select(.attributes.operatingSystem=="Linux" and .attributes.tenancy=="Shared" and .attributes.preInstalledSw=="NA" and .attributes.capacitystatus=="Used" and (.attributes.instanceType as $t | ["t3.micro","t3.small","t3.medium","m5.large"] | index($t)))) as $p
        ({}; . + {($p.attributes.instanceType): ($root|price($p.sku))})
    ),
    ebsGp3GbMonth: (
      [$root.products[] | select(.productFamily=="Storage" and .attributes.volumeApiName=="gp3")][0].sku as $sku
      | ($root|price($sku))
    ),
    ebsGp2GbMonth: (
      [$root.products[] | select(.productFamily=="Storage" and .attributes.volumeApiName=="gp2")][0].sku as $sku
      | ($root|price($sku))
    ),
    ebsIo2GbMonth: (
      [$root.products[] | select(.productFamily=="Storage" and .attributes.volumeApiName=="io2")][0].sku as $sku
      | ($root|price($sku))
    ),
    natGatewayHour: (
      [$root.products[] | select(.productFamily=="NAT Gateway" and .attributes.groupDescription=="Hourly charge for Regional NAT Gateways")][0].sku as $sku
      | ($root|price($sku))
    ),
    natGatewayGbProcessed: (
      [$root.products[] | select(.productFamily=="NAT Gateway" and .attributes.groupDescription=="Charge for per GB data processed by NAT Gateways")][0].sku as $sku
      | ($root|price($sku))
    )
  }' "$RAW_DIR/AmazonEC2.json")

  rds_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | {
    rdsInstanceHour: (
      reduce ($root.products[] | select(.attributes.databaseEngine=="PostgreSQL" and .attributes.deploymentOption=="Single-AZ" and (.attributes.instanceType as $t | ["db.t4g.micro","db.t4g.small","db.t4g.medium","db.r6g.large"] | index($t)))) as $p
        ({}; . + {($p.attributes.instanceType): ($root|price($p.sku))})
    ),
    rdsStorageGbMonth: (
      [$root.products[] | select(.productFamily=="Database Storage" and .attributes.databaseEngine=="PostgreSQL" and .attributes.volumeType=="General Purpose-GP3" and .attributes.deploymentOption=="Single-AZ")][0].sku as $sku
      | ($root|price($sku))
    ),
    rdsInstanceHourMultiAz: (
      reduce ($root.products[] | select(.attributes.databaseEngine=="PostgreSQL" and .attributes.deploymentOption=="Multi-AZ" and (.attributes.instanceType as $t | ["db.t4g.micro","db.t4g.small","db.t4g.medium","db.r6g.large"] | index($t)))) as $p
        ({}; . + {($p.attributes.instanceType): ($root|price($p.sku))})
    ),
    auroraServerlessAcuHour: (
      [$root.products[] | select(.attributes.databaseEngine=="Aurora PostgreSQL" and .productFamily=="ServerlessV2" and (.attributes.usagetype | test("Aurora:ServerlessV2Usage$")))][0].sku as $sku
      | ($root|price($sku))
    ),
    auroraStorageGbMonth: (
      [$root.products[] | select(.attributes.databaseEngine=="Aurora PostgreSQL" and .productFamily=="Database Storage" and .attributes.volumeType=="General Purpose-Aurora")][0].sku as $sku
      | ($root|price($sku))
    )
  }' "$RAW_DIR/AmazonRDS.json")

  redis_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | {
    redisNodeHour: (
      reduce ($root.products[] | select(.attributes.cacheEngine=="Redis" and (.attributes.usagetype | test("ExtendedSupport")|not) and (.attributes.instanceType as $t | ["cache.t4g.micro","cache.t4g.small","cache.t4g.medium","cache.r6g.large"] | index($t)))) as $p
        ({}; . + {($p.attributes.instanceType): ($root|price($p.sku))})
    )
  }' "$RAW_DIR/AmazonElastiCache.json")

  # Fargate: match by shape (tenancy/cputype/memorytype/cpuArchitecture), not by the
  # region-prefixed usagetype string (e.g. "APN2-Fargate-vCPU-Hours"), so this works unmodified
  # across regions.
  ecs_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | ($root.products[] | select(.productFamily=="Compute" and .attributes.tenancy=="Shared" and .attributes.cputype=="perCPU" and .attributes.cpuArchitecture==null and .attributes.operatingSystem==null)).sku as $x86Cpu
  | ($root.products[] | select(.productFamily=="Compute" and .attributes.tenancy=="Shared" and .attributes.memorytype=="perGB" and .attributes.cpuArchitecture==null and .attributes.operatingSystem==null)).sku as $x86Gb
  | ($root.products[] | select(.productFamily=="Compute" and .attributes.tenancy=="Shared" and .attributes.cputype=="perCPU" and .attributes.cpuArchitecture=="ARM")).sku as $armCpu
  | ($root.products[] | select(.productFamily=="Compute" and .attributes.tenancy=="Shared" and .attributes.memorytype=="perGB" and .attributes.cpuArchitecture=="ARM")).sku as $armGb
  | {
    fargateVcpuHourX86: ($root|price($x86Cpu)),
    fargateGbHourX86: ($root|price($x86Gb)),
    fargateVcpuHourArm: ($root|price($armCpu)),
    fargateGbHourArm: ($root|price($armGb))
  }' "$RAW_DIR/AmazonECS.json")

  # ALB: match by groupDescription text (region-invariant), not the region-prefixed usagetype.
  alb_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | ($root.products[] | select(.productFamily=="Load Balancer-Application" and .attributes.locationType=="AWS Region" and .attributes.groupDescription=="LoadBalancer hourly usage by Application Load Balancer")).sku as $hourSku
  | ($root.products[] | select(.productFamily=="Load Balancer-Application" and .attributes.locationType=="AWS Region" and .attributes.groupDescription=="Used Application Load Balancer capacity units-hr")).sku as $lcuSku
  | ($root.products[] | select(.productFamily=="Load Balancer-Network" and .attributes.locationType=="AWS Region" and .attributes.groupDescription=="LoadBalancer hourly usage by Network Load Balancer")).sku as $nlbHourSku
  | ($root.products[] | select(.productFamily=="Load Balancer-Network" and .attributes.locationType=="AWS Region" and .attributes.groupDescription=="Used Network Load Balancer capacity units-hr")).sku as $nlbLcuSku
  | ($root.products[] | select(.productFamily=="Load Balancer-Gateway" and .attributes.locationType=="AWS Region" and .attributes.groupDescription=="LoadBalancer hourly usage by Gateway Load Balancer")).sku as $gwlbHourSku
  | ($root.products[] | select(.productFamily=="Load Balancer-Gateway" and .attributes.locationType=="AWS Region" and .attributes.groupDescription=="Used Gateway Load Balancer capacity units-hr")).sku as $gwlbLcuSku
  | {
    albHour: ($root|price($hourSku)),
    albLcuHour: ($root|price($lcuSku)),
    nlbHour: ($root|price($nlbHourSku)),
    nlbLcuHour: ($root|price($nlbLcuSku)),
    gwlbHour: ($root|price($gwlbHourSku)),
    gwlbLcuHour: ($root|price($gwlbLcuSku))
  }' "$RAW_DIR/AWSELB.json")

  s3_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | [$root.products[] | select(.productFamily=="Storage" and .attributes.storageClass=="General Purpose" and .attributes.volumeType=="Standard")][0].sku as $sku
  | ($root.terms.OnDemand[$sku] | to_entries[0].value.priceDimensions[] | select(.beginRange=="0") | .pricePerUnit.USD | tonumber) as $standard
  | [$root.products[] | select(.productFamily=="Storage" and .attributes.volumeType=="Standard - Infrequent Access")][0].sku as $iaSku
  | ($root.terms.OnDemand[$iaSku] | to_entries[0].value.priceDimensions[] | select(.beginRange=="0") | .pricePerUnit.USD | tonumber) as $ia
  | { s3StandardGbMonth: $standard, s3StandardIaGbMonth: $ia }
  ' "$RAW_DIR/AmazonS3.json")

  cloudfront_out=$(jq --arg geo "$CF_GEO" "$JQ_PRICE_FN"'
  . as $root
  | ($root.products[] | select(.productFamily=="Data Transfer" and .attributes.transferType=="CloudFront Outbound" and .attributes.fromLocation==$geo and .attributes.toLocation=="External")).sku as $sku
  | { cloudFrontGbTransfer: ($root|price($sku)) }
  ' "$RAW_DIR/AmazonCloudFrontOther.json")

  # Lambda: match by group + a non-"Any" location (region-specific row), not the region-prefixed usagetype.
  lambda_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | ([$root.products[] | select(.attributes.group=="AWS-Lambda-Requests" and .attributes.location!="Any")][0]).sku as $reqSku
  | ([$root.products[] | select(.attributes.group=="AWS-Lambda-Duration" and .attributes.location!="Any")][0]).sku as $gbsSku
  | ([$root.products[] | select(.attributes.group=="AWS-Lambda-Requests-ARM" and .attributes.location!="Any")][0]).sku as $reqArmSku
  | ([$root.products[] | select(.attributes.group=="AWS-Lambda-Duration-ARM" and .attributes.location!="Any")][0]).sku as $gbsArmSku
  | {
    lambdaPerRequest: ($root|price($reqSku)),
    lambdaGbSecond: ($root|price($gbsSku)),
    lambdaPerRequestArm: ($root|price($reqArmSku)),
    lambdaGbSecondArm: ($root|price($gbsArmSku))
  }' "$RAW_DIR/AWSLambda.json")

  dynamodb_out=$(jq "$JQ_PRICE_FN$JQ_PAID_TIER_FN"'
  . as $root
  | ($root.products[] | select(.productFamily=="Amazon DynamoDB PayPerRequest Throughput" and .attributes.group=="DDB-ReadUnits")).sku as $readSku
  | ($root.products[] | select(.productFamily=="Amazon DynamoDB PayPerRequest Throughput" and .attributes.group=="DDB-WriteUnits")).sku as $writeSku
  | ($root.products[] | select(.productFamily=="Database Storage" and .attributes.volumeType=="Amazon DynamoDB - Indexed DataStore")).sku as $storageSku
  | ($root.terms.OnDemand[$storageSku] | to_entries[0].value.priceDimensions[] | select(.beginRange=="25") | .pricePerUnit.USD | tonumber) as $storagePrice
  | ([$root.products[] | select(.productFamily=="Provisioned IOPS" and .attributes.group=="DDB-ReadUnits")][0]).sku as $provReadSku
  | ([$root.products[] | select(.productFamily=="Provisioned IOPS" and .attributes.group=="DDB-WriteUnits")][0]).sku as $provWriteSku
  | {
    dynamoDbPerReadUnit: ($root|price($readSku)),
    dynamoDbPerWriteUnit: ($root|price($writeSku)),
    dynamoDbStorageGbMonth: $storagePrice,
    dynamoDbProvisionedReadUnitHour: ($root|paidTier($provReadSku)),
    dynamoDbProvisionedWriteUnitHour: ($root|paidTier($provWriteSku))
  }' "$RAW_DIR/AmazonDynamoDB.json")

  datatransfer_out=$(jq --arg loc "$LOCATION_NAME" "$JQ_PRICE_FN"'
  . as $root
  | ($root.products[] | select(.attributes.transferType=="AWS Outbound" and .attributes.fromLocation==$loc)).sku as $sku
  | { dataTransferOutGb: ($root|price($sku)) }
  ' "$RAW_DIR/AWSDataTransfer.json")

  # VPC-adjacent billable resources — all three actually live in the AmazonVPC price file,
  # under productFamily/group values that don't literally say "Transit Gateway" or
  # "VPC Endpoint" (verified by inspection, not assumed):
  #   - Interface VPC Endpoint (what most people mean by "a VPC endpoint") is
  #     productFamily=="VpcEndpoint", endpointType=="PrivateLink".
  #   - Transit Gateway VPC attachments have productFamily==null, group=="AWSTransitGateway",
  #     attachmentType=="VPC" (there's a separate VPN attachmentType we don't want).
  #   - VPC Peering is productFamily=="VPC Peering", transferType=="Intra Region Peering Outbound"
  #     for same-region peering egress (the common case this app models).
  vpc_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="VpcEndpoint" and .attributes.endpointType=="PrivateLink" and (.attributes.usagetype | test("VpcEndpoint-Hours$")))][0]).sku as $endpointHourSku
  | ([$root.products[] | select(.productFamily=="VpcEndpoint" and .attributes.endpointType=="PrivateLink" and (.attributes.usagetype | test("VpcEndpoint-Bytes$")))][0]).sku as $endpointGbSku
  | ([$root.products[] | select(.attributes.group=="AWSTransitGateway" and .attributes.attachmentType=="VPC" and (.attributes.usagetype | test("TransitGateway-Hours$")))][0]).sku as $tgwHourSku
  | ([$root.products[] | select(.attributes.group=="AWSTransitGateway" and .attributes.attachmentType=="VPC" and (.attributes.usagetype | test("TransitGateway-Bytes$")))][0]).sku as $tgwGbSku
  | ([$root.products[] | select(.productFamily=="VPC Peering" and .attributes.transferType=="Intra Region Peering Outbound")][0]).sku as $peeringSku
  | {
    vpcEndpointHour: ($root|price($endpointHourSku)),
    vpcEndpointGbProcessed: ($root|price($endpointGbSku)),
    transitGatewayHour: ($root|price($tgwHourSku)),
    transitGatewayGbProcessed: ($root|price($tgwGbSku)),
    vpcPeeringGbProcessed: ($root|price($peeringSku))
  }' "$RAW_DIR/AmazonVPC.json")

  # HTTP API only (not REST API) — cheaper and the default choice for new serverless apps, and
  # what CloudFront's existing originType:"api_gateway" config option already implies.
  apigateway_out=$(jq --arg loc "$LOCATION_NAME" "$JQ_PRICE_FN$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="API Calls" and .attributes.operation=="ApiGatewayHttpApi" and .attributes.location==$loc)][0]).sku as $sku
  | { apiGatewayHttpPerRequest: ($root|paidTier($sku)) }
  ' "$RAW_DIR/AmazonApiGateway.json")

  sqs_out=$(jq --arg loc "$LOCATION_NAME" "$JQ_PRICE_FN$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="API Request" and .attributes.queueType=="Standard" and .attributes.location==$loc)][0]).sku as $sku
  | { sqsPerRequest: ($root|paidTier($sku)) }
  ' "$RAW_DIR/AWSQueueService.json")

  sns_out=$(jq --arg loc "$LOCATION_NAME" "$JQ_PRICE_FN$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="API Request" and .attributes.group=="SNS-Requests-Tier1" and .attributes.location==$loc)][0]).sku as $sku
  | { snsPerRequest: ($root|paidTier($sku)) }
  ' "$RAW_DIR/AmazonSNS.json")

  # EFS Standard storage class, regional (multi-AZ) — not One Zone, not Infrequent Access.
  efs_out=$(jq --arg loc "$LOCATION_NAME" "$JQ_PRICE_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="Storage" and .attributes.storageClass=="General Purpose" and .attributes.location==$loc)][0]).sku as $sku
  | { efsStandardGbMonth: ($root|price($sku)) }
  ' "$RAW_DIR/AmazonEFS.json")

  opensearch_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | {
    openSearchInstanceHour: (
      reduce ($root.products[] | select(.productFamily=="Amazon OpenSearch Service Instance" and (.attributes.instanceType as $t | ["t3.small.search","m6g.large.search"] | index($t)))) as $p
        ({}; . + {($p.attributes.instanceType): ($root|price($p.sku))})
    ),
    openSearchGp3GbMonth: (
      [$root.products[] | select(.productFamily=="Amazon OpenSearch Service Volume" and .attributes.storageMedia=="GP3")][0].sku as $sku
      | ($root|price($sku))
    )
  }' "$RAW_DIR/AmazonES.json")

  # Secrets Manager's whole price list is just two SKUs (per-secret, per-API-request) — no
  # location filter needed since the downloaded file is already region-scoped.
  secretsmanager_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="Secret")][0]).sku as $secretSku
  | ([$root.products[] | select(.productFamily=="API Request")][0]).sku as $apiSku
  | {
    secretsManagerPerSecretMonth: ($root|price($secretSku)),
    secretsManagerPerApiRequest: ($root|price($apiSku))
  }' "$RAW_DIR/AWSSecretsManager.json")

  eventbridge_out=$(jq "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="EventBridge" and .attributes.operation=="PutEvents")][0]).sku as $sku
  | { eventBridgePerEvent: ($root|paidTier($sku)) }
  ' "$RAW_DIR/AWSEvents.json")

  stepfunctions_out=$(jq --arg loc "$LOCATION_NAME" "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="AWS Step Functions" and .attributes.group=="SFN-StateTransitions" and .attributes.location==$loc)][0]).sku as $sku
  | { stepFunctionsPerStateTransition: ($root|paidTier($sku)) }
  ' "$RAW_DIR/AmazonStates.json")

  cloudwatch_out=$(jq --arg loc "$LOCATION_NAME" "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="Data Payload" and .attributes.group=="Ingested Logs" and .attributes.operation=="PutLogEvents" and .attributes.location==$loc)][0]).sku as $ingestSku
  | ([$root.products[] | select(.productFamily=="Storage Snapshot" and .attributes.group=="Amazon CloudWatch Standard Storage pricing current" and .attributes.location==$loc)][0]).sku as $storageSku
  | ([$root.products[] | select(.productFamily=="Alarm" and .attributes.group=="Alarm" and (.attributes.usagetype | test("CW:AlarmMonitorUsage$")) and .attributes.location==$loc)][0]).sku as $alarmSku
  | {
    cloudWatchLogsIngestGb: ($root|paidTier($ingestSku)),
    cloudWatchLogsStorageGbMonth: ($root|paidTier($storageSku)),
    cloudWatchAlarmMonth: ($root|paidTier($alarmSku))
  }
  ' "$RAW_DIR/AmazonCloudWatch.json")

  kinesis_out=$(jq --arg loc "$LOCATION_NAME" "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="Kinesis Streams" and .attributes.group=="Provisioned shard hour" and .attributes.operation=="shardHourStorage" and .attributes.location==$loc)][0]).sku as $shardSku
  | ([$root.products[] | select(.productFamily=="Kinesis Streams" and .attributes.group=="Payload Units" and .attributes.operation=="PutRequest" and .attributes.location==$loc)][0]).sku as $putSku
  | {
    kinesisShardHour: ($root|paidTier($shardSku)),
    kinesisPutPayloadUnit: ($root|paidTier($putSku))
  }
  ' "$RAW_DIR/AmazonKinesis.json")

  waf_out=$(jq --arg loc "$LOCATION_NAME" "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="Web Application Firewall" and .attributes.group=="Web ACL" and (.attributes.usagetype | test("WebACLV2$")) and .attributes.location==$loc)][0]).sku as $aclSku
  | ([$root.products[] | select(.productFamily=="Web Application Firewall" and .attributes.group=="Rule" and (.attributes.usagetype | test("RuleV2$")) and .attributes.location==$loc)][0]).sku as $ruleSku
  | ([$root.products[] | select(.productFamily=="Web Application Firewall" and .attributes.group=="Request" and (.attributes.usagetype | test("RequestV2-Tier1$")) and .attributes.location==$loc)][0]).sku as $requestSku
  | {
    wafWebAclMonth: ($root|paidTier($aclSku)),
    wafRuleMonth: ($root|paidTier($ruleSku)),
    wafPerRequest: ($root|paidTier($requestSku))
  }
  ' "$RAW_DIR/awswaf.json")

  ses_out=$(jq --arg loc "$LOCATION_NAME" "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="Sending Email" and (.attributes.usagetype | test("(^|-)Recipients$")) and .attributes.operation=="Send" and .attributes.location==$loc)][0]).sku as $recipientSku
  | ([$root.products[] | select(.productFamily=="Sending Attachments" and (.attributes.usagetype | test("AttachmentsSize-Bytes$")) and .attributes.operation=="Send" and .attributes.location==$loc)][0]).sku as $attachmentSku
  | {
    sesPerRecipient: ($root|paidTier($recipientSku)),
    sesAttachmentGb: ($root|paidTier($attachmentSku))
  }
  ' "$RAW_DIR/AmazonSES.json")

  eks_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | ([$root.products[] | select(.attributes.operation=="CreateOperation" and (.attributes.usagetype | test("AmazonEKS-Hours:perCluster$")))][0]).sku as $standardSku
  | ([$root.products[] | select(.attributes.operation=="ExtendedSupport" and (.attributes.usagetype | test("AmazonEKS-Hours:extendedSupport$")))][0]).sku as $extendedSku
  | ([$root.products[] | select(.attributes.operation=="ProvisionedControlPlaneUsage" and .attributes.tiertype=="HAProvisionedXL")][0]).sku as $xlSku
  | ([$root.products[] | select(.attributes.operation=="ProvisionedControlPlaneUsage" and .attributes.tiertype=="HAProvisioned2XL")][0]).sku as $xl2Sku
  | ([$root.products[] | select(.attributes.operation=="ProvisionedControlPlaneUsage" and .attributes.tiertype=="HAProvisioned4XL")][0]).sku as $xl4Sku
  | ([$root.products[] | select(.attributes.operation=="ProvisionedControlPlaneUsage" and .attributes.tiertype=="HAProvisioned8XL")][0]).sku as $xl8Sku
  | { eksClusterHour: {
      standard: ($root|price($standardSku)),
      extended: ($root|price($extendedSku)),
      provisioned_xl: ($root|price($xlSku)),
      provisioned_2xl: ($root|price($xl2Sku)),
      provisioned_4xl: ($root|price($xl4Sku)),
      provisioned_8xl: ($root|price($xl8Sku))
    } }
  ' "$RAW_DIR/AmazonEKS.json")

  ecr_out=$(jq "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="EC2 Container Registry" and (.attributes.usagetype | test("(^|-)TimedStorage-ByteHrs$")))][0]).sku as $standardSku
  | ([$root.products[] | select(.productFamily=="EC2 Container Registry" and (.attributes.usagetype | test("TimedStorage-Archive-ByteHrs$")))][0]).sku as $archiveSku
  | { ecrStorageGbMonth: {
      standard: ($root|paidTier($standardSku)),
      archive: ($root|paidTier($archiveSku))
    } }
  ' "$RAW_DIR/AmazonECR.json")

  cloudtrail_out=$(jq "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="Management Tools - AWS CloudTrail Paid Events Recorded" or (.attributes.usagetype | test("PaidEventsRecorded$")))][0]).sku as $paidSku
  | ([$root.products[] | select(.attributes.usagetype | test("DataEventsRecorded$"))][0]).sku as $dataSku
  | ([$root.products[] | select((.attributes.usagetype | test("QueryScanned-Bytes$")) and (.attributes.usagetype | test("FreeTrial") | not))][0]).sku as $querySku
  | {
    cloudTrailPaidEvent: ($root|paidTier($paidSku)),
    cloudTrailDataEvent: ($root|paidTier($dataSku)),
    cloudTrailQueryGb: ($root|paidTier($querySku))
  }
  ' "$RAW_DIR/AWSCloudTrail.json")

  glue_out=$(jq "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.attributes.operation=="Jobrun" and (.attributes.usagetype | test("ETL-DPU-Hour$")))][0]).sku as $standardSku
  | ([$root.products[] | select(.attributes.operation=="Jobrun" and (.attributes.usagetype | test("ETL-DPU-Hour-Gen2$")))][0]).sku as $glue6Sku
  | ([$root.products[] | select(.attributes.operation=="FlexJobrun" and (.attributes.usagetype | test("ETL-Flex-DPU-Hour$")))][0]).sku as $flexSku
  | ([$root.products[] | select(.attributes.operation=="FlexJobrun" and (.attributes.usagetype | test("ETL-Flex-DPU-Hour-Gen2$")))][0]).sku as $flexGlue6Sku
  | ([$root.products[] | select(.attributes.operation=="CrawlerRun" and ((.attributes.usagetype | test("Crawler-DPU-Hour$")) or (.attributes.usagetype | test("Crawler-DPU-Hour-Gen2$"))))][0]).sku as $crawlerSku
  | {
    glueDpuHour: {
      standard: ($root|paidTier($standardSku)),
      glue_6: ($root|paidTier($glue6Sku)),
      flex: ($root|paidTier($flexSku)),
      flex_glue_6: ($root|paidTier($flexGlue6Sku))
    },
    glueCrawlerDpuHour: ($root|paidTier($crawlerSku))
  }
  ' "$RAW_DIR/AWSGlue.json")

  athena_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="Athena Queries" and (.attributes.usagetype | test("DataScannedInTB$")))][0]).sku as $sku
  | { athenaScannedTb: ($root|price($sku)) }
  ' "$RAW_DIR/AmazonAthena.json")

  redshift_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | (reduce ($root.products[] | select(.productFamily=="Compute Instance" and (.attributes.instanceType as $t | ["ra3.large","ra3.xlplus","dc2.large"] | index($t)))) as $p
      ({}; . + {($p.attributes.instanceType): ($root|price($p.sku))})) as $nodePrices
  | ([$root.products[] | select(.productFamily=="Redshift Managed Storage" and (.attributes.usagetype | test("RMS:ra3.large$")))][0]).sku as $storageSku
  | {
    redshiftRa3LargeHour: $nodePrices,
    redshiftStorageGbMonth: ($root|price($storageSku))
  }
  ' "$RAW_DIR/AmazonRedshift.json")

  msk_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | (reduce ($root.products[] | select(.productFamily=="Managed Streaming for Apache Kafka (MSK)" and .attributes.operation=="RunBroker" and (.attributes.usagetype as $u | ["Kafka.t3.small","Kafka.m5.large","Kafka.m7g.large"] | any(. as $suffix | $u | endswith($suffix))))) as $p
      ({}; . + {($p.attributes.usagetype | split("Kafka.")[1]): ($root|price($p.sku))})) as $brokerPrices
  | ([$root.products[] | select(.productFamily=="Managed Streaming for Apache Kafka (MSK)" and (.attributes.usagetype | test("Kafka.Storage.GP2$")) and .attributes.operation=="RunVolume")][0]).sku as $storageSku
  | {
    mskBrokerHour: $brokerPrices,
    mskStorageGbMonth: ($root|price($storageSku))
  }
  ' "$RAW_DIR/AmazonMSK.json")

  appsync_out=$(jq "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="API Calls" and (.attributes.usagetype | test("GraphQLInvocation$")))][0]).sku as $graphqlSku
  | ([$root.products[] | select(.productFamily=="RealTime" and (.attributes.usagetype | test("ConnectionDuration$")))][0]).sku as $minuteSku
  | {
    appSyncGraphqlRequest: ($root|paidTier($graphqlSku)),
    appSyncRealtimeMinute: ($root|paidTier($minuteSku))
  }
  ' "$RAW_DIR/AWSAppSync.json")

  cognito_out=$(jq "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="Amazon Cognito - Lite" and (.attributes.usagetype | test("CognitoLiteMAU$")))][0]).sku as $liteSku
  | ([$root.products[] | select(.productFamily=="Amazon Cognito - Essentials" and (.attributes.usagetype | test("CognitoEssentialsMAU$")))][0]).sku as $essentialsSku
  | ([$root.products[] | select(.productFamily=="Amazon Cognito - Plus" and (.attributes.usagetype | test("CognitoPlusMAU$")))][0]).sku as $plusSku
  | { cognitoMau: {
      lite: ($root|paidTier($liteSku)),
      essentials: ($root|paidTier($essentialsSku)),
      plus: ($root|paidTier($plusSku))
    } }
  ' "$RAW_DIR/AmazonCognito.json")

  bedrock_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | ([$root.products[] | select(.attributes.usagetype | test("NovaMicro-input-tokens$"))][0]).sku as $microInputSku
  | ([$root.products[] | select(.attributes.usagetype | test("NovaMicro-output-tokens$"))][0]).sku as $microOutputSku
  | ([$root.products[] | select(.attributes.usagetype | test("NovaLite-input-tokens$"))][0]).sku as $liteInputSku
  | ([$root.products[] | select(.attributes.usagetype | test("NovaLite-output-tokens$"))][0]).sku as $liteOutputSku
  | ([$root.products[] | select(.attributes.usagetype | test("NovaPro-input-tokens$"))][0]).sku as $proInputSku
  | ([$root.products[] | select(.attributes.usagetype | test("NovaPro-output-tokens$"))][0]).sku as $proOutputSku
  | {
    bedrockNovaLiteInput1kTokens: {
      nova_micro: ($root|price($microInputSku)),
      nova_lite: ($root|price($liteInputSku)),
      nova_pro: ($root|price($proInputSku))
    },
    bedrockNovaLiteOutput1kTokens: {
      nova_micro: ($root|price($microOutputSku)),
      nova_lite: ($root|price($liteOutputSku)),
      nova_pro: ($root|price($proOutputSku))
    }
  }
  ' "$RAW_DIR/AmazonBedrock.json")

  sagemaker_out=$(jq "$JQ_PRICE_FN"'
  . as $root
  | (reduce ($root.products[] | select(.productFamily=="ML Instance" and .attributes.operation=="RunInstance" and (.attributes.instanceType as $t | ["ml.m5.large-Hosting","ml.c6g.large-Hosting","ml.g4dn.xlarge-Hosting"] | index($t)))) as $p
      ({}; . + {($p.attributes.instanceType | sub("-Hosting$"; "")): ($root|price($p.sku))}))
      as $prices
  | { sageMakerMlM5LargeHour: $prices }
  ' "$RAW_DIR/AmazonSageMaker.json")

  ssm_out=$(jq "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.attributes.usagetype | test("PS-Advanced-Param-Tier1$"))][0]).sku as $parameterSku
  | ([$root.products[] | select(.productFamily=="API Request" and (.attributes.usagetype | test("PS-Param-Processed-Tier1$")))][0]).sku as $apiSku
  | {
    ssmAdvancedParameterMonth: {
      standard: 0,
      advanced: ($root|paidTier($parameterSku))
    },
    ssmParameterApiRequest: ($root|paidTier($apiSku))
  }
  ' "$RAW_DIR/AWSSystemsManager.json")

  kms_out=$(jq "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="Encryption Key" and (.attributes.usagetype | test("KMS-Keys$")))][0]).sku as $keySku
  | ([$root.products[] | select(.productFamily=="API Request" and (.attributes.usagetype | test("KMS-Requests$")))][0]).sku as $symmetricSku
  | ([$root.products[] | select(.attributes.group=="awskms-Requests-Asymmetric")][0]).sku as $asymmetricSku
  | ([$root.products[] | select(.attributes.group=="aws-kms-Requests-Asymmetric-RSA_2048")][0]).sku as $rsa2048Sku
  | {
    kmsKeyMonth: ($root|paidTier($keySku)),
    kmsRequest: {
      symmetric: ($root|paidTier($symmetricSku)),
      asymmetric: ($root|paidTier($asymmetricSku)),
      rsa_2048: ($root|paidTier($rsa2048Sku))
    }
  }
  ' "$RAW_DIR/awskms.json")

  cloudhsm_out=$(jq "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="Dedicated-Host" and (.attributes.usagetype | test("CloudHSMv2Usage$")) and .attributes.operation=="Hourly")][0]).sku as $hsm1Sku
  | ([$root.products[] | select(.productFamily=="Dedicated-Host" and (.attributes.usagetype | test("CloudHSMv2Usage-hsm2m.m$")) and .attributes.operation=="Hourly")][0]).sku as $hsm2mSku
  | { cloudHsmHour: {
      hsm1: ($root|paidTier($hsm1Sku)),
      hsm2m: ($root|paidTier($hsm2mSku))
    } }
  ' "$RAW_DIR/CloudHSM.json")

  backup_out=$(jq "$JQ_PAID_TIER_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="AWS Backup Storage" and (.attributes.usagetype | test("WarmStorage-ByteHrs-EBS-LAGV$")) and .attributes.operation=="Storage")][0]).sku as $ebsSku
  | ([$root.products[] | select(.productFamily=="AWS Backup Storage" and (.attributes.usagetype | test("WarmStorage-ByteHrs-EFS-LAGV$")) and .attributes.operation=="Storage")][0]).sku as $efsSku
  | ([$root.products[] | select(.productFamily=="AWS Backup Storage" and (.attributes.usagetype | test("WarmStorage-ByteHrs-DynamoDB-LAGV$")) and .attributes.operation=="Storage")][0]).sku as $ddbSku
  | { backupWarmStorageGbMonth: {
      ebs_lagv: ($root|paidTier($ebsSku)),
      efs_lagv: ($root|paidTier($efsSku)),
      dynamodb_lagv: ($root|paidTier($ddbSku))
    } }
  ' "$RAW_DIR/AWSBackup.json")

  local fetched_at
  fetched_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

  jq -n \
    --arg region "$REGION" \
    --arg fetchedAt "$fetched_at" \
    --argjson ec2 "$ec2_out" \
    --argjson rds "$rds_out" \
    --argjson redis "$redis_out" \
    --argjson ecs "$ecs_out" \
    --argjson alb "$alb_out" \
    --argjson s3 "$s3_out" \
    --argjson cloudfront "$cloudfront_out" \
    --argjson lambda "$lambda_out" \
    --argjson dynamodb "$dynamodb_out" \
    --argjson datatransfer "$datatransfer_out" \
    --argjson vpc "$vpc_out" \
    --argjson apigateway "$apigateway_out" \
    --argjson sqs "$sqs_out" \
    --argjson sns "$sns_out" \
    --argjson efs "$efs_out" \
    --argjson opensearch "$opensearch_out" \
    --argjson secretsmanager "$secretsmanager_out" \
    --argjson eventbridge "$eventbridge_out" \
    --argjson stepfunctions "$stepfunctions_out" \
    --argjson cloudwatch "$cloudwatch_out" \
    --argjson kinesis "$kinesis_out" \
    --argjson waf "$waf_out" \
    --argjson ses "$ses_out" \
    --argjson eks "$eks_out" \
    --argjson ecr "$ecr_out" \
    --argjson cloudtrail "$cloudtrail_out" \
    --argjson glue "$glue_out" \
    --argjson athena "$athena_out" \
    --argjson redshift "$redshift_out" \
    --argjson msk "$msk_out" \
    --argjson appsync "$appsync_out" \
    --argjson cognito "$cognito_out" \
    --argjson bedrock "$bedrock_out" \
    --argjson sagemaker "$sagemaker_out" \
    --argjson ssm "$ssm_out" \
    --argjson kms "$kms_out" \
    --argjson cloudhsm "$cloudhsm_out" \
    --argjson backup "$backup_out" \
    --argjson route53 "$ROUTE53_OUT" \
    '{
      meta: {
        region: $region,
        source: "AWS Price List Bulk API (pricing.us-east-1.amazonaws.com)",
        fetchedAt: $fetchedAt
      },
      hoursPerMonth: 730,
      usdToKrw: 1380
    }
    + $ec2 + $rds + $redis + $ecs + $alb + $s3 + $cloudfront + $lambda + $dynamodb + $datatransfer + $vpc
    + $apigateway + $sqs + $sns + $efs + $opensearch + $secretsmanager
    + $eventbridge + $stepfunctions + $cloudwatch + $kinesis + $waf + $ses
    + $eks + $ecr + $cloudtrail + $glue + $athena + $redshift + $msk + $appsync
    + $cognito + $bedrock + $sagemaker + $ssm + $kms + $cloudhsm + $backup
    + $route53' \
    > "$RESOURCES_DIR/$REGION-pricing.json"

  echo "[$REGION] wrote $RESOURCES_DIR/$REGION-pricing.json" >&2
}

# Route53 is a genuinely global service — hosted zone and query pricing don't vary by which AWS
# region the app is deployed in, so this is fetched once (not per-region, unlike everything
# above) and the same values are merged into every region's snapshot.
fetch_route53() {
  echo "[route53] downloading (global, region-independent) ..." >&2
  local tmp
  tmp="$(mktemp)"
  curl -sS --max-time 300 -o "$tmp" "$BASE_URL/AmazonRoute53/current/index.json"

  ROUTE53_OUT=$(jq "$JQ_PRICE_FN"'
  . as $root
  | ([$root.products[] | select(.productFamily=="DNS Zone" and .attributes.usagetype=="HostedZone")][0]).sku as $zoneSku
  | ([$root.products[] | select(.productFamily=="DNS Query" and (.attributes.usagetype | test("APN2-DNS-Queries$")))][0]).sku as $querySku
  | (
      $root.terms.OnDemand[$zoneSku] | to_entries[0].value.priceDimensions
      | to_entries | map(.value) | min_by(.beginRange|tonumber) | .pricePerUnit.USD | tonumber
    ) as $zonePrice
  | {
    route53HostedZoneMonth: $zonePrice,
    route53PerQuery: ($root|price($querySku))
  }' "$tmp")

  rm -f "$tmp"
}

mkdir -p "$RESOURCES_DIR"
fetch_route53
for region in $TARGET_REGIONS; do
  fetch_region "$region"
done

echo "Done. Regions fetched: $TARGET_REGIONS" >&2
