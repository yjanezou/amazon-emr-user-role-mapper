package com.amazon.aws.emr.api;

import com.amazonaws.util.StringUtils;

import java.io.InputStream;
import java.security.Principal;
import java.security.cert.CertificateFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Utility class to for ssl client cert operations.
 *
 *
 */
public class CertUtil {
    static final String DN_ATTRIBUTE_CN = "CN";
    static final String DN_ATTRIBUTE_OU = "OU";
    static final String DN_ATTRIBUTE_ORG = "O";
    static final String DN_ATTRIBUTE_LOCALITY = "L";
    static final String DN_ATTRIBUTE_STATE = "S";
    static final String DN_ATTRIBUTE_COUNTRY = "C";

    private static final String SSL_CLIENT_CERT_HEADER = "ssl_client_cert";
    private static final String FLOWSNAKE_OU_PREFIX = "fs-";

    /**
     *
     * @param request
     * @return
     */
    public static X509Certificate getCertificate(HttpServletRequest request) {
        if (null == request) {
            return null;
        }

        String certInfo = request.getHeader(SSL_CLIENT_CERT_HEADER);

        if (StringUtils.isNullOrEmpty(certInfo)){
            return null;
        }

        try (InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(certInfo))) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
            return cert;
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<String, String> getSubjectAttributes(X509Certificate cert) {
        Map<String, String > result = new HashMap<>();
        Principal subject = cert.getSubjectDN();
        String subjectArray[] = subject.toString().split(",");
        for (String s : subjectArray) {
            String[] str = s.trim().split("=");
            result.put(str[0], str[1]);
        }

        return result;
    }

    public static String getTeamNameWithFlowsnakeStandard(String orgUnit) {
        if (StringUtils.isNullOrEmpty((orgUnit)) || !orgUnit.contains(FLOWSNAKE_OU_PREFIX)) {
            return null;
        }
        String ouFields[] = orgUnit.split(FLOWSNAKE_OU_PREFIX);
        // whether it's using "spiffe://trust-domain/fs-identifier" or not, the last field should be the team name
        // TODO: double check with Flowsnake team
        return ouFields.length == 0? null : ouFields[ouFields.length-1];
    }
}
