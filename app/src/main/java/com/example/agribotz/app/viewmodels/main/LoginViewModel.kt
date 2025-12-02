package com.example.agribotz.app.viewmodels.main

import android.util.Log
import androidx.lifecycle.*
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiResult
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.util.PreferencesManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.regex.Pattern

private const val COUNTRY_CODE_EGYPT = "+20"
private const val MOBILE_EGYPT_PATTERN = "^(\\+201)[0-2,5][0-9]{8}"

class LoginViewModel(private val repository: Repository, private val prefManager: PreferencesManager) : ViewModel() {
    // region LiveData
    private var _status = MutableLiveData<ApiStatus>()
    val status: LiveData<ApiStatus> =_status

    private val _eventClickLogin = MutableLiveData<Boolean>(false)
    val eventClickLogin: LiveData<Boolean> = _eventClickLogin

    private val _eventTransError = MutableLiveData<Int?>(null)
    val eventTransError: LiveData<Int?> = _eventTransError

    private val _stateEnableLogin = MutableLiveData<Boolean>(false)
    val stateEnableLogin: LiveData<Boolean> = _stateEnableLogin

    // For messages that come from backend (dynamic Strings)
    private val _errorServerMessage = MutableLiveData<String?>()
    val errorServerMessage: LiveData<String?> = _errorServerMessage

    // For messages defined in strings.xml (fixed resource IDs)
    private val _errorServerMessageRes = MutableLiveData<Int?>()
    val errorServerMessageRes: LiveData<Int?> = _errorServerMessageRes

    private val _errorMobileRequired = MutableLiveData<String?>()
    var errorMobileRequired: LiveData<String?> = _errorMobileRequired

    fun setMobileError(message: String?) {
        _errorMobileRequired.value = message
    }

    private val _errorPasswordRequired = MutableLiveData<String?>()
    val errorPasswordRequired: LiveData<String?> = _errorPasswordRequired

    fun setPasswordError(message: String?) {
        _errorPasswordRequired.value = message
    }
    // endregion

    private var _countryCode = COUNTRY_CODE_EGYPT
    private var _mobileNumber: String = ""
    val mobileNumber: String
        get() {
            return if (_mobileNumber.isNotEmpty() && _mobileNumber[0] == '0') {
                "$_countryCode${_mobileNumber.substring(1)}"
            } else {
                "$_countryCode$_mobileNumber"
            }
        }

    private var password = ""

    fun onClickLogin() {
        if (mobileNumberValidator(mobileNumber)) {
            viewModelScope.launch {
                try {
                    _status.value = ApiStatus.LOADING
                    val result = repository.login(mobileNumber, password)

                    when (result) {
                        is ApiResult.Success -> {

                            val message = result.data.message
                            prefManager.saveLoginData(
                                accessToken = "Bearer " + message.accessToken,
                                renewToken = "Bearer " + message.renewToken,
                                user = message.user,
                                sites = message.sites
                            )

                            _status.value = ApiStatus.DONE
                            _eventClickLogin.value = true
                        }

                        is ApiResult.Error -> {
                            Log.e("LoginViewModel", "Login failed: ${result.devMessage}")

                            // Show the "no connection" icon only for connectivity problems
                            if (result.userMessageKey == R.string.Error_Internet_Connection) {
                                _status.value = ApiStatus.ERROR
                            }
                            else {
                                _status.value = ApiStatus.DONE
                            }

                            if (!result.userMessageString.isNullOrBlank()) {
                                _errorServerMessage.value = result.userMessageString
                                _errorServerMessageRes.value = null
                            }
                            else {
                                _errorServerMessageRes.value = result.userMessageKey
                                _errorServerMessage.value = null
                            }
                        }
                    }
                }
                catch (e: Exception) {
                    _status.value = ApiStatus.DONE
                    _eventTransError.value = R.string.Error_Transaction_Failed
                    Log.e("LoginViewModel", "Login failed with exception", e)
                }
            }
        }
        else {
            _eventTransError.value = R.string.Error_Mobile_Format
        }
    }

    fun onClickLoginCompleted() {
        _eventClickLogin.value = false
    }

    fun onTransErrorCompleted() {
        _eventTransError.value = null
    }

    private fun mobileNumberValidator(mobileNumber: String) : Boolean {
        return mobilePattern.matcher(mobileNumber).matches()
    }

    private fun updateLoginButtonState() {
        _stateEnableLogin.value = _mobileNumber.length >= 10 && password.isNotEmpty()
    }

    fun onMobileNumberTextChanged(text: CharSequence) {
        _mobileNumber = text.toString()
        updateLoginButtonState()
        _errorMobileRequired.value = null
        _eventTransError.value = null
    }

    fun onPasswordTextChanged(text: CharSequence) {
        password = text.toString()
        updateLoginButtonState()
        _errorPasswordRequired.value = null
    }

    companion object {
        private val mobilePattern = Pattern.compile(MOBILE_EGYPT_PATTERN)
    }
}