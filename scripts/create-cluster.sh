#!/bin/bash
# This script creates a new EMR cluster with sensible defaults.

# Check whether the script is sourced, i.e., run with `source create-cluster.sh` or simply executed.
is_sourced() {
    [[ "${BASH_SOURCE[0]-}" != "$0" ]]
}

if ! command -v aws >/dev/null 2>&1; then
  echo "❌ Error: aws CLI is not installed or not in PATH. Install it first."
  exit 1
fi
if ! is_sourced; then
    echo "⚠️  To correctly set up environment variables this script must be sourced, not executed."
    echo "    Please run: source $0"
    echo "    Continuing but environment variables will NOT be set in this shell."
fi
if [ "$#" -ne 2 ]; then
    echo "❌ Error: Invalid number of arguments."
    echo
    echo "   Usage: $0 <profile_name> <key_pair_name>"
    echo
    echo "   Example:"
    echo "       source ~/.aws/scripts/create-cluster.sh big-data-labs big-data"
    exit 1
fi

PROFILE_NAME=$1
KEY_PAIR_NAME=$2
REGION="us-east-1"
RELEASE_LABEL="emr-7.3.0"
NAME="Big Data Cluster"

# Cleanup function to be executed on script exit or error.
cleanup() {
    local exit_code=$?  # captures the exit code of the failed command or script
    echo "🧹 Cleaning up..."
    # Only terminate cluster if the script failed (exit_code != 0)
    if [[ $exit_code -ne 0 && -n "${CLUSTER_ID:-}" ]]; then
        echo "⏳ Terminating EMR cluster with ID: $CLUSTER_ID"
        aws emr terminate-clusters --cluster-ids "$CLUSTER_ID" --profile "$PROFILE_NAME"
        echo "✅ Cluster wil be terminated in few moments."
    fi
    if is_sourced; then
        trap - EXIT INT TERM  # Clean up traps
        return $exit_code
    else
        exit $exit_code
    fi
}
trap cleanup EXIT INT TERM # Trap any exit signals (EXIT, INT, TERM) to run the cleanup function.

echo "⏳ Creating EMR cluster with profile '$PROFILE_NAME'..."
CLUSTER_ID=$(aws emr create-cluster \
    --name "$NAME" \
    --release-label "$RELEASE_LABEL" \
    --applications Name=Hadoop Name=Spark \
    --instance-groups InstanceGroupType=MASTER,InstanceCount=1,InstanceType=m4.large InstanceGroupType=CORE,InstanceCount=6,InstanceType=m4.large \
    --service-role EMR_DefaultRole \
    --ec2-attributes InstanceProfile=EMR_EC2_DefaultRole,KeyName="$KEY_PAIR_NAME" \
    --region "$REGION" \
    --profile "$PROFILE_NAME" \
    --output text \
    --query 'ClusterId')
echo "✅ EMR cluster created successfully!"
echo "   Cluster ID: $CLUSTER_ID"

echo "⏳ Waiting for the cluster to be in 'WAITING' state. This may take a few minutes..."
aws emr wait cluster-running --cluster-id "$CLUSTER_ID" --profile "$PROFILE_NAME"
echo "✅ Cluster is now in 'WAITING' state."

echo "⏳ Retrieving Master Node Public DNS..."
MASTER_NODE_DNS=$(aws emr describe-cluster \
    --cluster-id "$CLUSTER_ID" \
    --profile "$PROFILE_NAME" \
    --query "Cluster.MasterPublicDnsName" \
    --output text)
echo "✅ Master Node Public DNS: $MASTER_NODE_DNS"

if is_sourced; then
  echo "⏳ Exporting Master Node Public DNS to environment variable..."
  export MASTER_NODE_DNS
  echo "✅ Master Node Public DNS set to 'MASTER_NODE_DNS'."
else
    echo "⚠️  Skipping export of 'MASTER_NODE_DNS' because the script was not sourced."
fi

trap - EXIT INT TERM # Disable cleanup trap if script completes successfully
