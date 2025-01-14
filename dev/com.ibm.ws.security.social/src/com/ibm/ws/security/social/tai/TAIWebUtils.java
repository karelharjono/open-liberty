/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.http.AuthUtils;
import com.ibm.ws.security.common.web.WebUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.ErrorHandlerImpl;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.Oauth2LoginConfigImpl;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.SocialUtil;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

public class TAIWebUtils {

    public static final TraceComponent tc = Tr.register(TAIWebUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final String JWT_SEGMENTS = "-segments";
    private static final String JWT_SEGMENT_INDEX = "-";
    private static final String Authorization_Header = "Authorization";
    private static final String ACCESS_TOKEN = "access_token";
    WebUtils webUtils = new WebUtils();
    SocialWebUtils socialWebUtils = new SocialWebUtils();
    AuthUtils authUtils = new AuthUtils();
    ReferrerURLCookieHandler referrerURLCookieHandler = null;

    public TAIWebUtils() {
        referrerURLCookieHandler = getCookieHandler();
    }

    @FFDCIgnore(SocialLoginException.class)
    public String getRedirectUrl(HttpServletRequest req, SocialLoginConfig clientConfig) {
        boolean isRedirectConfiguredAndValid = false;
        String host = clientConfig.getRedirectToRPHostAndPort();
        if (host != null && !host.isEmpty()) {
            // Only validate the format if a value has been configured
            try {
                SocialUtil.validateEndpointFormat(host, false);
                isRedirectConfiguredAndValid = true;
            } catch (SocialLoginException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to validate URL format of redirectToRPHostAndPort value [" + host + "] due to " + e.getMessage());
                }
            }
        }
        if (!isRedirectConfiguredAndValid) {
            host = getHostAndPort(req);
        }
        StringBuffer redirect = new StringBuffer(host);
        redirect.append(Oauth2LoginConfigImpl.getContextRoot()).append("/redirect/").append(clientConfig.getUniqueId());
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "redirect=" + redirect);
        }
        return redirect.toString();

    }

    /**
     * Gets and validates the authorization endpoint URL from the provided social login configuration.
     */
    public String getAuthorizationEndpoint(SocialLoginConfig clientConfig) throws SocialLoginException {
        final String authzEndpoint = clientConfig.getAuthorizationEndpoint();
        SocialUtil.validateEndpointWithQuery(authzEndpoint);
        return authzEndpoint;
    }

    /**
     * Generates a random state value, adds a cookie to the response with that value, and returns the value.
     */
    public String createStateCookie(HttpServletRequest request, HttpServletResponse response) {
        String stateValue = SocialUtil.generateRandom();
        String loginHint = socialWebUtils.getLoginHint(request);
        if (!request.getMethod().equalsIgnoreCase("GET") && loginHint != null) {
            stateValue = stateValue + loginHint;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Setting cookie " + ClientConstants.COOKIE_NAME_STATE_KEY + " to " + stateValue);
        }
        Cookie stateCookie = referrerURLCookieHandler.createCookie(ClientConstants.COOKIE_NAME_STATE_KEY, stateValue, request);
        response.addCookie(stateCookie);
        return stateValue;
    }

    public TAIResult sendToErrorPage(HttpServletResponse response, TAIResult taiResult) {
        return ErrorHandlerImpl.getInstance().handleErrorResponse(response, taiResult);
    }

    public void savePostParameters(HttpServletRequest request) {
        PostParameterHelper.savePostParams((IExtendedRequest) request);
    }

    public void restorePostParameters(HttpServletRequest request) {
        PostParameterHelper.restorePostParams((IExtendedRequest) request);
    }

    public ReferrerURLCookieHandler getCookieHandler() {
        WebAppSecurityConfig config = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
        if (config != null) {
            return config.createReferrerURLCookieHandler();
        } else {
            return new ReferrerURLCookieHandler(config);
        }
    }

    String getHostAndPort(HttpServletRequest req) {
        String hostName = req.getServerName();
        Integer httpsPort = webUtils.getRedirectPortFromRequest(req);

        String hostAndPort = null;
        if (httpsPort == null && req.isSecure()) {
            // TODO: need to specify SSL_PORT_IS_NULL message
            // Tr.error(tc, "SSL_PORT_IS_NULL");
            int port = req.getServerPort();
            // return whatever in the req
            String httpSchema = ((javax.servlet.ServletRequest) req).getScheme();
            hostAndPort = httpSchema + "://" + hostName + (port > 0 && port != 443 ? ":" + port : "");
        } else {
            hostAndPort = "https://" + hostName + (httpsPort == null ? "" : ":" + httpsPort);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "hostAndPort=" + hostAndPort);
        }
        return hostAndPort;
    }

    @Sensitive
    public String getBearerAccessToken(HttpServletRequest req, SocialLoginConfig clientConfig) {
        String headerName = clientConfig.getAccessTokenHeaderName();
        if (headerName != null) {
            String bearerToken = getBearerTokenFromCustomHeader(req, headerName);
            if (bearerToken == null) {
                Tr.warning(tc, "CUSTOM_ACCESS_TOKEN_HEADER_MISSING", Oauth2LoginConfigImpl.KEY_accessTokenHeaderName, clientConfig.getUniqueId(), headerName);
            }
            return bearerToken;
        } else {
            return getBearerTokenFromAuthzHeaderOrRequestBody(req);
        }
    }

    @Sensitive
    String getBearerTokenFromCustomHeader(HttpServletRequest req, String headerName) {
        String hdrValue = authUtils.getBearerTokenFromHeader(req, headerName);
        if (hdrValue != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Header value is not null");
            }
            return hdrValue.trim();
        } else {
            return getBearerTokenFromCustomHeaderSegments(req, headerName);
        }
    }

    @FFDCIgnore(Exception.class)
    @Sensitive
    String getBearerTokenFromCustomHeaderSegments(HttpServletRequest req, String headerName) {
        String headerSegments = req.getHeader(headerName + JWT_SEGMENTS);
        if (headerSegments == null) {
            return null;
        }
        String hdrValue = null;
        try {
            hdrValue = buildBearerTokenFromCustomHeaderSegments(req, headerName, Integer.parseInt(headerSegments));
            if (hdrValue != null && hdrValue.isEmpty()) {
                hdrValue = null;
            }
        } catch (Exception e) {
            //can be ignored
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Fail to read Header Segments:", e);
            }
        }
        return hdrValue;
    }

    @Sensitive
    String buildBearerTokenFromCustomHeaderSegments(HttpServletRequest req, String headerName, int numberOfSegments) {
        StringBuffer tokenStringBuilder = new StringBuffer();
        for (int i = 1; i < numberOfSegments + 1; i++) {
            String segHdrValue = req.getHeader(headerName + JWT_SEGMENT_INDEX + i);
            if (segHdrValue != null) {
                tokenStringBuilder.append(segHdrValue.trim());
            }
        }
        return tokenStringBuilder.toString();
    }

    @Sensitive
    String getBearerTokenFromAuthzHeaderOrRequestBody(HttpServletRequest req) {
        String hdrValue = authUtils.getBearerTokenFromHeader(req);
        if (hdrValue == null) {
            String reqMethod = req.getMethod();
            if (ClientConstants.REQ_METHOD_POST.equalsIgnoreCase(reqMethod)) {
                String contentType = req.getHeader(ClientConstants.REQ_CONTENT_TYPE_NAME);
                if (ClientConstants.REQ_CONTENT_TYPE_APP_FORM_URLENCODED.equals(contentType)) {
                    hdrValue = req.getParameter(ACCESS_TOKEN);
                }
            }
        }
        return hdrValue;
    }

}
