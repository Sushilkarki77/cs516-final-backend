aws cloudformation deploy \
  --template-file backend.yml \
  --stack-name e-market-BE-pipeline-stack \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    ConnectionArn="arn:aws:codeconnections:us-east-1:767828741560:connection/9e3140a7-3b69-4bed-9631-9a28c1358ff1" \
    FullRepositoryId="thanhnq1/cs516-final-backend" \
    GitHubLocation="https://github.com/thanhnq1/cs516-final-backend"

