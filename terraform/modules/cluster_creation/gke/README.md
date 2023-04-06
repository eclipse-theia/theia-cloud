# GKE Cluster Creation

This module may be used to create a Cluster on Google Kubernetes Engine that may be used for running a Theia Cloud Getting Started application.

We expect that users are already familiar with GKE and have the GCLoud SDK installed.

The module will create a new cluster with an auto-scaling node pool.
The default values will have 1-2 e2-standard-2 nodes.

When the cluster is created we will use the GCloud SDK to connect `kubectl` to the cluster.
