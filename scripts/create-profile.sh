#!/bin/bash
# This script creates a new aws profile with the provided information.

is_sourced() {
    [[ "${BASH_SOURCE[0]}" != "${0}" ]]
}

if ! is_sourced; then
    echo "⚠️  To correctly set up environment variables this script must be sourced, not executed."
    echo "    Please run: source $0"
    echo "    Continuing but environment variables will NOT be set in this shell."
fi
if [ "$#" -ne 4 ]; then
    echo "❌ Error: Invalid number of arguments."
    echo
    echo "   Usage: source $0 <profile_name> <aws_access_key_id> <aws_secret_access_key> <aws_session_token>"
    exit 1
fi

PROFILE_NAME=$1
AWS_ACCESS_KEY_ID=$2
AWS_SECRET_ACCESS_KEY=$3
AWS_SESSION_TOKEN=$4
AWS_REGION="us-east-1"

echo "⏳ Creating AWS profile '$PROFILE_NAME'..."
aws configure set aws_access_key_id "$AWS_ACCESS_KEY_ID" --profile "$PROFILE_NAME"
aws configure set aws_secret_access_key "$AWS_SECRET_ACCESS_KEY" --profile "$PROFILE_NAME"
aws configure set aws_session_token "$AWS_SESSION_TOKEN" --profile "$PROFILE_NAME"
aws configure set region "$AWS_REGION" --profile "$PROFILE_NAME"
echo "✅ Profile '$PROFILE_NAME' created successfully!"

if is_sourced; then
  echo "⏳ Exporting profile to 'AWS_PROFILE' environment variable..."
  export AWS_PROFILE="$PROFILE_NAME"
  echo "✅ AWS profile set to 'AWS_PROFILE'."
else
    echo "⚠️  Skipping export of 'AWS_PROFILE' because the script was not sourced."
fi
