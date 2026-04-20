# SSL 证书目录

此目录用于存放 SSL 证书文件。

## 所需文件

- `cert.pem` - SSL 证书文件
- `key.pem` - SSL 私钥文件

## 生成自签名证书（仅用于开发测试）

```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout key.pem \
  -out cert.pem \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=EasyOrange/OU=IT/CN=localhost"
```

## 生产环境

在生产环境中，请使用受信任的证书颁发机构（CA）签发的证书，例如：
- Let's Encrypt（免费）
- 阿里云 SSL 证书
- 腾讯云 SSL 证书
