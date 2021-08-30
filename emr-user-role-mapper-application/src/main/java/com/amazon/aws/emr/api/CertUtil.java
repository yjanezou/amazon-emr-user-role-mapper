package com.amazon.aws.emr.api;

import com.amazonaws.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.security.Principal;
import java.security.cert.CertificateFactory;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
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
@Slf4j
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
        Object certChain = request.getAttribute("javax.servlet.request.X509Certificate");
        log.info("[PermissionService] Fetched cert attribute: {} ", certChain);

        if (certChain != null) {
            X509Certificate certs[] = (X509Certificate[])certChain;
            for (int i = 0; i < certs.length; i++) {
                log.info ("[PermissionService] Client Certificate [" + i + "] = "
                        + certs[i].toString());
            }
            return certs[0];
        }
        return null;

    }

    public static String getOU(X509Certificate cert) {
        Principal subjectDN = cert.getSubjectDN();
        String dn = subjectDN.getName();

        try {
            LdapName ldapDN = new LdapName(dn);
            for(Rdn rdn: ldapDN.getRdns()) {
                String type = rdn.getType();
                Object value = rdn.getValue();
                log.info("[PermissionService] rdns: " + type + " -> " + rdn.getValue());
                /*
                 *  [PermissionService] rdns: C -> US
                 *  [PermissionService] rdns: O -> Salesforce
                 *  [PermissionService] rdns: OU -> spiffe://trust-domain/fs-ClientTestTeam
                 *  [PermissionService] rdns: CN -> localhost
                 */
                if (DN_ATTRIBUTE_OU.equalsIgnoreCase(type)) {
                    return value.toString();
                }

            }
        } catch (InvalidNameException e) {
            e.printStackTrace();
        }
/*
        Map<String, String > result = new HashMap<>();

        String subjectArray[] = subjectDN.toString().split(",");
        for (String s : subjectArray) {
            String[] str = s.trim().split("=");
            result.put(str[0], str[1]);
        }

        log.info("[PermissionService] Fetched {} subject attributes from cert", result.size());
        return result; */

        return null;
    }

    public static String getTeamNameWithFlowsnakeStandard(String orgUnit) {
        log.info("[PermissionService] Fetching team name from OU: {}", orgUnit);
        if (StringUtils.isNullOrEmpty((orgUnit)) || !orgUnit.contains(FLOWSNAKE_OU_PREFIX)) {
            return null;
        }
        String ouFields[] = orgUnit.split(FLOWSNAKE_OU_PREFIX);
        // whether it's using "spiffe://trust-domain/fs-identifier" or not, the last field should be the team name
        // TODO: double check with Flowsnake team
        String team = ouFields.length == 0? null : ouFields[ouFields.length-1];

        log.info("[PermissionService] Fetched team name as {}", team);
        return team ;
    }
}
