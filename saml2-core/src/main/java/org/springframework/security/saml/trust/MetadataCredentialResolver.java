/* Copyright 2011 Vladimir Schafer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.saml.trust;

import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.UsageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.MetadataManager;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Class customizes resolving from metadata by first using values present in the ExtenedeMetadata of an entity.
 *
 * @author Vladimir Schafer
 */
public class MetadataCredentialResolver extends org.opensaml.security.MetadataCredentialResolver {

    /**
     * Class logger.
     */
    private final Logger log = LoggerFactory.getLogger(MetadataCredentialResolver.class);

    /**
     * Metadata manager.
     */
    protected MetadataManager manager;

    /**
     * Key manager.
     */
    protected KeyManager keyManager;

    /**
     * Creates new resolver.
     *
     * @param metadataProvider metadata resolver
     * @param keyManager       key manger
     */
    public MetadataCredentialResolver(MetadataManager metadataProvider, KeyManager keyManager) {
        super(metadataProvider);
        this.manager = metadataProvider;
        this.keyManager = keyManager;
    }

    /**
     * Method tries to resolve all credentials for the given entityID. At first extendedMetadata for the entity is checked,
     * in case any matching credentials are found there they are returned. Otherwise data stored in metadata is used.
     *
     * @param entityID entity ID
     * @param role     role
     * @param protocol protocol
     * @param usage    usage
     * @return credentials usable for trust verification or decryption
     * @throws SecurityException error
     */
    @Override
    protected Collection<Credential> retrieveFromMetadata(String entityID, QName role, String protocol, UsageType usage) throws SecurityException {

        try {

            ExtendedMetadata extendedMetadata = manager.getExtendedMetadata(entityID);

            if (usage.equals(UsageType.UNSPECIFIED)) {
                Collection<Credential> credentials = new LinkedList<Credential>();
                if (extendedMetadata.getSigningKey() != null) {
                    log.debug("Using customized signing key {} from extended metadata for entityID {}", extendedMetadata.getSigningKey(), entityID);
                    credentials.add(keyManager.getCredential(extendedMetadata.getSigningKey()));
                }
                if (extendedMetadata.getEncryptionKey() != null) {
                    log.debug("Using customized encryption key {} from extended metadata for entityID {}", extendedMetadata.getEncryptionKey(), entityID);
                    credentials.add(keyManager.getCredential(extendedMetadata.getEncryptionKey()));
                }
                if (credentials.size() > 0) {
                    return credentials;
                }
            } else if (usage.equals(UsageType.SIGNING)) {
                if (extendedMetadata.getSigningKey() != null) {
                    log.debug("Using customized signing key {} from extended metadata for entityID {}", extendedMetadata.getSigningKey(), entityID);
                    return Collections.singleton(keyManager.getCredential(extendedMetadata.getSigningKey()));
                }
            } else if (usage.equals(UsageType.ENCRYPTION)) {
                if (extendedMetadata.getEncryptionKey() != null) {
                    log.debug("Using customized encryption key {} from extended metadata for entityID {}", extendedMetadata.getEncryptionKey(), entityID);
                    return Collections.singleton(keyManager.getCredential(extendedMetadata.getEncryptionKey()));
                }
            }

            log.debug("No customized signature or encryption keys configured for entityID {}, using metadata", entityID);
            return super.retrieveFromMetadata(entityID, role, protocol, usage);    //To change body of overridden methods use File | Settings | File Templates.

        } catch (MetadataProviderException e) {
            throw new SecurityException("Error loading metadata information", e);
        }

    }

}
