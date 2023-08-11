# Azure Kubernetes Service Deployment

## Setup

 - create ```terraform.tfvars``` file containing the following:

```
subscription_id = "yourSubscriptionId"
location = "yourAzureLocation"
cert_manager_issuer_email = "yourCertEmail"
resource_group_name = "resourceGroupName"       # mustn't exist yet, will be managed by terraform
keycloak_admin_password = "admin"               # set to your liking
postgres_postgres_password = "admin"            # set to your liking
postgres_password = "admin"                     # set to your liking
```

 - for using kubectl, after cluster creation set kubernetes context (assuming you have install azure-cli):

```
az login
az aks get-credentials --resource-group "resourceGroupName" --name "aks-theia-cloud-getting-started"
```

