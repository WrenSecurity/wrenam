# Sample X509 files

The following commands were used to generate files in this folder:

```sh
# Generate CA certificate
openssl genrsa -out sample-ca/ca.key.pem 2048
openssl req -x509 -new -key sample-ca/ca.key.pem -out sample-ca/ca.cert.pem -days 36500 -subj "/CN=Sample CA"

# Generate user certificate
openssl genrsa -out user.key.pem 2048
openssl req -new -key user.key.pem -out user.csr.pem -subj '/CN=john.smith' -addext "subjectAltName=otherName:1.3.6.1.4.1.311.20.2.3;UTF8:john.smith@example.com"
openssl ca -batch -config sample-ca/openssl.cnf -notext -in user.csr.pem -out user.cert.pem

# Generate revoked certificate
openssl genrsa -out revoked.key.pem 2048
openssl req -new -key revoked.key.pem -out revoked.csr.pem -subj '/CN=jane.smith' -addext "subjectAltName=otherName:1.3.6.1.4.1.311.20.2.3;UTF8:jane.smith@example.com"
openssl ca -batch -config sample-ca/openssl.cnf -notext -in revoked.csr.pem -out revoked.cert.pem 
openssl ca -config sample-ca/openssl.cnf -revoke revoked.cert.pem

# Generate revocation list
openssl ca -config sample-ca/openssl.cnf -gencrl -out sample.crl.pem
```

Removing generated content:

```sh
# Remove CA data
rm -f sample-ca/newcerts/*
echo 0 > sample-ca/crlnumber
echo -n "" > sample-ca/index.txt

# Remove user certificate
rm user.key.pem user.key.csr user.cert.pem

# Remove revoked certificate
rm revoked.key.pem revoked.key.csr revoked.cert.pem

# Remove revocation list
rm sample.crl.pem

# Remove CA certificate
rm sample-ca/ca.cert.pem sample-ca/ca.key.pem
```
