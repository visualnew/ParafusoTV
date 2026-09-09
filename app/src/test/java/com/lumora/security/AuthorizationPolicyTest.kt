package com.lumora.security

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthorizationPolicyTest {

    @Test
    fun `adult to adult without a destination PIN needs no credential`() {
        assertEquals(
            CredentialRequirement.NONE,
            AuthorizationPolicy.requirementFor(AuthorizationRequest.SwitchProfile(false, false))
        )
    }

    @Test
    fun `adult to protected adult needs the destination profile PIN`() {
        assertEquals(
            CredentialRequirement.DESTINATION_PROFILE_PIN,
            AuthorizationPolicy.requirementFor(AuthorizationRequest.SwitchProfile(false, true))
        )
    }

    @Test
    fun `leaving a child profile for an unprotected adult needs the administrative PIN`() {
        assertEquals(
            CredentialRequirement.ADMIN_PIN,
            AuthorizationPolicy.requirementFor(AuthorizationRequest.SwitchProfile(true, false))
        )
    }

    @Test
    fun `leaving a child profile for a protected adult needs both credentials`() {
        assertEquals(
            CredentialRequirement.ADMIN_AND_DESTINATION_PROFILE_PIN,
            AuthorizationPolicy.requirementFor(AuthorizationRequest.SwitchProfile(true, true))
        )
    }

    @Test
    fun `leaving a child profile without an administrative PIN requires secure setup`() {
        assertEquals(
            CredentialRequirement.ADMIN_PIN_SETUP,
            AuthorizationPolicy.requirementFor(
                AuthorizationRequest.SwitchProfile(activeProfileIsChild = true, destinationHasPin = false),
                adminPinConfigured = false
            )
        )
    }

    @Test
    fun `child to protected profile without an administrative PIN requires setup before both credentials`() {
        assertEquals(
            CredentialRequirement.ADMIN_PIN_SETUP,
            AuthorizationPolicy.requirementFor(
                AuthorizationRequest.SwitchProfile(activeProfileIsChild = true, destinationHasPin = true),
                adminPinConfigured = false
            )
        )
    }

    @Test
    fun `configured administrative PIN preserves the child to protected profile requirement`() {
        assertEquals(
            CredentialRequirement.ADMIN_AND_DESTINATION_PROFILE_PIN,
            AuthorizationPolicy.requirementFor(
                AuthorizationRequest.SwitchProfile(activeProfileIsChild = true, destinationHasPin = true),
                adminPinConfigured = true
            )
        )
    }

    @Test
    fun `unlocking child protected content needs the administrative PIN`() {
        assertEquals(
            CredentialRequirement.ADMIN_PIN,
            AuthorizationPolicy.requirementFor(AuthorizationRequest.UnlockChildProtectedContent)
        )
    }

    @Test
    fun `administrative and parental actions need the administrative PIN`() {
        val requests = listOf(
            AuthorizationRequest.OpenAdministrativeSettings,
            AuthorizationRequest.EditParentalControls,
            AuthorizationRequest.ChangeProfilePin,
            AuthorizationRequest.RemoveProfile,
            AuthorizationRequest.EditProviders
        )

        requests.forEach { request ->
            assertEquals(CredentialRequirement.ADMIN_PIN, AuthorizationPolicy.requirementFor(request))
        }
    }

    @Test
    fun `changing an existing administrative PIN requires the current administrative PIN`() {
        assertEquals(
            CredentialRequirement.ADMIN_PIN,
            AuthorizationPolicy.requirementFor(AuthorizationRequest.ChangeAdministrativePin(true))
        )
    }

    @Test
    fun `creating the first administrative PIN uses the setup flow`() {
        assertEquals(
            CredentialRequirement.ADMIN_PIN_SETUP,
            AuthorizationPolicy.requirementFor(AuthorizationRequest.ChangeAdministrativePin(false))
        )
    }

    @Test
    fun `restoring on a configured device needs the administrative PIN`() {
        assertEquals(
            CredentialRequirement.ADMIN_PIN,
            AuthorizationPolicy.requirementFor(AuthorizationRequest.RestoreBackup(true))
        )
    }

    @Test
    fun `restoring on an unconfigured device has no existing administrative credential`() {
        assertEquals(
            CredentialRequirement.NONE,
            AuthorizationPolicy.requirementFor(AuthorizationRequest.RestoreBackup(false))
        )
    }
}
