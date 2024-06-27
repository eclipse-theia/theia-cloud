# Installing the self signed CA for local testing

When testing locally you usually have to accept the self signed certificate in your browser. When working with wildcard certificates however, as used by e.g. the default webview hostnames, you usually will get an error with no way to accept the certificate.

You may import the self-signed CA in your browser however. You may export the secret to an importable file like this:

```bash
kubectl get secret theia-cloud-ca-key-pair -n cert-manager -o jsonpath='{.data.tls\.crt}' | base64 --decode > ca.crt
```

Then, e.g. in Chrome, got to <chrome://settings/certificates> and Add Authority.
