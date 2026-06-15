# 1. Bouw de image en push naar Google Container Registry of Artifact Registry
gcloud builds submit --tag gcr.io/tennis-497614/demo

# 2. Deploy naar Cloud Run
gcloud run deploy demo --image gcr.io/tennis-497614/demo --platform managed --region europe-west1