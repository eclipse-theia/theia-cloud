# Keycloak

## Installation on

### ...Minikube

Follow the steps in https://www.keycloak.org/getting-started/getting-started-kube

As time of writing, those are:

```bash
# create a keycloak deployment
kubectl create -f https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/latest/kubernetes-examples/keycloak.yaml

# Create an ingress for keycloak on Minikube
wget -q -O - https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/latest/kubernetes-examples/keycloak-ingress.yaml | \
sed "s/KEYCLOAK_HOST/keycloak.$(minikube ip).nip.io/" | \
kubectl create -f -

# Find the URL for keycloak:
KEYCLOAK_URL=https://keycloak.$(minikube ip).nip.io &&
echo "" &&
echo "Keycloak:                 $KEYCLOAK_URL" &&
echo "Keycloak Admin Console:   $KEYCLOAK_URL/admin" &&
echo "Keycloak Account Console: $KEYCLOAK_URL/realms/myrealm/account" &&
echo ""
```

## Administration

Go to the keycloak admin console and log in with admin - admin credentials.\
In the top left, hover over `Master` and selected `Add realm`.\
Import `doc/docs/platforms/realm-export.json` via `Select file` and click `Create`.\
Go to `Manage -> Users` in the left panel and select `Add user`.
Add 

* Username, e.g. `foo`
* Email, e.g. `foo@theia-cloud.io`
* Email verified to On

Select `Save` and go to `Credentials` Tab.
Set

* Password
* Password Confirmation
* Temporary to OFF

Select `Set Password`

Add more users if you want to.
