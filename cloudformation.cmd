aws cloudformation deploy \
  --template-file backend.yml \
  --stack-name ecommerce-BE-stack \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    ConnectionArn="arn:aws:codeconnections:us-east-1:340233612461:connection/4dce5a41-0e7f-49ec-8e1c-530878e519bb" \
    FullRepositoryId="Sushilkarki77/cs516-final-backend" \
    GitHubLocation="https://github.com/Sushilkarki77/cs516-final-backend" \
    MediaBucket="e-market-media-bucket-sk-dev"