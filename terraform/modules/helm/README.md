# Helm Installation

This module may be used to install Theia Cloud and all dependencies in a cluster via Helm.

We expect that user are already familiar with Helm and that `kubectl` points to the cluster in which Theia Cloud will be installed.

The module will install the Cert Manager, the Nginx Ingress Controller (optional when installed already), the Theia Cloud Base Chart (cluster wide resources), and Keycloak.\
After Keycloak was installed we will patch the Nginx Ingress Controller to use the certificate generated during the Keycloak installation as the default certificate.\
Finally we will install Theia Cloud.
