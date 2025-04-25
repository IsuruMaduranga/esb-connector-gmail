/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.connector;

import com.google.code.javax.mail.MessagingException;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.wso2.integration.connector.core.AbstractConnector;
import org.wso2.integration.connector.core.ConnectException;

/**
 * Class which reads OAuth access token and user name parameters from the
 * message context to perform OAuth authentication for Gmail.
 */
public class GmailConfig extends AbstractConnector {

    /*
     * Extracts the values for OAuth access token and user name from the message
     * context and stores them in the message context.
     */
    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        try {
            // Reading OAuth access token and username from the message context
            String accessToken = (String) messageContext.getProperty(GmailConstants.GMAIL_ACCESSTOKEN);
            String userId = GmailUtils.lookupFunctionParam(messageContext, GmailConstants.GMAIL_PARAM_USERNAME);
            // Storing OAuth user login details in the message context
            this.storeOauthUserLogin(messageContext, userId, accessToken);
        } catch (MessagingException e) {
            GmailUtils.storeErrorResponseStatus(messageContext,
                    e,
                    GmailErrorCodes.GMAIL_ERROR_CODE_MESSAGING_EXCEPTION);
            handleException(e.getMessage(), e, messageContext);
        } catch (Exception e) {
            GmailUtils.storeErrorResponseStatus(messageContext, e,
                    GmailErrorCodes.GMAIL_COMMON_EXCEPTION);
            handleException(e.getMessage(), e, messageContext);
        }
    }

    /**
     * Stores user name and access token for OAuth authentication
     *
     * @param messageContext message context where the user login information should be
     *                       stored
     * @param userId         user name
     * @param accessToken    access token
     * @throws com.google.code.javax.mail.MessagingException
     */
    private void storeOauthUserLogin(MessageContext messageContext, String userId,
                                     String accessToken) throws MessagingException {
        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) messageContext).getAxis2MessageContext();
        Object loginMode = axis2MessageContext.getProperty(GmailConstants.GMAIL_LOGIN_MODE);
        if (loginMode != null &&
                (loginMode.toString() == GmailConstants.GMAIL_OAUTH_LOGIN_MODE) &&
                messageContext.getProperty(GmailConstants.GMAIL_OAUTH_USERNAME).toString()
                        .equals(userId) &&
                messageContext.getProperty(GmailConstants.GMAIL_OAUTH_ACCESS_TOKEN).toString()
                        .equals(accessToken)) {
            log.info("The same authentication is already available. Hence no changes are needed.");
            return;
        }

        // Reset already stored instances
        GmailUtils.closeConnection(axis2MessageContext);
        if (log.isDebugEnabled()) {
            log.debug("Setting the logging mode to \"OAUTH\"");
        }
        axis2MessageContext.setProperty(GmailConstants.GMAIL_LOGIN_MODE,
                GmailConstants.GMAIL_OAUTH_LOGIN_MODE);
        if (log.isDebugEnabled()) {
            log.debug("Storing the new username and access token");
        }
        messageContext.setProperty(GmailConstants.GMAIL_OAUTH_USERNAME, userId);
        messageContext.setProperty(GmailConstants.GMAIL_OAUTH_ACCESS_TOKEN, accessToken);
    }
}
