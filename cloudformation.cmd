aws cloudformation deploy \
  --template-file backend.yml \
  --stack-name ecommerce-BE-stack \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    ConnectionArn="your arn" \
    FullRepositoryId="Sushilkarki77/cs516-final-backend" \
    GitHubLocation="https://github.com/Sushilkarki77/cs516-final-backend" \
    MediaBucket="e-market-media-bucket-sk-dev"