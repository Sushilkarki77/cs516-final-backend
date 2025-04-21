aws cloudformation deploy \
  --template-file codepipeline.yml \
  --stack-name ecommerce-BE-pipeline-stack \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    ConnectionArn="your-arn" \
    FullRepositoryId="Sushilkarki77/cs516-final-backend" \
    GitHubLocation="https://github.com/Sushilkarki77/cs516-final-backend" 

    