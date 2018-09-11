/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.rub.nds.tlsscanner.report;

import de.rub.nds.tlsattacker.attacks.constants.DrownVulnerabilityType;
import de.rub.nds.tlsattacker.attacks.constants.EarlyCcsVulnerabilityType;
import static de.rub.nds.tlsattacker.attacks.constants.EarlyCcsVulnerabilityType.NOT_VULNERABLE;
import static de.rub.nds.tlsattacker.attacks.constants.EarlyCcsVulnerabilityType.VULN_EXPLOITABLE;
import static de.rub.nds.tlsattacker.attacks.constants.EarlyCcsVulnerabilityType.VULN_NOT_EXPLOITABLE;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.CompressionMethod;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.constants.TokenBindingKeyParameters;
import de.rub.nds.tlsattacker.core.constants.TokenBindingVersion;
import de.rub.nds.tlsscanner.constants.AnsiColors;
import de.rub.nds.tlsscanner.constants.CipherSuiteGrade;
import de.rub.nds.tlsscanner.probe.handshakeSimulation.SimulatedClient;
import de.rub.nds.tlsscanner.probe.certificate.CertificateReport;
import de.rub.nds.tlsscanner.report.result.VersionSuiteListPair;

public class SiteReportPrinter {

    SiteReport report;

    public SiteReportPrinter(SiteReport report) {
        this.report = report;
    }

    public String getFullReport() {
        StringBuilder builder = new StringBuilder();
        builder.append("Report for ");
        builder.append(report.getHost());
        builder.append("\n");
        if (report.getServerIsAlive() == false) {
            builder.append("Cannot reach the Server. Is it online?");
            return builder.toString();
        }
        if (report.getSupportsSslTls() == false) {
            builder.append("Server does not seem to support SSL / TLS");
            return builder.toString();
        }

        appendProtocolVersions(builder);
        appendCipherSuites(builder);
        appendExtensions(builder);
        appendCompressions(builder);
        appendIntolerances(builder);
        appendAttackVulnerabilities(builder);
        appendGcm(builder);
        appendRfc(builder);
        appendCertificate(builder);
        appendSession(builder);
        appendRenegotiation(builder);
        appendHandshakeSimulation(builder);
        return builder.toString();
    }
      
    private StringBuilder appendHandshakeSimulation(StringBuilder builder) {
        if (report.getSimulatedClientList()!= null) {
            appendHSOverview(builder);
            if (report.getDetailLevel()==2) {
                appendHSDetail2(builder);
            } else if (report.getDetailLevel()==3) {
                appendHSDetail2(builder);
                appendHSDetail3(builder);
            }
        }
        return builder;
    }
    
    private StringBuilder appendHSOverview(StringBuilder builder) {
        prettyAppendHeading(builder, "TLS Handshake Simulation - Overview");
        prettyAppend(builder, "Tested Clients", Integer.toString(report.getSimulatedClientList().size()));
        if (report.getHandshakeSuccessfulCounter() == 0) {
            prettyAppendRed(builder, getGreenString("Successful Handshakes", "%-32s"), Integer.toString(report.getHandshakeSuccessfulCounter()));
        } else {
            prettyAppendGreen(builder, getGreenString("Successful Handshakes", "%-32s"), Integer.toString(report.getHandshakeSuccessfulCounter()));
        }
        if (report.getHandshakeFailedCounter() == 0) {
            prettyAppendGreen(builder, getRedString("Failed Handshakes", "%-32s"), Integer.toString(report.getHandshakeFailedCounter()));
        } else {
            prettyAppendRed(builder, getRedString("Failed Handshakes", "%-32s"), Integer.toString(report.getHandshakeFailedCounter()));
        }
        if (report.getConnectionSecureCounter() == 0) {
            prettyAppendRed(builder, getGreenString("Secure Connections", "%-32s"), Integer.toString(report.getConnectionSecureCounter()));
        } else {
            prettyAppendGreen(builder, getGreenString("Secure Connections", "%-32s"), Integer.toString(report.getConnectionSecureCounter()));
        }
        if (report.getConnectionInsecureCounter() == 0) {
            prettyAppendGreen(builder, getRedString("Insecure Connections", "%-32s"), Integer.toString(report.getConnectionInsecureCounter()));
        } else {
            prettyAppendRed(builder, getRedString("Insecure Connections", "%-32s"), Integer.toString(report.getConnectionInsecureCounter()));
        }
        return builder;
    }
    
    private StringBuilder appendHSDetail2(StringBuilder builder) {
        prettyAppendHeading(builder, "TLS Handshake Simulation - Level of Detail: 2");
        prettyAppendHSDetail2Row(builder, "TLS Client", "TLS Version", "Ciphersuite", "Forward Secrecy", "Server Public Key");
        builder.append("\n");
        for (SimulatedClient simulatedClient : report.getSimulatedClientList()) {
            if (simulatedClient.getReceivedServerHelloDone()) {
                prettyAppendHSDetail2Row(builder, simulatedClient.getType() + ":" + simulatedClient.getVersion(),
                        simulatedClient.getConnectionSecure(), simulatedClient.getSelectedProtocolVersion(), 
                        simulatedClient.getSelectedCiphersuite(), simulatedClient.getForwardSecrecy(),
                        simulatedClient.getServerPublicKeyLength());
            } else {
                prettyAppendHSDetail2Row(builder, simulatedClient.getType() + ":" + simulatedClient.getVersion());
            }
        }
        return builder;
    }
    
    private String getCipherSuiteColour(CipherSuite suite, String format) {
        CipherSuiteGrade grade = CiphersuiteRater.getGrade(suite);
        switch (grade) {
            case GOOD:
                return getGreenString(suite.name(), format);
            case LOW:
                return getRedString(suite.name(), format);
            case MEDIUM:
                return getYellowString(suite.name(), format);
            case NONE:
                return getBlackString(suite.name(), format);
            default:
                return getBlackString(suite.name(), format);
        }
    }   
    
    private StringBuilder prettyAppendHSDetail2Row(StringBuilder builder, String value1) {
        builder.append((report.isNoColor() == false ? AnsiColors.ANSI_RED : AnsiColors.ANSI_RESET) + String.format("%-28s",value1) + AnsiColors.ANSI_RESET);
        builder.append("| ");
        builder.append((report.isNoColor() == false ? AnsiColors.ANSI_RED : AnsiColors.ANSI_RESET) + "TLS Handshake failed" + AnsiColors.ANSI_RESET);
        builder.append("\n");
        return builder;
    }
    
    private StringBuilder prettyAppendHSDetail2Row(StringBuilder builder, String tlsClient, String tlsVersion, String ciphersuite, String forwardSecrecy, String keyLength) {
        builder.append(String.format("%-28s", tlsClient));
        builder.append(String.format("| %-14s", tlsVersion));
        builder.append(String.format("| %-52s", ciphersuite));
        builder.append(String.format("| %-19s", forwardSecrecy));
        builder.append(String.format("| %-17s", keyLength));
        builder.append("\n");
        return builder;
    }
    
    private String getBlackString(String value, String format) {
        return String.format(format,value);
    }
    
    private String getGreenString(String value, String format) {
        return (report.isNoColor() == false ? AnsiColors.ANSI_GREEN : AnsiColors.ANSI_RESET) + String.format(format,value) + AnsiColors.ANSI_RESET;
    }
    
    private String getYellowString(String value, String format) {
        return (report.isNoColor() == false ? AnsiColors.ANSI_YELLOW : AnsiColors.ANSI_RESET) + String.format(format,value) + AnsiColors.ANSI_RESET;
    }
    
    private String getRedString(String value, String format) {
        return (report.isNoColor() == false ? AnsiColors.ANSI_RED : AnsiColors.ANSI_RESET) + String.format(format,value) + AnsiColors.ANSI_RESET;
    }
    
    private StringBuilder prettyAppendHSDetail2Row(StringBuilder builder, String tlsClient, Boolean secure, ProtocolVersion tlsVersion, CipherSuite ciphersuite, Boolean forwardSecrecy, String keyLength) {
        String newTlsClient;
        if (secure) {
            newTlsClient = getGreenString(tlsClient, "%-28s");
        } else {
            newTlsClient = getRedString(tlsClient, "%-28s");
        }
        String newTlsVersion = getProtocolVersionColour(tlsVersion, "%-14s");
        String newCipherSuite = getCipherSuiteColour(ciphersuite, "%-52s");
        String newForwardSecrecy;
        if (forwardSecrecy) {
            newForwardSecrecy = getGreenString("Forward Secrecy", "%-19s");
        } else {
            newForwardSecrecy = getRedString("No Forward Secrecy", "%-19s");
        }
        builder.append(newTlsClient);
        builder.append("| ").append(newTlsVersion);
        builder.append("| ").append(newCipherSuite);
        builder.append("| ").append(newForwardSecrecy);
        builder.append("| ").append(getBlackString(keyLength+" Bit", "%-17s"));
        builder.append("\n");
        return builder;
    }
    
    private StringBuilder appendHSDetail3(StringBuilder builder) {
        prettyAppendHeading(builder, "TLS Handshake Simulation - Level of Detail: 3");
        for (SimulatedClient simulatedClient : report.getSimulatedClientList()) {
            prettyAppendHeading(builder, simulatedClient.getType() + ":" + simulatedClient.getVersion());
            prettyAppendGreenRed(builder, "TLS Handshake Successful", simulatedClient.getReceivedServerHelloDone());
            if (simulatedClient.getReceivedServerHelloDone()) {
                prettyAppendGreenRed(builder, "Connection Secure", simulatedClient.getConnectionSecure());
                builder.append("\n");
                prettyAppend(builder, "Protocol Version Client", getProtocolVersionColour(simulatedClient.getHighestClientProtocolVersion(),"%s"));
                prettyAppend(builder, "Protocol Version Selected", getProtocolVersionColour(simulatedClient.getSelectedProtocolVersion(),"%s"));
                prettyAppendGreenRed(builder, "Protocol Version is highest", simulatedClient.getHighestPossibleProtocolVersionSeleceted());
                builder.append("\n");
                prettyAppendSelectedCipherSuite(builder, "Selected Ciphersuite", simulatedClient.getSelectedCiphersuite());
                prettyAppendGreenRed(builder, "Forward Secrecy", simulatedClient.getForwardSecrecy());
                builder.append("\n");
                prettyAppend(builder, "Server Public Key Length (Bits)", simulatedClient.getServerPublicKeyLength());
                prettyAppend(builder, "Named Group", simulatedClient.getSelectedNamedGroup());
                builder.append("\n");
                prettyAppend(builder, "Selected Compression Method", simulatedClient.getSelectedCompressionMethod().name());
                prettyAppend(builder, "Negotiated Extensions", simulatedClient.getNegotiatedExtensionSet().toString());
                builder.append("\n");
                builder.append("Vulnerabilities").append("\n");
                builder.append("\n");
                prettyAppendRedGreen(builder, "Padding Oracle", simulatedClient.getPaddingOracleVulnerable());
                prettyAppendRedGreen(builder, "Bleichenbacher", simulatedClient.getBleichenbacherVulnerable());
                prettyAppendRedGreen(builder, "Crime", simulatedClient.getCrimeVulnerable());
                prettyAppendRedGreen(builder, "Sweet 32", simulatedClient.getSweet32Vulnerable());
                prettyAppendRedGreen(builder, "DROWN", simulatedClient.getDrownVulnerable());
            }
        }
        return builder;
    }
    
    private String getProtocolVersionColour(ProtocolVersion version, String format) {
        if (version.name().contains("13") || version.name().contains("12")) {
            return getGreenString(version.name(),format);
        } else if (version.name().contains("11") || version.name().contains("10")) {
            return getYellowString(version.name(),format);
        } else if (version.name().contains("SSL")) {
            return getRedString(version.name(),format);
        } else {
            return getBlackString(version.name(),format);
        }
    }
    
    private StringBuilder appendRfc(StringBuilder builder) {
        prettyAppendHeading(builder, "RFC");
        prettyAppendRedOnFailure(builder, "Checks MAC", report.getChecksMac());
        prettyAppendRedOnFailure(builder, "Checks Finished", report.getChecksFinished());
        return builder;
    }

    private StringBuilder appendRenegotiation(StringBuilder builder) {
        prettyAppendHeading(builder, "Renegotioation & SCSV");
        prettyAppendYellowOnSuccess(builder, "Clientside Secure", report.getSupportsClientSideSecureRenegotiation());
        prettyAppendRedOnSuccess(builder, "Clientside Insecure", report.getSupportsClientSideInsecureRenegotiation());
        prettyAppendRedOnFailure(builder, "SCSV Fallback", report.getTlsFallbackSCSVsupported());
        return builder;
    }

    private StringBuilder appendCertificate(StringBuilder builder) {
        if (report.getCertificateReports() != null && !report.getCertificateReports().isEmpty()) {
            prettyAppendHeading(builder, "Certificates");
            for (CertificateReport report : report.getCertificateReports()) {
                builder.append(report.toString()).append("\n");
            }
            prettyAppendHeading(builder, "Certificate Checks");
            prettyAppendRedOnSuccess(builder, "Expired Certificates", report.getCertificateExpired());
            prettyAppendRedOnSuccess(builder, "Not yet Valid Certificates", report.getCertificateNotYetValid());
            prettyAppendRedOnSuccess(builder, "Weak Hash Algorithms", report.getCertificateHasWeakHashAlgorithm());
            prettyAppendRedOnSuccess(builder, "Weak Signature Algorithms ", report.getCertificateHasWeakSignAlgorithm());
            prettyAppendRedOnFailure(builder, "Matches Domain", report.getCertificateMachtesDomainName());
            prettyAppendGreenOnSuccess(builder, "Only Trusted", report.getCertificateIsTrusted());
            prettyAppendRedOnFailure(builder, "Contains Blacklisted", report.getCertificateKeyIsBlacklisted());
        }
        return builder;
    }

    private StringBuilder appendSession(StringBuilder builder) {
        prettyAppendHeading(builder, "Session");
        prettyAppendYellowOnFailure(builder, "Supports Session resumption", report.getSupportsSessionIds());
        prettyAppendYellowOnFailure(builder, "Supports Session Tickets", report.getSupportsSessionTicket());
        prettyAppend(builder, "Session Ticket Hint", report.getSessionTicketLengthHint());
        prettyAppendYellowOnFailure(builder, "Session Ticket Rotation", report.getSessionTicketGetsRotated());
        prettyAppendRedOnFailure(builder, "Ticketbleed", report.getVulnerableTicketBleed());
        return builder;
    }

    private StringBuilder appendGcm(StringBuilder builder) {
        prettyAppendHeading(builder, "GCM");
        prettyAppendRedOnFailure(builder, "GCM Nonce reuse", report.getGcmReuse());
        if (null == report.getGcmPattern()) {
            prettyAppend(builder, "GCM Pattern", (String) null);
        } else {
            switch (report.getGcmPattern()) {
                case AKWARD:
                    prettyAppendYellow(builder, "GCM Pattern" + report.getGcmPattern().name());
                    break;
                case INCREMENTING:
                case RANDOM:
                    prettyAppendGreen(builder, "GCM Pattern" + report.getGcmPattern().name());
                    break;
                case REPEATING:
                    prettyAppendRed(builder, "GCM Pattern" + report.getGcmPattern().name());
                    break;
                default:
                    prettyAppend(builder, addIndentations("GCM Pattern") + report.getGcmPattern().name());
                    break;
            }
        }
        prettyAppendRedOnFailure(builder, "GCM Check", report.getGcmCheck());
        return builder;
    }

    private StringBuilder appendIntolerances(StringBuilder builder) {
        prettyAppendHeading(builder, "Intolerances");
        prettyAppendRedOnFailure(builder, "Version", report.getVersionIntolerance());
        prettyAppendRedOnFailure(builder, "Ciphersuite", report.getCipherSuiteIntolerance());
        prettyAppendRedOnFailure(builder, "Extension", report.getExtensionIntolerance());
        return builder;
    }

    private StringBuilder appendAttackVulnerabilities(StringBuilder builder) {
        prettyAppendHeading(builder, "Attack Vulnerabilities");
        prettyAppendRedGreen(builder, "Padding Oracle", report.getPaddingOracleVulnerable());
        prettyAppendRedGreen(builder, "Bleichenbacher", report.getBleichenbacherVulnerable());
        prettyAppendRedGreen(builder, "CRIME", report.getCrimeVulnerable());
        prettyAppendRedGreen(builder, "Breach", report.getBreachVulnerable());
        prettyAppendRedGreen(builder, "Invalid Curve", report.getInvalidCurveVulnerable());
        prettyAppendRedGreen(builder, "Invalid Curve Ephemerals", report.getInvalidCurveEphermaralVulnerable());
        prettyAppendRedGreen(builder, "SSL Poodle", report.getPoodleVulnerable());
        prettyAppendRedGreen(builder, "TLS Poodle", report.getTlsPoodleVulnerable());
        prettyAppendRedGreen(builder, "CVE-20162107", report.getCve20162107Vulnerable());
        prettyAppendRedGreen(builder, "Logjam", report.getLogjamVulnerable());
        prettyAppendRedGreen(builder, "Sweet 32", report.getSweet32Vulnerable());
        prettyAppendDrown(builder, "DROWN", report.getDrownVulnerable());
        prettyAppendRedGreen(builder, "Lucky13", report.getLucky13Vulnerable());
        prettyAppendRedGreen(builder, "Heartbleed", report.getHeartbleedVulnerable());
        prettyAppendEarlyCcs(builder, "EarlyCcs", report.getEarlyCcsVulnerable());
        return builder;
    }

    private StringBuilder appendCipherSuites(StringBuilder builder) {
        if (report.getCipherSuites() != null) {
            prettyAppendHeading(builder, "Supported Ciphersuites");
            for (CipherSuite suite : report.getCipherSuites()) {
                prettyAppendSupportedCipherSuite(builder, suite);
            }

            for (VersionSuiteListPair versionSuitePair : report.getVersionSuitePairs()) {
                prettyAppendHeading(builder, "Supported in " + versionSuitePair.getVersion());
                for (CipherSuite suite : versionSuitePair.getCiphersuiteList()) {
                    prettyAppendSupportedCipherSuite(builder, suite);
                }
            }
            prettyAppendHeading(builder, "Symmetric Supported");
            prettyAppendRedOnSuccess(builder, "Null", report.getSupportsNullCiphers());
            prettyAppendRedOnSuccess(builder, "Export", report.getSupportsExportCiphers());
            prettyAppendRedOnSuccess(builder, "Anon", report.getSupportsAnonCiphers());
            prettyAppendRedOnSuccess(builder, "DES", report.getSupportsDesCiphers());
            prettyAppendYellowOnSuccess(builder, "SEED", report.getSupportsSeedCiphers());
            prettyAppendYellowOnSuccess(builder, "IDEA", report.getSupportsIdeaCiphers());
            prettyAppendRedOnSuccess(builder, "RC2", report.getSupportsRc2Ciphers());
            prettyAppendRedOnSuccess(builder, "RC4", report.getSupportsRc4Ciphers());
            prettyAppendYellowOnSuccess(builder, "3DES", report.getSupportsTrippleDesCiphers());
            prettyAppend(builder, "AES", report.getSupportsAes());
            prettyAppend(builder, "CAMELLIA", report.getSupportsCamellia());
            prettyAppend(builder, "ARIA", report.getSupportsAria());
            prettyAppendGreenOnSuccess(builder, "CHACHA20 POLY1305", report.getSupportsChacha());

            prettyAppendHeading(builder, "KeyExchange Supported");
            prettyAppendYellowOnSuccess(builder, "RSA", report.getSupportsRsa());
            prettyAppend(builder, "DH", report.getSupportsDh());
            prettyAppend(builder, "ECDH", report.getSupportsEcdh());
            prettyAppendYellowOnSuccess(builder, "GOST", report.getSupportsGost());
            prettyAppend(builder, "SRP", report.getSupportsSrp());
            prettyAppend(builder, "Kerberos", report.getSupportsKerberos());
            prettyAppend(builder, "Plain PSK", report.getSupportsPskPlain());
            prettyAppend(builder, "PSK RSA", report.getSupportsPskRsa());
            prettyAppend(builder, "PSK DHE", report.getSupportsPskDhe());
            prettyAppend(builder, "PSK ECDHE", report.getSupportsPskEcdhe());
            prettyAppendYellowOnSuccess(builder, "Fortezza", report.getSupportsFortezza());
            prettyAppendGreenOnSuccess(builder, "New Hope", report.getSupportsNewHope());
            prettyAppendGreenOnSuccess(builder, "ECMQV", report.getSupportsEcmqv());

            prettyAppendHeading(builder, "Perfect Forward Secrecy");
            prettyAppendGreenOnSuccess(builder, "Supports PFS", report.getSupportsPfsCiphers());
            prettyAppendGreenOnSuccess(builder, "Prefers PFS", report.getPrefersPfsCiphers());
            prettyAppendGreenOnSuccess(builder, "Supports Only PFS", report.getSupportsOnlyPfsCiphers());

            prettyAppendHeading(builder, "Cipher Types Supports");
            prettyAppend(builder, "Stream", report.getSupportsStreamCiphers());
            prettyAppend(builder, "Block", report.getSupportsBlockCiphers());
            prettyAppendGreenOnSuccess(builder, "AEAD", report.getSupportsAeadCiphers());

            prettyAppendHeading(builder, "Ciphersuite General");
            prettyAppendGreenRed(builder, "Enforces Ciphersuite ordering", report.getEnforcesCipherSuiteOrdering());
        }
        return builder;
    }

    private StringBuilder appendProtocolVersions(StringBuilder builder) {
        if (report.getVersions() != null) {
            prettyAppendHeading(builder, "Supported Protocol Versions");
            for (ProtocolVersion version : report.getVersions()) {
                builder.append(version.name()).append("\n");
            }
            prettyAppendHeading(builder, "Versions");
            prettyAppendRedGreen(builder, "SSL 2.0", report.getSupportsSsl2());
            prettyAppendRedGreen(builder, "SSL 3.0", report.getSupportsSsl3());
            prettyAppendYellowOnFailure(builder, "TLS 1.0", report.getSupportsTls10());
            prettyAppendYellowOnFailure(builder, "TLS 1.1", report.getSupportsTls11());
            prettyAppendRedOnFailure(builder, "TLS 1.2", report.getSupportsTls12());
            prettyAppendGreenOnSuccess(builder, "TLS 1.3", report.getSupportsTls13());
            prettyAppendYellowOnSuccess(builder, "TLS 1.3 Draft 14", report.getSupportsTls13Draft14());
            prettyAppendYellowOnSuccess(builder, "TLS 1.3 Draft 15", report.getSupportsTls13Draft15());
            prettyAppendYellowOnSuccess(builder, "TLS 1.3 Draft 16", report.getSupportsTls13Draft16());
            prettyAppendYellowOnSuccess(builder, "TLS 1.3 Draft 17", report.getSupportsTls13Draft17());
            prettyAppendGreenOnSuccess(builder, "TLS 1.3 Draft 18", report.getSupportsTls13Draft18());
            prettyAppendGreenOnSuccess(builder, "TLS 1.3 Draft 19", report.getSupportsTls13Draft19());
            prettyAppendGreenOnSuccess(builder, "TLS 1.3 Draft 20", report.getSupportsTls13Draft20());
            prettyAppendGreenOnSuccess(builder, "TLS 1.3 Draft 21", report.getSupportsTls13Draft21());
            prettyAppendGreenOnSuccess(builder, "TLS 1.3 Draft 22", report.getSupportsTls13Draft22());
            //prettyAppend(builder, "DTLS 1.0", report.getSupportsDtls10());
            //prettyAppend(builder, "DTLS 1.2", report.getSupportsDtls10());
            //prettyAppend(builder, "DTLS 1.3", report.getSupportsDtls13());
        }
        return builder;
    }

    private StringBuilder appendExtensions(StringBuilder builder) {
        if (report.getSupportedExtensions() != null) {
            prettyAppendHeading(builder, "Supported Extensions");
            for (ExtensionType type : report.getSupportedExtensions()) {
                builder.append(type.name()).append("\n");
            }
        }
        prettyAppendHeading(builder, "Extensions");
        prettyAppendGreenRed(builder, "Secure Renegotiation", report.getSupportsSecureRenegotiation());
        prettyAppendGreenOnSuccess(builder, "Supports Extended Master Secret", report.getSupportsExtendedMasterSecret());
        prettyAppendGreenOnSuccess(builder, "Supports Encrypt Then Mac", report.getSupportsEncryptThenMacSecret());
        prettyAppendGreenOnSuccess(builder, "Supports Tokenbinding", report.getSupportsTokenbinding());

        if (report.getSupportsTokenbinding() == Boolean.TRUE) {
            prettyAppendHeading(builder, "Tokenbinding Version");
            for (TokenBindingVersion version : report.getSupportedTokenBindingVersion()) {
                builder.append(version.toString()).append("\n");
            }

            prettyAppendHeading(builder, "Tokenbinding Key Parameters");
            for (TokenBindingKeyParameters keyParameter : report.getSupportedTokenBindingKeyParameters()) {
                builder.append(keyParameter.toString()).append("\n");
            }
        }
        appendCurves(builder);
        appendSignatureAndHashAlgorithms(builder);
        return builder;
    }

    private StringBuilder appendCurves(StringBuilder builder) {
        if (report.getSupportedNamedGroups() != null) {
            prettyAppendHeading(builder, "Supported Named Groups");
            if (report.getSupportedNamedGroups().size() > 0) {
                for (NamedGroup group : report.getSupportedNamedGroups()) {
                    builder.append(group.name()).append("\n");
                }
            } else {
                builder.append("none\n");
            }
        }
        return builder;
    }

    private StringBuilder appendSignatureAndHashAlgorithms(StringBuilder builder) {
        if (report.getSupportedSignatureAndHashAlgorithms() != null) {
            prettyAppendHeading(builder, "Supported Signature and Hash Algorithms");
            if (report.getSupportedSignatureAndHashAlgorithms().size() > 0) {
                for (SignatureAndHashAlgorithm algorithm : report.getSupportedSignatureAndHashAlgorithms()) {
                    prettyAppend(builder, algorithm.toString());
                }
            } else {
                builder.append("none\n");
            }
        }
        return builder;
    }

    private StringBuilder appendCompressions(StringBuilder builder) {
        if (report.getSupportedCompressionMethods() != null) {
            prettyAppendHeading(builder, "Supported Compressions");
            for (CompressionMethod compression : report.getSupportedCompressionMethods()) {
                prettyAppend(builder, compression.name());
            }
        }
        return builder;
    }

    private void prettyAppendSupportedCipherSuite(StringBuilder builder, CipherSuite suite) {
        CipherSuiteGrade grade = CiphersuiteRater.getGrade(suite);
        switch (grade) {
            case GOOD:
                prettyAppendGreen(builder, suite.name());
                break;
            case LOW:
                prettyAppendRed(builder, suite.name());
                break;
            case MEDIUM:
                prettyAppendYellow(builder, suite.name());
                break;
            case NONE:
                prettyAppend(builder, suite.name());
                break;
            default:
                prettyAppend(builder, suite.name());
        }
    }
    
    private void prettyAppendSelectedCipherSuite(StringBuilder builder, String name, CipherSuite suite) {
        CipherSuiteGrade grade = CiphersuiteRater.getGrade(suite);
        switch (grade) {
            case GOOD:
                prettyAppendGreen(builder, name, suite.name());
                break;
            case LOW:
                prettyAppendRed(builder, name, suite.name());
                break;
            case MEDIUM:
                prettyAppendYellow(builder, name, suite.name());
                break;
            case NONE:
                prettyAppend(builder, name, suite.name());
                break;
            default:
                prettyAppend(builder, name, suite.name());
        }
    }    
    
    private StringBuilder prettyAppend(StringBuilder builder, String value) {
        return builder.append(value).append("\n");
    }
    
    private StringBuilder prettyAppend(StringBuilder builder, String name, String value) {
        return builder.append(addIndentations(name)).append(": ").append(value == null ? "Unknown" : value).append("\n");
    }

    private StringBuilder prettyAppend(StringBuilder builder, String name, Long value) {
        return builder.append(addIndentations(name)).append(": ").append(value == null ? "Unknown" : value).append("\n");
    }

    private StringBuilder prettyAppend(StringBuilder builder, String name, Boolean value) {
        return builder.append(addIndentations(name)).append(": ").append(value == null ? "Unknown" : value).append("\n");
    }

    private StringBuilder prettyAppendGreenOnSuccess(StringBuilder builder, String name, Boolean value) {
        return builder.append(addIndentations(name)).append(": ").append(value == null ? "Unknown" : (value == Boolean.TRUE ? (report.isNoColor() == false ? AnsiColors.ANSI_GREEN : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET : value)).append("\n");
    }

    private StringBuilder prettyAppendGreenOnFailure(StringBuilder builder, String name, Boolean value) {
        return builder.append(addIndentations(name)).append(": ").append(value == null ? "Unknown" : (value == Boolean.TRUE ? value : (report.isNoColor() == false ? AnsiColors.ANSI_GREEN : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET)).append("\n");
    }

    private StringBuilder prettyAppendRedOnSuccess(StringBuilder builder, String name, Boolean value) {
        return builder.append(addIndentations(name)).append(": ").append(value == null ? "Unknown" : (value == Boolean.TRUE ? (report.isNoColor() == false ? AnsiColors.ANSI_RED : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET : value)).append("\n");
    }

    private StringBuilder prettyAppendRedOnFailure(StringBuilder builder, String name, Boolean value) {
        return builder.append(addIndentations(name)).append(": ").append(value == null ? "Unknown" : (value == Boolean.TRUE ? value : (report.isNoColor() == false ? AnsiColors.ANSI_RED : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET)).append("\n");
    }

    private StringBuilder prettyAppendYellowOnFailure(StringBuilder builder, String name, Boolean value) {
        return builder.append(addIndentations(name)).append(": ").append(value == null ? "Unknown" : (value == Boolean.TRUE ? value : (report.isNoColor() == false ? AnsiColors.ANSI_YELLOW : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET)).append("\n");
    }

    private StringBuilder prettyAppendYellowOnSuccess(StringBuilder builder, String name, Boolean value) {
        return builder.append(addIndentations(name)).append(": ").append(value == null ? "Unknown" : (value == Boolean.TRUE ? (report.isNoColor() == false ? AnsiColors.ANSI_YELLOW : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET : value)).append("\n");
    }

    private StringBuilder prettyAppendGreenRed(StringBuilder builder, String name, Boolean value) {
        return builder.append(addIndentations(name)).append(": ").append(value == null ? "Unknown" : (value == Boolean.TRUE ? (report.isNoColor() == false ? AnsiColors.ANSI_GREEN : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET : (report.isNoColor() == false ? AnsiColors.ANSI_RED : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET)).append("\n");
    }

    private StringBuilder prettyAppendRedGreen(StringBuilder builder, String name, Boolean value) {
        return builder.append(addIndentations(name)).append(": ").append(value == null ? "Unknown" : (value == Boolean.TRUE ? (report.isNoColor() == false ? AnsiColors.ANSI_RED : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET : (report.isNoColor() == false ? AnsiColors.ANSI_GREEN : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET)).append("\n");
    }

    private StringBuilder prettyAppendYellow(StringBuilder builder, String value) {
        return builder.append((report.isNoColor() == false ? AnsiColors.ANSI_YELLOW : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET).append("\n");
    }
    
    private StringBuilder prettyAppendYellow(StringBuilder builder, String name, String value) {
        return builder.append(addIndentations(name)).append(": ").append((report.isNoColor() == false ? AnsiColors.ANSI_YELLOW : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET).append("\n");
    }    

    private StringBuilder prettyAppendRed(StringBuilder builder, String value) {
        return builder.append((report.isNoColor() == false ? AnsiColors.ANSI_RED : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET).append("\n");
    }
    
    private StringBuilder prettyAppendRed(StringBuilder builder, String name, String value) {
        return builder.append(addIndentations(name)).append(": ").append((report.isNoColor() == false ? AnsiColors.ANSI_RED : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET).append("\n");
    }

    private StringBuilder prettyAppendGreen(StringBuilder builder, String value) {
        return builder.append((report.isNoColor() == false ? AnsiColors.ANSI_GREEN : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET).append("\n");
    }
    
    private StringBuilder prettyAppendGreen(StringBuilder builder, String name, String value) {
        return builder.append(addIndentations(name)).append(": ").append((report.isNoColor() == false ? AnsiColors.ANSI_GREEN : AnsiColors.ANSI_RESET) + value + AnsiColors.ANSI_RESET).append("\n");
    }

    private StringBuilder prettyAppendHeading(StringBuilder builder, String value) {
        return builder.append((report.isNoColor() == false ? AnsiColors.ANSI_BOLD + AnsiColors.ANSI_BLUE : AnsiColors.ANSI_RESET) + "\n--------------------------------------------------------\n" + value + "\n\n" + AnsiColors.ANSI_RESET);
    }

    private void prettyAppendDrown(StringBuilder builder, String testName, DrownVulnerabilityType drownVulnerable) {
        builder.append(addIndentations(testName)).append(": ");
        if (drownVulnerable == null) {
            prettyAppend(builder, null);
            return;
        }
        switch (drownVulnerable) {
            case FULL:
                prettyAppendRed(builder, "true - fully exploitable");
                break;
            case SSL2:
                prettyAppendRed(builder, "true - SSL 2 supported!");
                break;
            case NONE:
                prettyAppendGreen(builder, "false");
                break;
            case UNKNOWN:
                prettyAppend(builder, null);
                break;
        }
    }

    private void prettyAppendEarlyCcs(StringBuilder builder, String testName, EarlyCcsVulnerabilityType earlyCcsVulnerable) {
        builder.append(addIndentations(testName)).append(": ");
        if (earlyCcsVulnerable == null) {
            prettyAppend(builder, "null");
            return;
        }
        switch (earlyCcsVulnerable) {
            case VULN_EXPLOITABLE:
                prettyAppendRed(builder, "true - exploitable");
                break;
            case VULN_NOT_EXPLOITABLE:
                prettyAppendRed(builder, "true - probably not exploitable");
                break;
            case NOT_VULNERABLE:
                prettyAppendGreen(builder, "false");
                break;
            case UNKNOWN:
                prettyAppend(builder, "null");
                break;
        }
    }

    private String addIndentations(String value) {
        StringBuilder builder = new StringBuilder();
        builder.append(value);
        if (value.length() < 8) {
            builder.append("\t\t\t\t ");
        } else if (value.length() < 16) {
            builder.append("\t\t\t ");
        } else if (value.length() < 24) {
            builder.append("\t\t ");
        } else if (value.length() < 32) {
            builder.append("\t ");
        } else {
            builder.append(" ");
        }
        return builder.toString();
    }
}
