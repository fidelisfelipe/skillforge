package com.skillforge.hub.onboarding;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Service
@ConditionalOnProperty("guild.mtls.enabled")
public class MtlsCaService {

    private static final Logger log = LoggerFactory.getLogger(MtlsCaService.class);
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    static {
        if (Security.getProvider(BC) == null) Security.addProvider(new BouncyCastleProvider());
    }

    @Value("${guild.mtls.certs-dir:./certs}")
    private String certsDir;

    private KeyPair caKeyPair;
    private X509Certificate caCert;

    @PostConstruct
    void init() throws Exception {
        Path dir = Paths.get(certsDir);
        Files.createDirectories(dir);
        Path keyFile  = dir.resolve("ca.key.pem");
        Path certFile = dir.resolve("ca.crt.pem");

        if (Files.exists(keyFile) && Files.exists(certFile)) {
            loadCa(keyFile, certFile);
            log.info("🔐 mTLS CA carregada de {}", dir.toAbsolutePath());
        } else {
            generateCa(keyFile, certFile);
            log.info("🔐 mTLS CA gerada em {} — copie ca.crt.pem para o ngrok Traffic Policy", dir.toAbsolutePath());
        }
    }

    private void loadCa(Path keyFile, Path certFile) throws Exception {
        byte[] keyBytes = decodePem(Files.readString(keyFile));
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        byte[] certBytes = decodePem(Files.readString(certFile));
        caCert = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(certBytes));

        caKeyPair = new KeyPair(caCert.getPublicKey(), privateKey);
    }

    private void generateCa(Path keyFile, Path certFile) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BC);
        kpg.initialize(2048, new SecureRandom());
        caKeyPair = kpg.generateKeyPair();

        X500Name name = new X500Name("CN=SkillForge Hub CA, O=SkillForge, C=PT");
        Date notBefore = new Date();
        Date notAfter  = new Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000);

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name, BigInteger.ONE, notBefore, notAfter, name, caKeyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BC).build(caKeyPair.getPrivate());
        caCert = new JcaX509CertificateConverter().setProvider(BC)
                .getCertificate(builder.build(signer));

        writePem(keyFile, "PRIVATE KEY", caKeyPair.getPrivate().getEncoded(), true);
        writePem(certFile, "CERTIFICATE", caCert.getEncoded(), false);
    }

    public byte[] issuePkcs12(String githubLogin, String password) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BC);
        kpg.initialize(2048, new SecureRandom());
        KeyPair clientKp = kpg.generateKeyPair();

        X500Name issuer  = new X500Name("CN=SkillForge Hub CA, O=SkillForge, C=PT");
        X500Name subject = new X500Name("CN=" + githubLogin + ", O=SkillForge, C=PT");
        Date notBefore = new Date();
        Date notAfter  = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);
        BigInteger serial = new BigInteger(64, new SecureRandom());

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, clientKp.getPublic());
        builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        builder.addExtension(Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BC).build(caKeyPair.getPrivate());
        X509Certificate clientCert = new JcaX509CertificateConverter().setProvider(BC)
                .getCertificate(builder.build(signer));

        KeyStore ks = KeyStore.getInstance("PKCS12", BC);
        ks.load(null, null);
        ks.setKeyEntry(githubLogin, clientKp.getPrivate(), password.toCharArray(),
                new java.security.cert.Certificate[]{clientCert, caCert});

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ks.store(baos, password.toCharArray());
        return baos.toByteArray();
    }

    public String getCaCertPem() throws Exception {
        StringWriter sw = new StringWriter();
        try (PemWriter pw = new PemWriter(sw)) {
            pw.writeObject(new PemObject("CERTIFICATE", caCert.getEncoded()));
        }
        return sw.toString();
    }

    private void writePem(Path path, String type, byte[] data, boolean restrictRead) throws IOException {
        StringWriter sw = new StringWriter();
        try (PemWriter pw = new PemWriter(sw)) {
            pw.writeObject(new PemObject(type, data));
        }
        Files.writeString(path, sw.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        if (restrictRead) {
            path.toFile().setReadable(false, false);
            path.toFile().setReadable(true, true);
        }
    }

    private byte[] decodePem(String pem) {
        String b64 = pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(b64);
    }
}
