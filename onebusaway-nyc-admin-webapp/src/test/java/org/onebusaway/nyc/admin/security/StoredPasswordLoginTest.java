/**
 * Copyright (C) 2026 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.admin.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.users.impl.authentication.VersionedPasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Checks that an admin password in the format already stored in the users database still logs in
 * on Spring Security 5.8 (OBANYC-4240).
 *
 * <p>Admin login uses the library's {@code daoAuthenticationProvider} with
 * {@code passwordEncoderV2}: a {@link VersionedPasswordEncoder} ("v2") around
 * {@link BCryptPasswordEncoder} (onebusaway-users {@code application-context.xml} and
 * {@code application-context-security.xml}). This builds the same two pieces by hand, because
 * loading those XML files needs the users database.</p>
 */
public class StoredPasswordLoginTest {

  /** Password and its stored form ("v2|" + bcrypt), as the users table holds it. */
  private static final String PASSWORD = "Java21-upgrade-test";
  private static final String STORED_HASH =
      "v2|$2a$10$96BghdIMUggk1zTOiK3KBO8r9dADKUQ6QOXXJ1.kW8u4MAO6Ddqji";

  private DaoAuthenticationProvider provider;

  @Before
  public void setUp() {
    VersionedPasswordEncoder encoder = new VersionedPasswordEncoder();
    encoder.setVersionId("v2");
    encoder.setPasswordEncoder(new BCryptPasswordEncoder());

    UserDetailsService users = mock(UserDetailsService.class);
    when(users.loadUserByUsername("admin")).thenReturn(new User("admin", STORED_HASH,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

    provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(users);
    provider.setPasswordEncoder(encoder);
  }

  @Test
  public void storedPasswordLogsIn() {
    Authentication result =
        provider.authenticate(new UsernamePasswordAuthenticationToken("admin", PASSWORD));
    assertTrue(result.isAuthenticated());
    assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
  }

  @Test(expected = BadCredentialsException.class)
  public void wrongPasswordIsRefused() {
    provider.authenticate(new UsernamePasswordAuthenticationToken("admin", "not-the-password"));
  }

  @Test
  public void hashWithoutVersionPrefixIsRefused() {
    // VersionedPasswordEncoder only accepts "<version>|<hash>"; a bare bcrypt hash never matches.
    VersionedPasswordEncoder encoder = new VersionedPasswordEncoder();
    encoder.setVersionId("v2");
    encoder.setPasswordEncoder(new BCryptPasswordEncoder());
    assertFalse(encoder.matches(PASSWORD, STORED_HASH.substring("v2|".length())));
  }
}
