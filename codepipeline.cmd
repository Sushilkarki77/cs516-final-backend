aws cloudformation deploy \
  --template-file codepipeline.yml \
  --stack-name ecommerce-pipeline-stack \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    ConnectionArn="arn:aws:codeconnections:us-east-1:340233612461:connection/d04bd876-9127-419e-bfa5-d9112b459d26" \
    FullRepositoryId="Sushilkarki77/cs516-final-backend" \
    GitHubLocation="https://github.com/Sushilkarki77/cs516-final-backend" 

    