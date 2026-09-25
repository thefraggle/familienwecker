package de.familienwecker.famwake.data

import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.util.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorMappingTest {

    @Test
    fun appErrorFromException_mapsStandardFirebaseExceptions() {
        val userNotFoundEx = Exception("ERROR_USER_NOT_FOUND")
        assertEquals(AppError.UserNotFound, appErrorFromException(userNotFoundEx))

        val invalidCredentialsEx = Exception("INVALID_CREDENTIALS")
        assertEquals(AppError.EmailOrPasswordEmpty, appErrorFromException(invalidCredentialsEx))

        val emailExistsEx = Exception("EMAIL_EXISTS")
        assertEquals(AppError.EmailAlreadyInUse, appErrorFromException(emailExistsEx))

        val weakPasswordEx = Exception("WEAK_PASSWORD")
        assertEquals(AppError.WeakPassword, appErrorFromException(weakPasswordEx))

        val invalidEmailEx = Exception("INVALID_EMAIL")
        assertEquals(AppError.InvalidEmail, appErrorFromException(invalidEmailEx))

        val tooManyRequestsEx = Exception("RESOURCE_EXHAUSTED: quota exceeded")
        assertEquals(AppError.TooManyRequests, appErrorFromException(tooManyRequestsEx))

        val permissionDeniedEx = Exception("PERMISSION_DENIED: missing rules")
        assertTrue(appErrorFromException(permissionDeniedEx) is AppError.PermissionDenied)
    }

    @Test
    fun appError_toUiText_returnsCorrectResourceIds() {
        assertEquals(R.string.error_user_not_found, (AppError.UserNotFound.toUiText() as UiText.StringResource).resId)
        assertEquals(R.string.error_invalid_credentials, (AppError.EmailOrPasswordEmpty.toUiText() as UiText.StringResource).resId)
        assertEquals(R.string.error_email_already_in_use, (AppError.EmailAlreadyInUse.toUiText() as UiText.StringResource).resId)
        assertEquals(R.string.error_weak_password, (AppError.WeakPassword.toUiText() as UiText.StringResource).resId)
        assertEquals(R.string.error_invalid_email, (AppError.InvalidEmail.toUiText() as UiText.StringResource).resId)
        assertEquals(R.string.error_too_many_requests, (AppError.TooManyRequests.toUiText() as UiText.StringResource).resId)
        assertEquals(R.string.error_family_not_found, (AppError.FamilyNotFound.toUiText() as UiText.StringResource).resId)
        assertEquals(R.string.error_code_generation_failed, (AppError.CodeGenerationFailed.toUiText() as UiText.StringResource).resId)
    }
}
