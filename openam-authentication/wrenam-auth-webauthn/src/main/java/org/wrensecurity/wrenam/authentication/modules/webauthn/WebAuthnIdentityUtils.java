package org.wrensecurity.wrenam.authentication.modules.webauthn;

import static com.sun.identity.idm.IdType.USER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.forgerock.util.query.QueryFilter.equalTo;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import java.util.Collections;
import java.util.Set;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.util.query.QueryFilter;

/**
 * Utility methods for working with user identities across WebAuthn authentication module.
 */
public class WebAuthnIdentityUtils {

    /**
     * Get the username of a user by userId.
     *
     * @param userId userId to find user by
     * @param userIdAttr userId attribute name on identity
     * @param realm realm the user belongs to
     * @return the AMIdentity of user satisfying crestQuery
     */
    public static String getUsernameForUserId(byte[] userId, String userIdAttr, String realm)
            throws InternalServerErrorException {
        QueryFilter<JsonPointer> queryFilter = equalTo(new JsonPointer(userIdAttr), new String(userId, UTF_8));
        AMIdentity user = getIdentity(new CrestQuery(queryFilter), realm);
        return user.getName();
    }

    /**
     * Get the {@code AMIdentity} of a user satisfying crestQuery that exists in realm.
     *
     * @param crestQuery crestQuery to find user by
     * @param realm realm the user belongs to
     * @return the AMIdentity of user satisfying crestQuery
     */
    public static AMIdentity getIdentity(CrestQuery crestQuery, String realm) throws InternalServerErrorException {
        AMIdentity amid;
        AMIdentityRepository amIdRepo = AuthD.getAuth().getAMIdentityRepository(realm);
        final IdSearchControl idsc = new IdSearchControl();
        idsc.setAllReturnAttributes(true);
        Set<?> results = Collections.emptySet();
        try {
            IdSearchResults searchResults = amIdRepo.searchIdentities(USER, crestQuery, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }
            if (results.isEmpty()) {
                throw new IdRepoException("getIdentity: User satisfying CREST query is not found");
            } else if (results.size() > 1) {
                throw new IdRepoException("getIdentity: More than one user found satisfying CREST query");
            }
            Object result = results.iterator().next();
            if (result instanceof AMIdentity) {
                amid = (AMIdentity) result;
            } else {
                throw new IdRepoException("getIdentity: Result is not of type AMIdentity");
            }
        } catch (IdRepoException | SSOException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
        return amid;
    }

    /**
     * Return the value of the requested attribute or null if the attribute is not set on the AMIdentity.
     *
     * <p>This method is only valid for AMIdentity objects of type User, Agent, Group, and Role.
     *
     * @param attributeName name of attribute
     * @return attribute value
     * @throws IdRepoException if there are repository related error conditions
     * @throws SSOException if user's single sign on token is invalid
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAttributeValue(AMIdentity identity, String attributeName) throws IdRepoException, SSOException {
        Set<T> attribute = identity.getAttribute(attributeName);
        if (attribute != null && !attribute.isEmpty()) {
            return attribute.iterator().next();
        }
        return null;
    }

}
