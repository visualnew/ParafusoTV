package com.lumora.security

/** Credentials that a presentation layer must collect before performing an action. */
internal enum class CredentialRequirement {
    NONE,
    ADMIN_PIN,
    DESTINATION_PROFILE_PIN,
    ADMIN_AND_DESTINATION_PROFILE_PIN,
    ADMIN_PIN_SETUP
}

/** Pure authorization requests. This layer deliberately knows nothing about Activity or dialogs. */
internal sealed interface AuthorizationRequest {
    data class SwitchProfile(
        val activeProfileIsChild: Boolean,
        val destinationHasPin: Boolean
    ) : AuthorizationRequest

    data object UnlockChildProtectedContent : AuthorizationRequest
    data object OpenAdministrativeSettings : AuthorizationRequest
    data object EditParentalControls : AuthorizationRequest
    data object ChangeProfilePin : AuthorizationRequest
    data class ChangeAdministrativePin(val adminPinConfigured: Boolean) : AuthorizationRequest
    data object RemoveProfile : AuthorizationRequest
    data class RestoreBackup(val adminPinConfigured: Boolean) : AuthorizationRequest
    data object EditProviders : AuthorizationRequest
}

/** The product authorization matrix, separated from the UI that eventually collects PINs. */
internal object AuthorizationPolicy {
    fun requirementFor(request: AuthorizationRequest): CredentialRequirement = when (request) {
        is AuthorizationRequest.SwitchProfile -> when {
            request.activeProfileIsChild && request.destinationHasPin ->
                CredentialRequirement.ADMIN_AND_DESTINATION_PROFILE_PIN
            request.activeProfileIsChild -> CredentialRequirement.ADMIN_PIN
            request.destinationHasPin -> CredentialRequirement.DESTINATION_PROFILE_PIN
            else -> CredentialRequirement.NONE
        }
        AuthorizationRequest.UnlockChildProtectedContent,
        AuthorizationRequest.OpenAdministrativeSettings,
        AuthorizationRequest.EditParentalControls,
        AuthorizationRequest.ChangeProfilePin,
        AuthorizationRequest.RemoveProfile,
        AuthorizationRequest.EditProviders -> CredentialRequirement.ADMIN_PIN
        is AuthorizationRequest.ChangeAdministrativePin ->
            if (request.adminPinConfigured) CredentialRequirement.ADMIN_PIN else CredentialRequirement.ADMIN_PIN_SETUP
        is AuthorizationRequest.RestoreBackup ->
            if (request.adminPinConfigured) CredentialRequirement.ADMIN_PIN else CredentialRequirement.NONE
    }

    /**
     * Resolves an administrative requirement into its bootstrap flow when this device has not
     * configured an administrative PIN yet. The caller must ask this policy again only after
     * setup succeeds, rather than treating a missing PIN as authorization.
     */
    fun requirementFor(
        request: AuthorizationRequest,
        adminPinConfigured: Boolean
    ): CredentialRequirement {
        val requirement = requirementFor(request)
        return if (!adminPinConfigured && requirement.requiresAdministrativePin()) {
            CredentialRequirement.ADMIN_PIN_SETUP
        } else {
            requirement
        }
    }

    private fun CredentialRequirement.requiresAdministrativePin(): Boolean =
        this == CredentialRequirement.ADMIN_PIN ||
            this == CredentialRequirement.ADMIN_AND_DESTINATION_PROFILE_PIN
}
