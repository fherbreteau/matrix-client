# Security Policy

## 🔒 Reporting Security Vulnerabilities

The security of the matrix-client project is a top priority. If you discover any security vulnerabilities, please follow this responsible disclosure process.

## 📬 How to Report

**Please do NOT report security vulnerabilities through public GitHub issues, discussions, or pull requests.**

Instead, report them privately by:

1. **Email**: fherbreteau@gmail.com
2. **Matrix**: @fherbreteau:matrix.org (encrypted message preferred)

## 🛡️ Supported Versions

Security updates are provided for the following versions:

| Version | Supported | Security Updates |
|---------|--------------------|------------------|
| 0.1.x   | ✅ Actively Supported | ✅ Yes |
| older   | ❌ Not Supported    | ❌ No |

## 🕒 Response Process

1. **Acknowledgment**: You will receive an acknowledgment within 24 hours
2. **Assessment**: Our security team will assess the vulnerability within 72 hours
3. **Patch Development**: Critical vulnerabilities will be patched within 7 days
4. **Disclosure**: Coordinated disclosure with credit to reporter

## ⚠️ Security Best Practices

### For Users

- Always use the latest version
- Verify artifacts obtained from Maven Central
- Use HTTPS for all homeserver URLs
- Keep your Java runtime updated
- Never hardcode access tokens in your code; use environment variables or a secrets manager

### For Developers

- Follow secure coding practices
- Validate all inputs and outputs
- Implement proper error handling
- Never log credentials or access tokens
- Keep the dependency policy: production code must use the JDK only

## 🛡️ Repository Security Settings

The following repository-level protections are recommended and should stay enabled:

| Capability | Status |
|------------|--------|
| Secret scanning | ✅ Enabled |
| Secret scanning push protection | ✅ Enabled |
| Dependabot security updates | ✅ Enabled |
| CI security analysis | ✅ Enabled (`.github/workflows/`) |

**Push protection** blocks pushes containing recognized secret patterns
(GitHub tokens, cloud provider keys, etc.) on every branch at push time,
so a leaked credential cannot sit unnoticed in a feature branch or fork.
Overriding a blocked push is possible with a stated reason and should be
reserved for false positives and test fixtures.

## 🔐 Transport Security

The library talks to homeservers over HTTP(S) through `java.net.http.HttpClient`:

- Always configure clients with `https://` homeserver URLs; plain `http://`
  should only be used against local development instances.
- Access tokens are sent as `Authorization` headers — the transport never
  caches or exposes them beyond the request in flight.

## 📋 Security Checklist

- [x] Secure coding practices
- [x] Regular dependency updates
- [x] Secure build process
- [x] Responsible disclosure policy
- [x] Security documentation

## 🤝 Responsible Disclosure

We follow responsible disclosure principles:

1. Private reporting of vulnerabilities
2. Coordinated patch release
3. Public disclosure after patch
4. Credit to security researchers

## 📄 Legal

By reporting security vulnerabilities, you agree to:

- Keep the vulnerability confidential until patch release
- Allow us reasonable time to develop and test fixes
- Not exploit the vulnerability for malicious purposes
- Follow our responsible disclosure process

## 🙏 Acknowledgments

We appreciate the security community's efforts in making our software more secure. Security researchers who responsibly disclose vulnerabilities will be acknowledged in our release notes (unless anonymity is requested).

---

**Last Updated**: 2026-09-20
**Contact**: fherbreteau@gmail.com
